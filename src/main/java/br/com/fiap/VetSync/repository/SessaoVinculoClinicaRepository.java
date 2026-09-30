package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.SessaoVinculoClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface SessaoVinculoClinicaRepository extends JpaRepository<SessaoVinculoClinica, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SessaoVinculoClinica> findByDsTokenHash(String dsTokenHash);
}
