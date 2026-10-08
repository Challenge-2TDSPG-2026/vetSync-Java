package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.HorarioClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HorarioClinicaRepository extends JpaRepository<HorarioClinica, Long> {
    List<HorarioClinica> findByClinica_IdClinicaOrderByNrDiaSemanaAscHrInicioAsc(Long idClinica);
    List<HorarioClinica> findByClinica_IdClinicaAndNrDiaSemana(Long idClinica, Integer dia);
    void deleteByClinica_IdClinica(Long idClinica);
}
