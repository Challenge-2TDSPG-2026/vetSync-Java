package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.SiaSessaoAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface SiaSessaoAgendamentoRepository extends JpaRepository<SiaSessaoAgendamento, String> {
    void deleteByExpiraEmBefore(LocalDateTime limite);
}
