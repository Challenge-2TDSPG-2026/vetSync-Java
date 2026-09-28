package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.TipoVacina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoVacinaRepository extends JpaRepository<TipoVacina, Long> {
    Optional<TipoVacina> findByNmTipoVacinaIgnoreCase(String nome);
}
