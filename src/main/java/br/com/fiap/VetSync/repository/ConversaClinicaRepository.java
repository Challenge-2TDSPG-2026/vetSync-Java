package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.ConversaClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConversaClinicaRepository extends JpaRepository<ConversaClinica, Long> {
    List<ConversaClinica> findByClinica_IdClinicaOrderByDtCriacaoDesc(Long idClinica);
    List<ConversaClinica> findByTutor_IdTutorOrderByDtCriacaoDesc(Long idTutor);
    Optional<ConversaClinica> findByClinica_IdClinicaAndTutor_IdTutor(Long idClinica, Long idTutor);
}
