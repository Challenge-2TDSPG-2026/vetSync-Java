package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.EventoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventoHistoricoRepository extends JpaRepository<EventoHistorico, Long> {
    List<EventoHistorico> findByEvento_IdEventoOrderByDtOcorrenciaDesc(Long idEvento);
}
