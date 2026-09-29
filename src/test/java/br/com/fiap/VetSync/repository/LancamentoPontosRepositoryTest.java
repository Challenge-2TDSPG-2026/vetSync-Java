package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LancamentoPontosRepositoryTest {

    @Autowired
    private LancamentoPontosRepository lancamentoPontosRepository;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private EspecieRepository especieRepository;

    @Autowired
    private RacaRepository racaRepository;

    @Autowired
    private TipoEventoRepository tipoEventoRepository;

    @Autowired
    private EventoSaudeRepository eventoSaudeRepository;

    @Test
    @DisplayName("Deve buscar lançamentos de pontos por status e por tutor")
    void buscarLancamentos() {
        Tutor tutor = tutorRepository.save(Tutor.builder()
                .nmTutor("Bruno")
                .dsEmail("bruno.pontos@teste.com")
                .dsCpf("66677788899")
                .dsSenha("pwd123")
                .build());

        Especie esp = especieRepository.save(Especie.builder().nmEspecie("Canis").build());
        Raca raca = racaRepository.save(Raca.builder().nmRaca("Beagle").especie(esp).build());
        Pet pet = petRepository.save(Pet.builder().nmPet("Snoopy").dtNascimento(LocalDate.now().minusYears(2)).tutor(tutor).raca(raca).build());
        TipoEvento tipo = tipoEventoRepository.save(TipoEvento.builder().nmTipoEvento("Vacina").dsCategoria("PREVENTIVO").nrPontos(20).build());

        EventoSaude ev = eventoSaudeRepository.save(EventoSaude.builder()
                .pet(pet).tipoEvento(tipo).dtEvento(LocalDate.now()).dsStatus(StatusEvento.CONCLUIDO).build());

        lancamentoPontosRepository.save(LancamentoPontos.builder()
                .evento(ev).nrPontos(20).dsStatus(StatusLancamentoPontos.LIBERADO).dtLancamento(LocalDate.now()).build());

        List<LancamentoPontos> pendentes = lancamentoPontosRepository.findByDsStatusOrderByDtLancamentoAsc(StatusLancamentoPontos.PENDENTE);
        assertThat(pendentes).isEmpty();

        List<LancamentoPontos> liberadosTutor = lancamentoPontosRepository.findByEvento_Pet_Tutor_IdTutorAndDsStatus(tutor.getIdTutor(), StatusLancamentoPontos.LIBERADO);
        assertThat(liberadosTutor).hasSize(1);
        assertThat(liberadosTutor.get(0).getNrPontos()).isEqualTo(20);
    }
}
