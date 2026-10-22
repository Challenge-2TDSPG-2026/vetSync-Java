package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.SiaResumo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiaResumoRepository extends JpaRepository<SiaResumo, Long> {
    List<SiaResumo> findByPet_IdPetAndTutorEmailIgnoreCaseOrderByCriadoEmDesc(Long idPet, String tutorEmail);
    List<SiaResumo> findByEvento_IdEventoAndCompartilhadoEmIsNotNullAndRevogadoEmIsNullOrderByCriadoEmDesc(Long idEvento);
}
