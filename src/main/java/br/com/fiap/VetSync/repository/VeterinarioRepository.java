package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.Veterinario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface VeterinarioRepository extends JpaRepository<Veterinario, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Veterinario v where v.idVeterinario = :id")
    Optional<Veterinario> findByIdForBooking(@Param("id") Long id);
    Optional<Veterinario> findByDsEmail(String dsEmail);
    boolean existsByDsEmail(String dsEmail);
    boolean existsByNrCrmv(String nrCrmv);
    Optional<Veterinario> findByNrCrmv(String nrCrmv);
    List<Veterinario> findByDsEspecialidade(String dsEspecialidade);
    List<Veterinario> findByClinica_IdClinicaOrderByNmVeterinario(Long idClinica);
}
