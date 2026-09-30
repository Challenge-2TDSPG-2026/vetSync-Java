package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.VinculoTutorClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface VinculoTutorClinicaRepository extends JpaRepository<VinculoTutorClinica, Long> {
    Optional<VinculoTutorClinica> findByTutor_IdTutorAndDtEncerramentoIsNull(Long idTutor);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VinculoTutorClinica v where v.tutor.idTutor = :idTutor and v.dtEncerramento is null")
    Optional<VinculoTutorClinica> findAtivoPorTutorParaAtualizacao(@Param("idTutor") Long idTutor);

    @Query("select count(v) > 0 from VinculoTutorClinica v where v.tutor.idTutor = :idTutor and v.dtEncerramento is null and v.clinica.stContratante = 'A'")
    boolean existsAtivoEmClinicaContratante(@Param("idTutor") Long idTutor);
}
