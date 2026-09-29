package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.DispositivoPush;
import br.com.fiap.VetSync.entity.PlataformaPush;
import br.com.fiap.VetSync.entity.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DispositivoPushRepository extends JpaRepository<DispositivoPush, Long> {

    Optional<DispositivoPush> findByTutorAndTokenAndPlataforma(Tutor tutor, String token, PlataformaPush plataforma);

    /** Lista: a unicidade é (tutor, token, plataforma), então o mesmo token pode ter mais de uma linha. */
    List<DispositivoPush> findByTutorAndToken(Tutor tutor, String token);

    /** Mesmo aparelho logado antes em outra conta: essas linhas não podem continuar recebendo push. */
    @Query("""
            select d from DispositivoPush d
             where d.token = :token
               and d.tutor.idTutor <> :idTutor
               and d.ativo = true
            """)
    List<DispositivoPush> findAtivosDeOutrosTutores(@Param("token") String token,
                                                    @Param("idTutor") Long idTutor);
}
