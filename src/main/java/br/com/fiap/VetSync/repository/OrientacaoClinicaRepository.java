package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.OrientacaoClinica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrientacaoClinicaRepository extends JpaRepository<OrientacaoClinica, Long> {
    List<OrientacaoClinica> findByEvento_IdEventoOrderByDtCriacaoDesc(Long idEvento);
    List<OrientacaoClinica> findByEvento_Pet_IdPetOrderByDtCriacaoDesc(Long idPet);
    Optional<OrientacaoClinica> findByIdOrientacaoAndEvento_IdEvento(Long idOrientacao, Long idEvento);
}