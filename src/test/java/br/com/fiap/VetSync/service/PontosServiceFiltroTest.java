package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercita as consultas reais (filtro por clínica/tutor/status e saldo por clínica) no banco de teste. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PontosService.class)
class PontosServiceFiltroTest {

    @Autowired private PontosService pontosService;
    @Autowired private LancamentoPontosRepository lancamentoPontosRepository;
    @Autowired private ClinicaRepository clinicaRepository;
    @Autowired private TutorRepository tutorRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private EspecieRepository especieRepository;
    @Autowired private RacaRepository racaRepository;
    @Autowired private TipoEventoRepository tipoEventoRepository;
    @Autowired private EventoSaudeRepository eventoSaudeRepository;

    private LancamentoPontos lancar(Pet pet, TipoEvento tipo, Clinica clinica, int pontos,
                                    StatusLancamentoPontos status, LocalDate liberacao, LocalDate validade) {
        EventoSaude ev = eventoSaudeRepository.save(EventoSaude.builder()
                .pet(pet).tipoEvento(tipo).clinica(clinica).dtEvento(LocalDate.now())
                .dsStatus(StatusEvento.CONCLUIDO).build());
        return lancamentoPontosRepository.save(LancamentoPontos.builder()
                .evento(ev).clinica(clinica).nrPontos(pontos).dsStatus(status)
                .dtLiberacao(liberacao).dtValidade(validade).build());
    }

    @Test
    @DisplayName("Filtra por clínica, status (inclusive EXPIRADO derivado) e tutor; saldo nunca soma clínicas")
    void filtrosESaldoPorClinica() {
        Clinica c1 = clinicaRepository.save(Clinica.builder().nmClinica("Centro").dsCnpj("10000000000001").build());
        Clinica c2 = clinicaRepository.save(Clinica.builder().nmClinica("Norte").dsCnpj("10000000000002").build());
        Tutor ana = tutorRepository.save(Tutor.builder().nmTutor("Ana").dsEmail("ana.filtro@teste.com")
                .dsCpf("11111111111").dsSenha("pwd123").build());
        Tutor beto = tutorRepository.save(Tutor.builder().nmTutor("Beto").dsEmail("beto.filtro@teste.com")
                .dsCpf("22222222222").dsSenha("pwd123").build());
        Especie esp = especieRepository.save(Especie.builder().nmEspecie("Canis").build());
        Raca raca = racaRepository.save(Raca.builder().nmRaca("Beagle").especie(esp).build());
        Pet petAna = petRepository.save(Pet.builder().nmPet("Rex").dtNascimento(LocalDate.now().minusYears(2)).tutor(ana).raca(raca).build());
        Pet petBeto = petRepository.save(Pet.builder().nmPet("Bidu").dtNascimento(LocalDate.now().minusYears(2)).tutor(beto).raca(raca).build());
        TipoEvento tipo = tipoEventoRepository.save(TipoEvento.builder().nmTipoEvento("Vacina").dsCategoria("PREVENTIVO").nrPontos(10).build());

        LocalDate hoje = LocalDate.now();
        lancar(petAna, tipo, c1, 100, StatusLancamentoPontos.LIBERADO, hoje.minusDays(5), hoje.plusDays(360));
        lancar(petAna, tipo, c1, 50, StatusLancamentoPontos.LIBERADO, hoje.minusDays(400), hoje.minusDays(35)); // expirado
        lancar(petAna, tipo, c2, 70, StatusLancamentoPontos.LIBERADO, hoje.minusDays(5), hoje.plusDays(360));
        lancar(petAna, tipo, c1, 30, StatusLancamentoPontos.PENDENTE, null, null);
        lancar(petBeto, tipo, c1, 20, StatusLancamentoPontos.BLOQUEADO, null, null);

        assertThat(pontosService.listar(null, null, null)).hasSize(5);
        assertThat(pontosService.listar(c1.getIdClinica(), null, null)).hasSize(4);
        assertThat(pontosService.listar(c2.getIdClinica(), null, null)).hasSize(1);
        assertThat(pontosService.listar(c1.getIdClinica(), StatusLancamentoPontos.EXPIRADO, null)).hasSize(1);
        assertThat(pontosService.listar(c1.getIdClinica(), StatusLancamentoPontos.LIBERADO, null)).hasSize(1);
        assertThat(pontosService.listar(null, StatusLancamentoPontos.BLOQUEADO, beto.getIdTutor())).hasSize(1);
        assertThat(pontosService.listarParaTutor("ana.filtro@teste.com", null, null)).hasSize(4);
        assertThat(pontosService.listarParaTutor("ana.filtro@teste.com", c2.getIdClinica(), null)).hasSize(1);

        PontosService.SaldoPontos saldoC1 = pontosService.calcularSaldo(ana.getIdTutor(), c1.getIdClinica());
        PontosService.SaldoPontos saldoC2 = pontosService.calcularSaldo(ana.getIdTutor(), c2.getIdClinica());
        assertThat(saldoC1.saldoDisponivel()).isEqualTo(100);
        assertThat(saldoC1.pontosExpirados()).isEqualTo(50);
        assertThat(saldoC1.pontosPendentes()).isEqualTo(30);
        assertThat(saldoC2.saldoDisponivel()).isEqualTo(70);

        List<PontosService.SaldoTutorClinica> saldos = pontosService.listarSaldos(null, ana.getIdTutor());
        assertThat(saldos).hasSize(2); // uma linha por clínica, nunca 170 somados
        assertThat(saldos).extracting(s -> s.saldo().saldoDisponivel()).containsExactlyInAnyOrder(100, 70);
    }
}