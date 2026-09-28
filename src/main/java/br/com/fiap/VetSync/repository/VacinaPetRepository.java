package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.VacinaPet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VacinaPetRepository extends JpaRepository<VacinaPet, Long> {
    List<VacinaPet> findByPet_IdPetOrderByDtAplicacaoDesc(Long idPet);
}
