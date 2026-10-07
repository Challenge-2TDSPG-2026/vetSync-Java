package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long>, JpaSpecificationExecutor<Auditoria> {
    List<Auditoria> findByDsEntidadeAndIdEntidadeOrderByDtOcorrenciaDesc(String entidade, Long idEntidade);
}