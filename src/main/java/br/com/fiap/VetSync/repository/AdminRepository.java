package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import java.util.List;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    @EntityGraph(attributePaths = {"cargo"})
    Optional<Admin> findByDsEmail(String dsEmail);
    List<Admin> findByClinica_IdClinicaOrderByNmAdmin(Long idClinica);
    boolean existsByClinica_IdClinicaAndDonoTrue(Long idClinica);
    boolean existsByCargo_IdCargo(Long idCargo);
}
