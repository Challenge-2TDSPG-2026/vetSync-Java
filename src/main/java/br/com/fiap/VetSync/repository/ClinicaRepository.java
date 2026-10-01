package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.Clinica;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClinicaRepository extends JpaRepository<Clinica, Long> {

    /** Bloqueia a linha da clínica para serializar emissão/revogação de códigos e mudanças de contrato. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Clinica c where c.idClinica = :idClinica")
    Optional<Clinica> findByIdParaAtualizacao(@Param("idClinica") Long idClinica);
}