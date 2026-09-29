package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RecompensaRepositoryTest {

    @Autowired
    private RecompensaRepository recompensaRepository;

    @Autowired
    private ResgateRepository resgateRepository;

    @Autowired
    private TutorRepository tutorRepository;

    @Test
    @DisplayName("Deve buscar recompensas ativas e resgates por tutor e status")
    void buscarRecompensasEResgates() {
        Recompensa rAtiva = recompensaRepository.save(Recompensa.builder()
                .nmRecompensa("Petisco")
                .nrCustoPontos(30)
                .dsTipo(TipoRecompensa.PRODUTO)
                .flAtivo(true)
                .build());

        recompensaRepository.save(Recompensa.builder()
                .nmRecompensa("Antigo")
                .nrCustoPontos(100)
                .dsTipo(TipoRecompensa.CUPOM_DESCONTO)
                .flAtivo(false)
                .build());

        List<Recompensa> ativas = recompensaRepository.findByFlAtivoTrue();
        assertThat(ativas).extracting(Recompensa::getNmRecompensa).contains("Petisco").doesNotContain("Antigo");

        Tutor tutor = tutorRepository.save(Tutor.builder()
                .nmTutor("Guilherme")
                .dsEmail("gui.rec@teste.com")
                .dsCpf("99911122233")
                .dsSenha("pwd")
                .build());

        resgateRepository.save(Resgate.builder()
                .tutor(tutor)
                .recompensa(rAtiva)
                .dsStatus(StatusResgate.PENDENTE)
                .dtResgate(LocalDateTime.now())
                .build());

        List<Resgate> resgatesTutor = resgateRepository.findByTutor_IdTutorOrderByDtResgateDesc(tutor.getIdTutor());
        assertThat(resgatesTutor).hasSize(1);

        List<Resgate> pendentes = resgateRepository.findByDsStatusOrderByDtResgateAsc(StatusResgate.PENDENTE);
        assertThat(pendentes).hasSize(1);
    }
}
