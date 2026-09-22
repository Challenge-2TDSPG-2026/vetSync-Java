package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.PetAcesso;
import br.com.fiap.VetSync.entity.StatusAcessoPet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetAcessoRepository extends JpaRepository<PetAcesso, Long> {

    List<PetAcesso> findByPet_IdPetAndDsStatus(Long idPet, StatusAcessoPet dsStatus);

    List<PetAcesso> findByTutor_IdTutorAndDsStatus(Long idTutor, StatusAcessoPet dsStatus);

    Optional<PetAcesso> findByPet_IdPetAndTutor_IdTutor(Long idPet, Long idTutor);

    boolean existsByPet_IdPetAndTutor_DsEmailIgnoreCaseAndDsStatus(
            Long idPet, String dsEmail, StatusAcessoPet dsStatus);
}