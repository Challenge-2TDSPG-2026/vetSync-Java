package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.SiaConversa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiaConversaRepository extends JpaRepository<SiaConversa, String> {
    List<SiaConversa> findByPet_IdPetAndTutorEmailIgnoreCaseOrderByAtualizadoEmDesc(Long idPet, String tutorEmail);
}
