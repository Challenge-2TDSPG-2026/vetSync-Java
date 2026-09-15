package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.ProfissionalEstetica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfissionalEsteticaRepository extends JpaRepository<ProfissionalEstetica, Long> {
    Optional<ProfissionalEstetica> findByDsEmail(String dsEmail);
    boolean existsByDsEmail(String dsEmail);
    boolean existsByNrRegistro(String nrRegistro);

    @Query("""
            SELECT p FROM ProfissionalEstetica p
            JOIN p.servicos s
            WHERE s.idServico IN :idsServico
            GROUP BY p
            HAVING COUNT(DISTINCT s.idServico) = :qtdServicos
            """)
    List<ProfissionalEstetica> findQueAtendemTodosOsServicos(
            @Param("idsServico") List<Long> idsServico,
            @Param("qtdServicos") long qtdServicos
    );
}