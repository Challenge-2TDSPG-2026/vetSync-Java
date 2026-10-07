package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import br.com.fiap.VetSync.service.PainelAdminService.PontosPainel;
import br.com.fiap.VetSync.service.PainelAdminService.PontosPorClinica;
import br.com.fiap.VetSync.service.PainelAdminService.ResumoPainel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Indicadores do painel do admin contra o banco de teste: por clínica, globais e sem contar pontos indisponíveis. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PainelAdminService.class, PontosService.class, ClinicaService.class})
class PainelAdminServiceTest {

    @Autowired private PainelAdminService painelAdminService;
    @Autowired private LancamentoPontosRepository lancamentoPontosRepository;
    @Autowired private ClinicaRepository clinicaRepository;
    @Autowired private TutorRepository tutorRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private EspecieRepository especieRepository;
    @Autowired private RacaRepository racaRepository;
    @Autowired private TipoEventoRepository tipoEventoRepository;
    @Autowired private EventoSaudeRepository eventoSaudeRepository;
    @Autowired private RecompensaRepository recompensaRepository;
    @Autowired private ResgateRepository resgateRepository;

    private void lancar(Pet pet, TipoEvento tipo, Clinica clinica, int pontos,
                        StatusLancamentoPontos status, LocalDate liberacao, LocalDate validade) {
        EventoSaude ev = eventoSaudeRepository.save(EventoSaude.builder()
                .pet(pet).tipoEvento(tipo).clinica(clinica).dtEvento(LocalDate.now())
                .dsStatus(StatusEvento.CONCLUIDO).build());
        lancamentoPontosRepository.save(LancamentoPontos.builder()
                .evento(ev).clinica(clinica).nrPontos(pontos).dsStatus(status)
                .dtLiberacao(liberacao).dtValidade(validade).build());
    }

    private Recompensa recompensa(Clinica clinica, String nome, boolean ativa) {
        return recompensaRepository.save(Recompensa.builder().nmRecompensa(nome).nrCustoPontos(40)
                .dsTipo(TipoRecompensa.PRODUTO).flAtivo(ativa).clinica(clinica).build());
    }

    @Test
    @DisplayName("Painel por clínica, global rotulado e disponíveis sem pendentes, bloqueados, vencidos ou reservados")
    void painelPorClinicaEGlobal() {
        Clinica c1 = clinicaRepository.save(Clinica.builder().nmClinica("Centro").dsCnpj("20000000000001").build());
        Clinica c2 = clinicaRepository.save(Clinica.builder().nmClinica("Norte").dsCnpj("20000000000002").build());
        Tutor ana = tutorRepository.save(Tutor.builder().nmTutor("Ana").dsEmail("ana.painel@teste.com")
                .dsCpf("33333333333").dsSenha("pwd123").build());
        Tutor beto = tutorRepository.save(Tutor.builder().nmTutor("Beto").dsEmail("beto.painel@teste.com")
                .dsCpf("44444444444").dsSenha("pwd123").build());
        Especie esp = especieRepository.save(Especie.builder().nmEspecie("Canis").build());
        Raca raca = racaRepository.save(Raca.builder().nmRaca("Beagle").especie(esp).build());
        Pet petAna = petRepository.save(Pet.builder().nmPet("Rex").dtNascimento(LocalDate.now().minusYears(2)).tutor(ana).raca(raca).build());
        Pet petBeto = petRepository.save(Pet.builder().nmPet("Bidu").dtNascimento(LocalDate.now().minusYears(2)).tutor(beto).raca(raca).build());
        TipoEvento tipo = tipoEventoRepository.save(TipoEvento.builder().nmTipoEvento("Vacina").dsCategoria("PREVENTIVO").nrPontos(10).build());

        LocalDate hoje = LocalDate.now();
        lancar(petAna, tipo, c1, 100, StatusLancamentoPontos.LIBERADO, hoje.minusDays(5), hoje.plusDays(360));
        lancar(petAna, tipo, c1, 50, StatusLancamentoPontos.LIBERADO, hoje.minusDays(400), hoje.minusDays(35)); // vencido
        lancar(petAna, tipo, c1, 30, StatusLancamentoPontos.PENDENTE, null, null);
        lancar(petBeto, tipo, c1, 20, StatusLancamentoPontos.BLOQUEADO, null, null);
        lancar(petAna, tipo, c2, 70, StatusLancamentoPontos.LIBERADO, hoje.minusDays(5), hoje.plusDays(360));

        Recompensa r1 = recompensa(c1, "Banho", true);
        recompensa(c1, "Antigo", false);
        recompensa(c2, "Tosa", true);
        resgateRepository.save(Resgate.builder().tutor(ana).recompensa(r1).nmRecompensa("Banho")
                .nrCustoPontos(40).clinica(c1).dsStatus(StatusResgate.PENDENTE).build());

        // --- escopo de clínica
        ResumoPainel centro = painelAdminService.resumo(c1.getIdClinica());
        assertThat(centro.escopo()).isEqualTo("CLINICA");
        assertThat(centro.totaisGlobais()).isFalse();
        assertThat(centro.rotuloEscopo()).isEqualTo("Centro");
        assertThat(centro.porClinica()).isEmpty();
        PontosPainel p1 = centro.pontos();
        assertThat(p1.pontosPendentes()).isEqualTo(30);
        assertThat(p1.pontosBloqueados()).isEqualTo(20);
        assertThat(p1.pontosExpirados()).isEqualTo(50);
        assertThat(p1.pontosReservados()).isEqualTo(40);
        assertThat(p1.pontosDisponiveis()).isEqualTo(60); // 100 liberados - 40 reservados
        assertThat(p1.lancamentosPendentes()).isEqualTo(1);
        assertThat(p1.lancamentosBloqueados()).isEqualTo(1);
        assertThat(centro.recompensas().recompensasAtivas()).isEqualTo(1);
        assertThat(centro.recompensas().recompensasInativas()).isEqualTo(1);
        assertThat(centro.recompensas().resgatesPendentes()).isEqualTo(1);

        ResumoPainel norte = painelAdminService.resumo(c2.getIdClinica());
        assertThat(norte.pontos().pontosDisponiveis()).isEqualTo(70);
        assertThat(norte.pontos().pontosPendentes()).isZero();
        assertThat(norte.recompensas().recompensasAtivas()).isEqualTo(1);
        assertThat(norte.recompensas().resgatesPendentes()).isZero();

        // --- escopo global, deixando explícito que soma todas as clínicas
        ResumoPainel global = painelAdminService.resumo(null);
        assertThat(global.escopo()).isEqualTo("GLOBAL");
        assertThat(global.totaisGlobais()).isTrue();
        assertThat(global.rotuloEscopo()).isEqualTo("Totais de todas as clínicas");
        assertThat(global.idClinica()).isNull();
        assertThat(global.pontos().pontosDisponiveis()).isEqualTo(130);
        assertThat(global.pontos().pontosPendentes()).isEqualTo(30);
        assertThat(global.pontos().pontosBloqueados()).isEqualTo(20);
        assertThat(global.pontos().pontosExpirados()).isEqualTo(50);
        assertThat(global.recompensas().recompensasAtivas()).isEqualTo(2);
        assertThat(global.recompensas().recompensasTotal()).isEqualTo(3);
        assertThat(global.porClinica()).extracting(PontosPorClinica::nmClinica).containsExactly("Centro", "Norte");
        assertThat(global.porClinica().get(0).pontos().pontosDisponiveis()).isEqualTo(60);
        assertThat(global.porClinica().get(1).pontos().pontosDisponiveis()).isEqualTo(70);
    }

    @Test
    @DisplayName("Clínica inexistente devolve 404")
    void clinicaInexistente() {
        assertThatThrownBy(() -> painelAdminService.resumo(999999L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}