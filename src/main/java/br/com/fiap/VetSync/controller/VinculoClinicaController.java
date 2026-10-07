package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.entity.VinculoTutorClinica;
import br.com.fiap.VetSync.repository.TutorRepository;
import br.com.fiap.VetSync.service.VinculoClinicaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/vinculos-clinica")
@RequiredArgsConstructor
@Tag(name = "Vínculos de clínica", description = "Cadastro e troca de clínica vinculada ao tutor")
public class VinculoClinicaController {
    private final VinculoClinicaService vinculoService;
    private final TutorRepository tutorRepository;

    public record CodigoRequest(@NotBlank(message = "Código da clínica é obrigatório") String codigo) {}
    public record SessaoResponse(String sessaoVinculo, Long idClinica, String nomeClinica, LocalDateTime expiraEm) {}
    public record VinculoResponse(Long idClinica, String nomeClinica, LocalDateTime inicio, boolean ativo) {}
    public record ContratoRequest(@NotNull(message = "ativo é obrigatório") Boolean ativo) {}
    public record CodigoEmitidoResponse(Long idClinica, String nomeClinica, String codigo) {}
    public record ClinicaResumoResponse(Long idClinica, String nomeClinica, boolean contratanteAtiva,
                                        String statusContrato, boolean codigoAtivo,
                                        LocalDateTime codigoEmitidoEm) {}

    @PostMapping("/validar-codigo")
    public SessaoResponse validarCodigo(@Valid @RequestBody CodigoRequest request) {
        var validado = vinculoService.validarCodigo(request.codigo());
        return new SessaoResponse(validado.sessaoVinculo(), validado.clinica().getIdClinica(), validado.clinica().getNmClinica(), validado.expiraEm());
    }

    @PostMapping("/trocar")
    @PreAuthorize("hasRole('TUTOR')")
    public VinculoResponse trocar(Authentication authentication, @Valid @RequestBody CodigoRequest request) {
        VinculoTutorClinica vinculo = vinculoService.trocarVinculo(authentication.getName(), request.codigo());
        return toResponse(vinculo);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TUTOR')")
    public VinculoResponse meuVinculo(Authentication authentication) {
        Long idTutor = tutorRepository.findByDsEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor não encontrado")).getIdTutor();
        VinculoTutorClinica vinculo = vinculoService.buscarVinculoAtivo(idTutor);
        if (vinculo == null || !vinculo.getClinica().estaContratanteAtiva()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhuma clínica ativa vinculada");
        return toResponse(vinculo);
    }

    @GetMapping("/clinicas")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ClinicaResumoResponse> listarClinicas() {
        return vinculoService.listarClinicas().stream()
                .map(item -> new ClinicaResumoResponse(
                        item.clinica().getIdClinica(), item.clinica().getNmClinica(),
                        item.clinica().estaContratanteAtiva(),
                        item.clinica().estaContratanteAtiva() ? "ATIVO" : "INATIVO",
                        item.codigoAtivo(), item.codigoEmitidoEm()))
                .toList();
    }

    @PostMapping("/clinicas/{idClinica}/codigo")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CodigoEmitidoResponse emitirCodigo(@PathVariable Long idClinica) {
        var emitido = vinculoService.emitirCodigo(idClinica);
        return new CodigoEmitidoResponse(emitido.clinica().getIdClinica(), emitido.clinica().getNmClinica(), emitido.codigo());
    }

    @DeleteMapping("/clinicas/{idClinica}/codigo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void revogarCodigo(@PathVariable Long idClinica) {
        vinculoService.revogarCodigo(idClinica);
    }

    @PatchMapping("/clinicas/{idClinica}/contrato")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void alterarContrato(@PathVariable Long idClinica, @Valid @RequestBody ContratoRequest request) {
        vinculoService.definirContrato(idClinica, request.ativo());
    }

    private VinculoResponse toResponse(VinculoTutorClinica vinculo) {
        Clinica clinica = vinculo.getClinica();
        return new VinculoResponse(clinica.getIdClinica(), clinica.getNmClinica(), vinculo.getDtInicio(), vinculo.estaAtivo() && clinica.estaContratanteAtiva());
    }
}