package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.ProntuarioCompartilhadoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProntuarioCompartilhamentoServiceTest {

    @Mock private ProntuarioCompartilhadoRepository repository;
    @Mock private ProntuarioService prontuarioService;
    @Mock private ProntuarioRegistroService registroService;
    @Mock private PetService petService;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks private ProntuarioCompartilhamentoService service;

    private final Pet pet = Pet.builder().idPet(10L).nmPet("Luna").build();

    private ProntuarioCompartilhado link(LocalDateTime expira, LocalDateTime revogada, Set<SecaoProntuario> secoes) {
        return ProntuarioCompartilhado.builder().idCompartilhamento(3L).pet(pet)
                .dsSecoes(ProntuarioCompartilhado.serializarSecoes(secoes))
                .idUsuarioCriador(1L).criadaEm(LocalDateTime.now().minusDays(1)).expiraEm(expira)
                .revogadaEm(revogada).build();
    }

    // ------------------------------------------------------------------- criar

    @Test
    void criar_guardaSomenteOHashEDevolveOTokenPuroUmaVez() {
        when(petService.buscarPorId(10L)).thenReturn(pet);
        when(repository.findByPet_IdPetOrderByCriadaEmDesc(10L)).thenReturn(List.of());
        when(repository.save(any(ProntuarioCompartilhado.class))).thenAnswer(i -> i.getArgument(0));

        var criado = service.criar(10L, 1L, Set.of(SecaoProntuario.EXAMES), "  Clínica Norte  ", 3, null, null);

        assertThat(criado.tokenPuro()).hasSizeGreaterThanOrEqualTo(43); // 32 bytes em base64url
        ProntuarioCompartilhado salvo = criado.compartilhamento();
        assertThat(salvo.getTokenHash()).hasSize(64).isNotEqualTo(criado.tokenPuro());
        assertThat(salvo.getTokenHash()).isEqualTo(ProntuarioCompartilhamentoService.calcularHash(criado.tokenPuro()));
        assertThat(salvo.getDsDestinatario()).isEqualTo("Clínica Norte");
        assertThat(salvo.secoes()).containsExactly(SecaoProntuario.EXAMES);
        assertThat(salvo.getExpiraEm()).isBetween(LocalDateTime.now().plusDays(3).minusMinutes(1),
                LocalDateTime.now().plusDays(3).plusMinutes(1));
        verify(auditoriaService).registrarAcao(eq(AuditoriaTipos.PRONTUARIO), eq(10L),
                eq("PRONTUARIO_COMPARTILHADO"), isNull(), isNull(), any(), any(), any());
    }

    @Test
    void criar_semSecoesLiberaTodas_eUsaValidadePadrao() {
        when(petService.buscarPorId(10L)).thenReturn(pet);
        when(repository.findByPet_IdPetOrderByCriadaEmDesc(10L)).thenReturn(List.of());
        when(repository.save(any(ProntuarioCompartilhado.class))).thenAnswer(i -> i.getArgument(0));

        var criado = service.criar(10L, 1L, null, null, null, null, null);

        assertThat(criado.compartilhamento().secoes()).containsExactlyInAnyOrderElementsOf(EnumSet.allOf(SecaoProntuario.class));
        assertThat(criado.compartilhamento().getExpiraEm()).isAfter(LocalDateTime.now().plusDays(6));
    }

    @Test
    void criar_rejeitaValidadeForaDoLimite() {
        when(petService.buscarPorId(10L)).thenReturn(pet);

        assertThatThrownBy(() -> service.criar(10L, 1L, null, null, 31, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> service.criar(10L, 1L, null, null, 0, null, null))
                .isInstanceOf(ResponseStatusException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void criar_rejeitaPeriodoInvertido() {
        when(petService.buscarPorId(10L)).thenReturn(pet);

        assertThatThrownBy(() -> service.criar(10L, 1L, null, null, 5,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void criar_limitaLinksAtivosPorPet() {
        when(petService.buscarPorId(10L)).thenReturn(pet);
        List<ProntuarioCompartilhado> ativos = IntStream.range(0, ProntuarioCompartilhamentoService.MAXIMO_LINKS_ATIVOS_POR_PET)
                .mapToObj(i -> link(LocalDateTime.now().plusDays(1), null, EnumSet.allOf(SecaoProntuario.class))).toList();
        when(repository.findByPet_IdPetOrderByCriadaEmDesc(10L)).thenReturn(ativos);

        assertThatThrownBy(() -> service.criar(10L, 1L, null, null, 5, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void criar_linksExpiradosOuRevogadosNaoContamNoLimite() {
        when(petService.buscarPorId(10L)).thenReturn(pet);
        List<ProntuarioCompartilhado> inativos = IntStream.range(0, 8).mapToObj(i -> i % 2 == 0
                ? link(LocalDateTime.now().minusDays(1), null, EnumSet.allOf(SecaoProntuario.class))
                : link(LocalDateTime.now().plusDays(1), LocalDateTime.now().minusHours(1), EnumSet.allOf(SecaoProntuario.class))).toList();
        when(repository.findByPet_IdPetOrderByCriadaEmDesc(10L)).thenReturn(inativos);
        when(repository.save(any(ProntuarioCompartilhado.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.criar(10L, 1L, null, null, 5, null, null).tokenPuro()).isNotBlank();
    }

    // ----------------------------------------------------------------- revogar

    @Test
    void revogar_marcaERegistraAuditoriaUmaUnicaVez() {
        ProntuarioCompartilhado l = link(LocalDateTime.now().plusDays(1), null, EnumSet.allOf(SecaoProntuario.class));
        when(repository.findByIdCompartilhamentoAndPet_IdPet(3L, 10L)).thenReturn(Optional.of(l));

        service.revogar(10L, 3L);
        service.revogar(10L, 3L); // idempotente

        assertThat(l.getRevogadaEm()).isNotNull();
        assertThat(l.isAtivo()).isFalse();
        verify(repository).save(l);
        verify(auditoriaService).registrarAcao(eq(AuditoriaTipos.PRONTUARIO), eq(10L),
                eq("PRONTUARIO_COMPARTILHAMENTO_REVOGADO"), isNull(), any(), isNull(), any(), any());
    }

    @Test
    void revogar_linkDeOutroPetRetorna404() {
        when(repository.findByIdCompartilhamentoAndPet_IdPet(3L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revogar(99L, 3L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --------------------------------------------------------------- visualizar

    @Test
    void visualizar_tokenDesconhecidoRetorna404() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.visualizar("qualquer"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> service.visualizar(" ")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void visualizar_expiradoOuRevogadoRetorna410_eNaoMontaProntuario() {
        when(repository.findByTokenHash(any()))
                .thenReturn(Optional.of(link(LocalDateTime.now().minusMinutes(1), null, EnumSet.allOf(SecaoProntuario.class))))
                .thenReturn(Optional.of(link(LocalDateTime.now().plusDays(1), LocalDateTime.now().minusMinutes(1), EnumSet.allOf(SecaoProntuario.class))));

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> service.visualizar("t"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.GONE);
        }
        verify(prontuarioService, never()).montar(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void visualizar_contaAcesso_auditaEMontaApenasComEscopoDoLink_ecomReceitasLiberadas() {
        Set<SecaoProntuario> escopo = EnumSet.of(SecaoProntuario.EXAMES, SecaoProntuario.RECEITAS);
        ProntuarioCompartilhado l = link(LocalDateTime.now().plusDays(1), null, escopo);
        l.setDtPeriodoInicio(LocalDate.of(2026, 1, 1));
        l.setDtPeriodoFim(LocalDate.of(2026, 6, 30));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(l));
        ProntuarioService.Prontuario esperado = org.mockito.Mockito.mock(ProntuarioService.Prontuario.class);
        when(prontuarioService.montar(10L, escopo, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), true))
                .thenReturn(esperado);

        ProntuarioService.Prontuario resultado = service.visualizar("token");

        assertThat(resultado).isSameAs(esperado);
        assertThat(l.getNrAcessos()).isEqualTo(1);
        assertThat(l.getUltimoAcessoEm()).isNotNull();
        verify(repository).save(l);
        ArgumentCaptor<String> acao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrarAcao(eq(AuditoriaTipos.PRONTUARIO), eq(10L), acao.capture(),
                isNull(), isNull(), any(), eq("LINK_PUBLICO"), eq("EXTERNO"));
        assertThat(acao.getValue()).isEqualTo("PRONTUARIO_ACESSADO_POR_LINK");
    }

    // ------------------------------------------------------------ arquivo exame

    @Test
    void arquivoDoExame_secaoExamesForaDoEscopoRetorna404() {
        ProntuarioCompartilhado l = link(LocalDateTime.now().plusDays(1), null, EnumSet.of(SecaoProntuario.RECEITAS));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(l));

        assertThatThrownBy(() -> service.arquivoDoExame("t", 5L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(registroService, never()).buscarExameComArquivo(any(), any());
    }

    @Test
    void arquivoDoExame_foraDoPeriodoRetorna404() {
        ProntuarioCompartilhado l = link(LocalDateTime.now().plusDays(1), null, EnumSet.of(SecaoProntuario.EXAMES));
        l.setDtPeriodoInicio(LocalDate.of(2026, 3, 1));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(l));
        ResultadoExame exame = ResultadoExame.builder().idResultado(5L).pet(pet).nmArquivo("a.pdf")
                .dsMimeType("application/pdf").dsConteudo(new byte[]{1}).dtResultado(LocalDate.of(2026, 2, 1)).build();
        when(registroService.buscarExameComArquivo(10L, 5L)).thenReturn(exame);

        assertThatThrownBy(() -> service.arquivoDoExame("t", 5L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void arquivoDoExame_devolveOArquivoEAudita() {
        ProntuarioCompartilhado l = link(LocalDateTime.now().plusDays(1), null, EnumSet.of(SecaoProntuario.EXAMES));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(l));
        ResultadoExame exame = ResultadoExame.builder().idResultado(5L).pet(pet).nmArquivo("laudo.pdf")
                .dsMimeType("application/pdf").dsConteudo(new byte[]{1, 2}).dtResultado(LocalDate.of(2026, 2, 1)).build();
        when(registroService.buscarExameComArquivo(10L, 5L)).thenReturn(exame);

        var arquivo = service.arquivoDoExame("t", 5L);

        assertThat(arquivo.nome()).isEqualTo("laudo.pdf");
        assertThat(arquivo.bytes()).containsExactly(1, 2);
        verify(auditoriaService).registrarAcao(eq(AuditoriaTipos.PRONTUARIO), eq(10L),
                eq("PRONTUARIO_ARQUIVO_ACESSADO_POR_LINK"), isNull(), isNull(), any(), any(), any());
    }
}