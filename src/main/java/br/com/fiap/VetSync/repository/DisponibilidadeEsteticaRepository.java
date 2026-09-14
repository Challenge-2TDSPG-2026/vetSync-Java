package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.DisponibilidadeEstetica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisponibilidadeEsteticaRepository extends JpaRepository<DisponibilidadeEstetica, Long> {
    List<DisponibilidadeEstetica> findByProfissionalEstetica_IdProfissionalEstetica(Long idProfissionalEstetica);
}