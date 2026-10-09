package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.ProntuarioCompartilhado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProntuarioCompartilhadoRepository extends JpaRepository<ProntuarioCompartilhado, Long> {
    Optional<ProntuarioCompartilhado> findByTokenHash(String tokenHash);
    List<ProntuarioCompartilhado> findByPet_IdPetOrderByCriadaEmDesc(Long idPet);
    Optional<ProntuarioCompartilhado> findByIdCompartilhamentoAndPet_IdPet(Long id, Long idPet);
}