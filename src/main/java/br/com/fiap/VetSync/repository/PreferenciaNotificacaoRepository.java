package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.PreferenciaNotificacao;
import br.com.fiap.VetSync.entity.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreferenciaNotificacaoRepository extends JpaRepository<PreferenciaNotificacao, Long> {
    Optional<PreferenciaNotificacao> findByTutor(Tutor tutor);
}
