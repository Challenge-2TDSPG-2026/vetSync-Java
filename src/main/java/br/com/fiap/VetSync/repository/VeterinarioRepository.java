package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.Veterinario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VeterinarioRepository extends JpaRepository<Veterinario, Long> {
    Optional<Veterinario> findByDsEmail(String dsEmail);
    boolean existsByDsEmail(String dsEmail);
    boolean existsByNrCrmv(String nrCrmv);
    List<Veterinario> findByDsEspecialidade(String dsEspecialidade);
}