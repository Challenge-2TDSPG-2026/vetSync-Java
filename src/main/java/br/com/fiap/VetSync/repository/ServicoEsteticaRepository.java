package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.ServicoEstetica;
import br.com.fiap.VetSync.entity.TipoServicoEstetica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicoEsteticaRepository extends JpaRepository<ServicoEstetica, Long> {
    List<ServicoEstetica> findByTpServico(TipoServicoEstetica tpServico);
}