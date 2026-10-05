package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.entity.Medicamento;
import br.com.fiap.VetSync.repository.MedicamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicamentoService {

    private final MedicamentoRepository medicamentoRepository;
    private final ClinicaService clinicaService;

    public Medicamento criar(String nmMedicamento, String dsPrincipio, BigDecimal vlPrecoRef, Long idClinica) {
        Clinica clinica = clinicaService.buscarObrigatoria(idClinica);
        Medicamento medicamento = Medicamento.builder()
                .clinica(clinica)
                .nmMedicamento(nmMedicamento)
                .dsPrincipio(dsPrincipio)
                .vlPrecoRef(vlPrecoRef)
                .build();
        return medicamentoRepository.save(medicamento);
    }

    public Medicamento buscarPorId(Long id) {
        return medicamentoRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicamento não encontrado com id: " + id));
    }

    public List<Medicamento> listar() {
        return medicamentoRepository.findAll();
    }

    /** Catálogo que o veterinário enxerga: o da própria clínica + medicamentos legados sem clínica. */
    public List<Medicamento> listarDaClinica(Long idClinica) {
        return medicamentoRepository.findByClinicaIsNullOrClinica_IdClinicaOrderByNmMedicamentoAsc(idClinica);
    }

    /** Filtro opcional do admin pela clínica escolhida. */
    public List<Medicamento> listarDaClinicaExata(Long idClinica) {
        return medicamentoRepository.findByClinica_IdClinicaOrderByNmMedicamentoAsc(idClinica);
    }

    public Medicamento atualizar(Long id, String nmMedicamento, String dsPrincipio, BigDecimal vlPrecoRef, Long idClinica) {
        Medicamento medicamento = buscarPorId(id);
        medicamento.setClinica(clinicaService.buscarObrigatoria(idClinica));
        medicamento.setNmMedicamento(nmMedicamento);
        medicamento.setDsPrincipio(dsPrincipio);
        medicamento.setVlPrecoRef(vlPrecoRef);
        return medicamentoRepository.save(medicamento);
    }

    public void deletar(Long id) {
        Medicamento medicamento = buscarPorId(id);
        medicamentoRepository.delete(medicamento);
    }
}