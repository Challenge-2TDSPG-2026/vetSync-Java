package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.PetConvite;
import br.com.fiap.VetSync.entity.StatusConvitePet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetConviteRepository extends JpaRepository<PetConvite, Long> {

    Optional<PetConvite> findByDsTokenHash(String dsTokenHash);

    List<PetConvite> findByPet_IdPetOrderByDtCriacaoDesc(Long idPet);

    List<PetConvite> findByPet_IdPetAndDsEmailDestinoIgnoreCaseAndDsStatus(
            Long idPet, String dsEmailDestino, StatusConvitePet dsStatus);
}