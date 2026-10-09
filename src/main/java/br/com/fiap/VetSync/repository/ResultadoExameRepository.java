package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.ResultadoExame;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResultadoExameRepository extends JpaRepository<ResultadoExame, Long> {
    List<ResultadoExame> findByPet_IdPetOrderByDtResultadoDescIdResultadoDesc(Long idPet);
    Optional<ResultadoExame> findByIdResultadoAndPet_IdPet(Long idResultado, Long idPet);
}