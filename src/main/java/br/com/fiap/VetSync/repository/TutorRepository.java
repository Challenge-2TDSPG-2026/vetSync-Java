package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.Tutor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TutorRepository extends JpaRepository<Tutor, Long> {
    /** Trava a linha do tutor até o fim da transação. Serve para serializar operações que mexem no saldo de pontos. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tutor t where t.idTutor = :idTutor")
    Optional<Tutor> findByIdParaAtualizar(@Param("idTutor") Long idTutor);

    Optional<Tutor> findByDsEmail(String dsEmail);
    boolean existsByDsEmail(String dsEmail);
}