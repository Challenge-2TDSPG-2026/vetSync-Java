package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.Recompensa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecompensaRepository extends JpaRepository<Recompensa, Long> {
    List<Recompensa> findByFlAtivoTrue();

    // Catálogo visível para quem pertence a uma clínica (tutor vinculado ou veterinário).
    List<Recompensa> findByFlAtivoTrueAndClinica_IdClinicaOrderByIdRecompensaAsc(Long idClinica);

    // Listagem administrativa (inclui inativos)
    List<Recompensa> findAllByOrderByIdRecompensaAsc();

    // Listagem administrativa filtrada por clínica (inclui inativos)
    List<Recompensa> findAllByClinica_IdClinicaOrderByIdRecompensaAsc(Long idClinica);

    // Itens legados, anteriores ao escopo por clínica (o Admin precisa escolher a clínica deles)
    List<Recompensa> findAllByClinicaIsNullOrderByIdRecompensaAsc();

    // Indicadores do painel do admin (global ou por clínica)
    long countByFlAtivo(Boolean ativo);
    long countByFlAtivoAndClinica_IdClinica(Boolean ativo, Long idClinica);
}