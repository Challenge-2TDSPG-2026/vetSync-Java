package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.BloqueioAgendaEstetica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BloqueioAgendaEsteticaRepository extends JpaRepository<BloqueioAgendaEstetica, Long> {
    List<BloqueioAgendaEstetica> findByProfissionalEstetica_IdProfissionalEstetica(Long idProfissionalEstetica);
}