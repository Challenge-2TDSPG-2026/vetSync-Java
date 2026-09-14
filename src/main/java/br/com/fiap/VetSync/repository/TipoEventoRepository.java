package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoEventoRepository extends JpaRepository<TipoEvento, Long> {
    Optional<TipoEvento> findFirstByNmTipoEventoIgnoreCase(String nmTipoEvento);
}