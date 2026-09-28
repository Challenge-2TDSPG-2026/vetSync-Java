package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.PerfilSaude;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilSaudeRepository extends JpaRepository<PerfilSaude, Long> {
    Optional<PerfilSaude> findByPet_IdPet(Long idPet);
}
