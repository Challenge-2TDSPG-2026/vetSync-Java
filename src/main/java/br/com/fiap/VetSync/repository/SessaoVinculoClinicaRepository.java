package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.CodigoVinculoClinica;
import br.com.fiap.VetSync.entity.SessaoVinculoClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface SessaoVinculoClinicaRepository extends JpaRepository<SessaoVinculoClinica, Long> {

    Optional<SessaoVinculoClinica> findByDsTokenHash(String dsTokenHash);

    /**
     * Consumo atômico: um único UPDATE condicional. Só uma transação consegue marcar a sessão
     * como utilizada; as concorrentes recebem 0 linhas afetadas.
     */
    @Modifying(flushAutomatically = true)
    @Query("update SessaoVinculoClinica s set s.dtUtilizacao = :agora " +
            "where s.dsTokenHash = :hash and s.dtUtilizacao is null and s.dtExpiracao > :agora")
    int consumir(@Param("hash") String hash, @Param("agora") LocalDateTime agora);

    /** Expira as sessões ainda abertas originadas de códigos revogados. */
    @Modifying(flushAutomatically = true)
    @Query("update SessaoVinculoClinica s set s.dtExpiracao = :agora " +
            "where s.codigoVinculo in :codigos and s.dtUtilizacao is null and s.dtExpiracao > :agora")
    int expirarPendentesDosCodigos(@Param("codigos") Collection<CodigoVinculoClinica> codigos,
                                   @Param("agora") LocalDateTime agora);
}