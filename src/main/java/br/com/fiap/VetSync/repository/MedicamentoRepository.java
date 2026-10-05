package br.com.fiap.VetSync.repository;

import br.com.fiap.VetSync.entity.Medicamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {

    List<Medicamento> findByClinica_IdClinicaOrderByNmMedicamentoAsc(Long idClinica);

    /** Medicamentos da clínica mais os legados (ainda sem clínica definida pelo admin). */
    List<Medicamento> findByClinicaIsNullOrClinica_IdClinicaOrderByNmMedicamentoAsc(Long idClinica);
}