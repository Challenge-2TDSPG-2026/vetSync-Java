package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.LinkAgendamentoVet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkAgendamentoVetRepository extends JpaRepository<LinkAgendamentoVet, Long> {
    Optional<LinkAgendamentoVet> findByDsToken(String dsToken);
}