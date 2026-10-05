package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.Medicamento;
import br.com.fiap.VetSync.security.PerfilUtils;
import br.com.fiap.VetSync.service.MedicamentoService;
import br.com.fiap.VetSync.service.VeterinarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/medicamentos")
@RequiredArgsConstructor
@Tag(name = "Medicamentos", description = "Catálogo de medicamentos usados nas prescrições")
public class MedicamentoController {

    private final MedicamentoService medicamentoService;
    private final VeterinarioService veterinarioService;

    public record MedicamentoRequest(
            @NotBlank(message = "Nome do medicamento é obrigatório") String nmMedicamento,
            String dsPrincipio,
            BigDecimal vlPrecoRef,
            // Admin escolhe a clínica pelo nome (obrigatório para o admin). Veterinário sempre usa a própria clínica.
            Long idClinica
    ) {}

    public record MedicamentoResponse(
            Long idMedicamento, String nmMedicamento, String dsPrincipio, BigDecimal vlPrecoRef,
            Long idClinica, String nmClinica
    ) {}

    private MedicamentoResponse toResponse(Medicamento m) {
        return new MedicamentoResponse(m.getIdMedicamento(), m.getNmMedicamento(), m.getDsPrincipio(), m.getVlPrecoRef(),
                m.getClinica() != null ? m.getClinica().getIdClinica() : null,
                m.getClinica() != null ? m.getClinica().getNmClinica() : null);
    }

    /** Veterinário só trabalha na própria clínica; para o admin vale a clínica escolhida no pedido. */
    private Long clinicaEfetiva(Authentication authentication, Long idClinicaPedido) {
        if (!PerfilUtils.isVeterinario(authentication)) {
            return idClinicaPedido;
        }
        Long idClinicaVet = veterinarioService.buscarAutenticado(authentication).getClinica().getIdClinica();
        if (idClinicaPedido != null && !idClinicaPedido.equals(idClinicaVet)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "O veterinário só gerencia medicamentos da própria clínica");
        }
        return idClinicaVet;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    @Operation(summary = "Cadastrar medicamento no catálogo")
    public MedicamentoResponse criar(@Valid @RequestBody MedicamentoRequest request, Authentication authentication) {
        Long idClinica = clinicaEfetiva(authentication, request.idClinica());
        return toResponse(medicamentoService.criar(request.nmMedicamento(), request.dsPrincipio(), request.vlPrecoRef(), idClinica));
    }

    @GetMapping
    @Operation(summary = "Listar medicamentos do catálogo",
            description = "Veterinário vê os da própria clínica (e os legados sem clínica). Admin vê todos, ou filtra com ?idClinica=.")
    public List<MedicamentoResponse> listar(Authentication authentication,
                                            @RequestParam(value = "idClinica", required = false) Long idClinica) {
        List<Medicamento> medicamentos;
        if (PerfilUtils.isVeterinario(authentication)) {
            medicamentos = medicamentoService.listarDaClinica(
                    veterinarioService.buscarAutenticado(authentication).getClinica().getIdClinica());
        } else if (idClinica != null) {
            medicamentos = medicamentoService.listarDaClinicaExata(idClinica);
        } else {
            medicamentos = medicamentoService.listar();
        }
        return medicamentos.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar medicamento por ID")
    public MedicamentoResponse buscarPorId(@PathVariable Long id) {
        return toResponse(medicamentoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    @Operation(summary = "Atualizar dados de um medicamento do catálogo")
    public MedicamentoResponse atualizar(@PathVariable Long id, @Valid @RequestBody MedicamentoRequest request,
                                         Authentication authentication) {
        Long idClinica = clinicaEfetiva(authentication, request.idClinica());
        if (PerfilUtils.isVeterinario(authentication)) {
            Medicamento atual = medicamentoService.buscarPorId(id);
            if (atual.getClinica() != null && !atual.getClinica().getIdClinica().equals(idClinica)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esse medicamento pertence a outra clínica");
            }
        }
        return toResponse(medicamentoService.atualizar(id, request.nmMedicamento(), request.dsPrincipio(), request.vlPrecoRef(), idClinica));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remover medicamento do catálogo", description = "Só ADMIN — evita que um vet apague sem querer um medicamento já usado em prescrições.")
    public void deletar(@PathVariable Long id) {
        medicamentoService.deletar(id);
    }
}