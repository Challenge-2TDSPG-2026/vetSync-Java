package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.Notificacao;
import br.com.fiap.VetSync.entity.Tutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    Page<Notificacao> findByTutorAndLida(Tutor tutor, boolean lida, Pageable pageable);

    Page<Notificacao> findByTutor(Tutor tutor, Pageable pageable);

    /** Busca já limitada ao tutor: notificação de outro tutor simplesmente "não existe". */
    Optional<Notificacao> findByIdNotificacaoAndTutor(Long idNotificacao, Tutor tutor);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Notificacao n
               set n.lida = true
             where n.tutor = :tutor
               and n.lida = false
            """)
    int markAllAsReadByTutor(@Param("tutor") Tutor tutor);
}
