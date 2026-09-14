package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.ProfissionalEstetica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfissionalEsteticaRepository extends JpaRepository<ProfissionalEstetica, Long> {
    Optional<ProfissionalEstetica> findByDsEmail(String dsEmail);
    boolean existsByDsEmail(String dsEmail);
    boolean existsByNrRegistro(String nrRegistro);
}