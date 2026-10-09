package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProntuarioServiceTest {

    @Mock private PetService petService;
    @Mock private PerfilSaudeRepository perfilSaudeRepository;
    @Mock private EventoSaudeRepository eventoSaudeRepository;
    @Mock private OrientacaoClinicaRepository orientacaoRepository;
    @Mock private PrescricaoRepository prescricaoRepository;
    @Mock private ResultadoExameRepository exameRepository;

    @InjectMocks private ProntuarioService service;

    private final Veterinario ana = Veterinario.builder().idVeterinario(1L).nmVeterinario("Dra. Ana").nrCrmv("12345").build();
    private Pet pet;

    @BeforeEach
    void setUp() {
        Raca raca = Raca.builder().nmRaca("Labrador").especie(Especie.builder().nmEspecie("Cão").build()).build();
        pet = Pet.builder().idPet(10L).nmPet("Luna").dsSexo("F").dtNascimento(LocalDate.of(2020, 1, 1)).raca(raca).build();
    }

    private EventoSaude atendimento(long id, LocalDate data) {
        return EventoSaude.builder().idEvento(id).pet(pet).veterinario(ana).dtEvento(data).hrEvento("10:00")
                .dsStatus(StatusEvento.CONCLUIDO).dsDiagnostico("Otite").dsConduta("Limpeza")
                .dsObservacaoClinica("Sem febre").vlCusto(new BigDecimal("150.00")).dsObservacaoTutor("Coça muito")
                .tipoEvento(TipoEvento.builder().nmTipoEvento("Consulta").dsCategoria("TERAPEUTICO").build()).build();
    }

    private Prescricao receita(long id, StatusPrescricao status, LocalDate inicio) {
        return Prescricao.builder().idPrescricao(id).evento(atendimento(100, inicio)).dsStatus(status)
                .medicamento(Medicamento.builder().nmMedicamento("Amoxicilina").dsPrincipio("Amoxicilina").build())
                .dsPosologia("1 comprimido a cada 12h").dtInicio(inicio).qtDosesDia(2).build();
    }

    private ResultadoExame exame(long id, LocalDate resultado) {
        return ResultadoExame.builder().idResultado(id).pet(pet).veterinario(ana).nmExame("Hemograma")
                .dtResultado(resultado).dsResultado("Normal").nmArquivo(id == 1 ? "a.pdf" : null).build();
    }

    @Test
    void montar_resumeOPetSemDadosDoTutor() {
        when(petService.buscarPorId(10L)).thenReturn(pet);

        ProntuarioService.Prontuario p = service.montar(10L, null, null, null, false);

        assertThat(p.pet().nome()).isEqualTo("Luna");
        assertThat(p.pet().numero()).isEqualTo("0010");
        assertThat(p.pet().especie()).isEqualTo("Cão");
        assertThat(p.pet().raca()).isEqualTo("Labrador");
        assertThat(p.versao()).isEqualTo(ProntuarioService.VERSAO_FORMATO);
        assertThat(p.secoes()).containsExactlyInAnyOrderElementsOf(EnumSet.allOf(SecaoProntuario.class));
    }

    @Test
    void montar_soConsultaAsSecoesPedidas() {
        when(petService.buscarPorId(10L)).thenReturn(pet);
        when(exameRepository.findByPet_IdPetOrderByDtResultadoDescIdResultadoDesc(10L))
                .thenReturn(List.of(exame(1, LocalDate.of(2026, 3, 1))));

        ProntuarioService.Prontuario p = service.montar(10L, EnumSet.of(SecaoProntuario.EXAMES), null, null, true);

        assertThat(p.exames()).hasSize(1);
        assertThat(p.exames().get(0).temArquivo()).isTrue();
        assertThat(p.atendimentos()).isEmpty();
        assertThat(p.perfilSaude()).isNull();
        verifyNoInteractions(eventoSaudeRepository, orientacaoRepository, prescricaoRepository, perfilSaudeRepository);
    }

    @Test
    void montar_atendimentosNaoExpoemCustoNemObservacaoDoTutor() {
        when(petService.buscarPorId(10L)).thenReturn(pet);
        when(eventoSaudeRepository.findByPet_IdPetAndDsStatusOrderByDtEventoDescIdEventoDesc(10L, StatusEvento.CONCLUIDO))
                .thenReturn(List.of(atendimento(1, LocalDate.of(2026, 2, 1))));

        ProntuarioService.Prontuario p = service.montar(10L, EnumSet.of(SecaoProntuario.ATENDIMENTOS), null, null, true);

        ProntuarioService.Atendimento a = p.atendimentos().get(0);
        assertThat(a.tipo()).isEqualTo("Consulta");
        assertThat(a.veterinario()).isEqualTo("Dra. Ana");
        assertThat(a.crmv()).isEqualTo("12345");
        assertThat(a.diagnostico()).isEqualTo("Otite");
        assertThat(a.observacaoClinica()).isEqualTo("Sem febre");
        // O record não possui campos de custo nem de observação do tutor — garantia estrutural da privacidade.
        assertThat(ProntuarioService.Atendimento.class.getRecordComponents())
                .extracting(c -> c.getName().toLowerCase())
                .noneMatch(n -> n.contains("custo") || n.contains("tutor"));
    }

    @Test
    void montar_exportacaoExternaTrazSoReceitasLiberadas_eConsultaInternaTrazTodas() {
        when(petService.buscarPorId(10L)).thenReturn(pet);
        LocalDate hoje = LocalDate.of(2026, 5, 1);
        when(prescricaoRepository.findByEvento_Pet_IdPetOrderByDtInicioDescIdPrescricaoDesc(10L)).thenReturn(List.of(
                receita(1, StatusPrescricao.LIBERADO, hoje),
                receita(2, StatusPrescricao.SOLICITADO, hoje),
                receita(3, StatusPrescricao.NEGADO, hoje)));

        var externo = service.montar(10L, EnumSet.of(SecaoProntuario.RECEITAS), null, null, true);
        var interno = service.montar(10L, EnumSet.of(SecaoProntuario.RECEITAS), null, null, false);

        assertThat(externo.receitas()).extracting(ProntuarioService.Receita::id).containsExactly(1L);
        assertThat(interno.receitas()).hasSize(3);
        assertThat(externo.receitas().get(0).medicamento()).isEqualTo("Amoxicilina");
    }

    @Test
    void montar_filtraPorPeriodoInclusivo() {
        when(petService.buscarPorId(10L)).thenReturn(pet);
        when(exameRepository.findByPet_IdPetOrderByDtResultadoDescIdResultadoDesc(10L)).thenReturn(List.of(
                exame(1, LocalDate.of(2026, 6, 30)),
                exame(2, LocalDate.of(2026, 3, 1)),
                exame(3, LocalDate.of(2026, 1, 1)),
                exame(4, LocalDate.of(2025, 12, 31))));

        var p = service.montar(10L, EnumSet.of(SecaoProntuario.EXAMES),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), true);

        assertThat(p.exames()).extracting(ProntuarioService.Exame::id).containsExactly(1L, 2L, 3L);
    }

    @Test
    void montar_linhaDoTempoMisturaTudoDoMaisRecenteParaOMaisAntigo() {
        when(petService.buscarPorId(10L)).thenReturn(pet);
        when(eventoSaudeRepository.findByPet_IdPetAndDsStatusOrderByDtEventoDescIdEventoDesc(10L, StatusEvento.CONCLUIDO))
                .thenReturn(List.of(atendimento(1, LocalDate.of(2026, 3, 10))));
        when(exameRepository.findByPet_IdPetOrderByDtResultadoDescIdResultadoDesc(10L))
                .thenReturn(List.of(exame(2, LocalDate.of(2026, 3, 20))));
        when(prescricaoRepository.findByEvento_Pet_IdPetOrderByDtInicioDescIdPrescricaoDesc(10L))
                .thenReturn(List.of(receita(3, StatusPrescricao.LIBERADO, LocalDate.of(2026, 3, 11))));
        OrientacaoClinica o = OrientacaoClinica.builder().idOrientacao(4L).evento(atendimento(1, LocalDate.of(2026, 3, 10)))
                .veterinario(ana).dsTitulo("Repouso").dsTexto("7 dias").dtCriacao(LocalDateTime.of(2026, 3, 10, 15, 0)).build();
        when(orientacaoRepository.findByEvento_Pet_IdPetOrderByDtCriacaoDesc(10L)).thenReturn(List.of(o));
        when(perfilSaudeRepository.findByPet_IdPet(10L)).thenReturn(Optional.of(
                PerfilSaude.builder().pet(pet).alergias("Dipirona").contatoEmergencia("(19) 99999-0000").build()));

        var p = service.montar(10L, null, null, null, true);

        assertThat(p.linhaDoTempo()).extracting(ProntuarioService.EntradaLinhaDoTempo::tipo)
                .containsExactly("EXAME", "RECEITA", "ATENDIMENTO", "ORIENTACAO");
        assertThat(p.perfilSaude().alergias()).isEqualTo("Dipirona");
        assertThat(ProntuarioService.PerfilResumo.class.getRecordComponents())
                .extracting(c -> c.getName().toLowerCase()).noneMatch(n -> n.contains("contato"));
    }

    @Test
    void montar_periodoInvertidoRecebe400() {
        assertThatThrownBy(() -> service.montar(10L, null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1), false))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(petService, never()).buscarPorId(10L);
    }
}