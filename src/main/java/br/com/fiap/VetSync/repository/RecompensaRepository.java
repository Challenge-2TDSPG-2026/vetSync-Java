package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.Recompensa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecompensaRepository extends JpaRepository<Recompensa, Long> {
    List<Recompensa> findByFlAtivoTrue();

    // Catálogo visível para quem pertence a uma clínica (tutor vinculado ou veterinário).
    List<Recompensa> findByFlAtivoTrueAndClinica_IdClinicaOrderByIdRecompensaAsc(Long idClinica);

    // NOVO: listagem administrativa (inclui inativos)
    List<Recompensa> findAllByOrderByIdRecompensaAsc();
}