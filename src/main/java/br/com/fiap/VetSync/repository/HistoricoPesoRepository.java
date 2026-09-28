package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.HistoricoPeso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoPesoRepository extends JpaRepository<HistoricoPeso, Long> {
    List<HistoricoPeso> findByPet_IdPetOrderByDataMedicaoDesc(Long idPet);
}
