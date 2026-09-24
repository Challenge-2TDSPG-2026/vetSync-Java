package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.Recompensa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecompensaRepository extends JpaRepository<Recompensa, Long> {
    List<Recompensa> findByFlAtivoTrue();

    // NOVO: listagem administrativa (inclui inativos)
    List<Recompensa> findAllByOrderByIdRecompensaAsc();
}