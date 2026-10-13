package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.Auditoria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long>, JpaSpecificationExecutor<Auditoria> {
    List<Auditoria> findByDsEntidadeAndIdEntidadeOrderByDtOcorrenciaDesc(String entidade, Long idEntidade);

    /**
     * Atividade de um pet: a auditoria é indexada pela entidade (acesso, evento, pet), então o feed do pet junta
     * os acessos e eventos dele com o que é registrado direto pelo id do pet (prontuário e convites).
     * As listas nunca podem ser vazias (IN () é inválido em alguns bancos): quem chama passa um id sentinela.
     */
    @Query("""
            select a from Auditoria a
            where (a.dsEntidade = 'ACESSO_PET' and a.idEntidade in :idsAcesso)
               or (a.dsEntidade = 'EVENTO' and a.idEntidade in :idsEvento)
               or (a.dsEntidade in ('PRONTUARIO', 'PET_CONVITE') and a.idEntidade = :idPet)
            order by a.dtOcorrencia desc, a.idAuditoria desc
            """)
    List<Auditoria> findAtividadeDoPet(@Param("idPet") Long idPet,
                                       @Param("idsAcesso") List<Long> idsAcesso,
                                       @Param("idsEvento") List<Long> idsEvento,
                                       Pageable pageable);
}