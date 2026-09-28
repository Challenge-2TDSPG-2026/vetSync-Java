package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.EventoAnexo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EventoAnexoRepository extends JpaRepository<EventoAnexo, Long> {
    List<EventoAnexo> findByEvento_IdEventoOrderByDtCriacaoDesc(Long idEvento);
    Optional<EventoAnexo> findByIdAnexoAndEvento_IdEvento(Long idAnexo, Long idEvento);
}
