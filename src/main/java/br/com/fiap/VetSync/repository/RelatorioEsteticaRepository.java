package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.RelatorioEstetica;
import br.com.fiap.VetSync.entity.StatusRelatorioEstetica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelatorioEsteticaRepository extends JpaRepository<RelatorioEstetica, Long> {
    List<RelatorioEstetica> findByEvento_IdEvento(Long idEvento);
    List<RelatorioEstetica> findByEvento_Pet_Tutor_DsEmailOrderByIdRelatorioDesc(String email);
    List<RelatorioEstetica> findByEvento_ProfissionalEstetica_DsEmailOrderByIdRelatorioDesc(String email);
    List<RelatorioEstetica> findByDsStatusOrderByIdRelatorioAsc(StatusRelatorioEstetica status);
}