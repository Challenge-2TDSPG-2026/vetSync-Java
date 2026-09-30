package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.StatusAcessoPet;
import br.com.fiap.VetSync.entity.TutorResponsavel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TutorResponsavelRepository extends JpaRepository<TutorResponsavel, Long> {

    List<TutorResponsavel> findByTutorProprietario_IdTutorAndDsStatusOrderByDtConcedidoDesc(
            Long idTutor, StatusAcessoPet status);

    Optional<TutorResponsavel> findByTutorProprietario_IdTutorAndTutorResponsavel_IdTutor(
            Long idTutorProprietario, Long idTutorResponsavel);
}
