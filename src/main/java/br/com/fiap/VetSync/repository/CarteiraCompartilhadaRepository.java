package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.CarteiraCompartilhada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarteiraCompartilhadaRepository extends JpaRepository<CarteiraCompartilhada, Long> {

    Optional<CarteiraCompartilhada> findByTokenHash(String tokenHash);

    List<CarteiraCompartilhada> findByPet_IdPetAndRevogadaEmIsNullOrderByCriadaEmDesc(Long idPet);
}