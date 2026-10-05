package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.LancamentoPontos;
import br.com.fiap.VetSync.entity.StatusLancamentoPontos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LancamentoPontosRepository extends JpaRepository<LancamentoPontos, Long> {
    Optional<LancamentoPontos> findByEvento_IdEvento(Long idEvento);
    Optional<LancamentoPontos> findByPlanoTratamento_IdPlano(Long idPlano);
    List<LancamentoPontos> findByDsStatusOrderByDtLancamentoAsc(StatusLancamentoPontos status);

    List<LancamentoPontos> findByEvento_Pet_Tutor_IdTutorAndDsStatus(Long idTutor, StatusLancamentoPontos status);
    List<LancamentoPontos> findByPlanoTratamento_Pet_Tutor_IdTutorAndDsStatus(Long idTutor, StatusLancamentoPontos status);

    // Saldo por clínica: os pontos só valem na clínica em que foram ganhos.
    List<LancamentoPontos> findByEvento_Pet_Tutor_IdTutorAndClinica_IdClinicaAndDsStatus(
            Long idTutor, Long idClinica, StatusLancamentoPontos status);
    List<LancamentoPontos> findByPlanoTratamento_Pet_Tutor_IdTutorAndClinica_IdClinicaAndDsStatus(
            Long idTutor, Long idClinica, StatusLancamentoPontos status);

    List<LancamentoPontos> findByEvento_Pet_Tutor_DsEmailOrderByDtLancamentoDesc(String email);
    List<LancamentoPontos> findByPlanoTratamento_Pet_Tutor_DsEmailOrderByDtLancamentoDesc(String email);
}