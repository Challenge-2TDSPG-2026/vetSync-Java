package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.CodigoVinculoClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CodigoVinculoClinicaRepository extends JpaRepository<CodigoVinculoClinica, Long> {
    Optional<CodigoVinculoClinica> findByDsCodigoHash(String dsCodigoHash);

    List<CodigoVinculoClinica> findByClinica_IdClinicaAndStAtivo(Long idClinica, String stAtivo);

    @Query("select distinct c.clinica.idClinica from CodigoVinculoClinica c where c.stAtivo = 'A' and c.dtRevogacao is null")
    Set<Long> findIdsClinicasComCodigoAtivo();

    /** Código ativo de cada clínica com a data em que foi emitido (no máximo um por clínica). */
    interface CodigoAtivoView {
        Long getIdClinica();
        LocalDateTime getDtCriacao();
    }

    @Query("select c.clinica.idClinica as idClinica, c.dtCriacao as dtCriacao from CodigoVinculoClinica c "
            + "where c.stAtivo = 'A' and c.dtRevogacao is null")
    List<CodigoAtivoView> findCodigosAtivos();
}