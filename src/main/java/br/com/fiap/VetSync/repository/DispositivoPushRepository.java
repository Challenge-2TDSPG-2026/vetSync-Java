package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.DispositivoPush;
import br.com.fiap.VetSync.entity.PlataformaPush;
import br.com.fiap.VetSync.entity.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DispositivoPushRepository extends JpaRepository<DispositivoPush, Long> {
    Optional<DispositivoPush> findByTutorAndTokenAndPlataforma(Tutor tutor, String token, PlataformaPush plataforma);
    Optional<DispositivoPush> findByTutorAndToken(Tutor tutor, String token);
}
