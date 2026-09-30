package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.CodigoVinculoClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CodigoVinculoClinicaRepository extends JpaRepository<CodigoVinculoClinica, Long> {
    Optional<CodigoVinculoClinica> findByDsCodigoHash(String dsCodigoHash);
}
