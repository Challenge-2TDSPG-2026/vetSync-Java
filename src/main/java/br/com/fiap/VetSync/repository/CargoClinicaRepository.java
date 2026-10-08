package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.CargoClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CargoClinicaRepository extends JpaRepository<CargoClinica, Long> {
    List<CargoClinica> findByClinica_IdClinicaOrderByNmCargo(Long idClinica);
    boolean existsByClinica_IdClinicaAndNmCargoIgnoreCase(Long idClinica, String nome);
}
