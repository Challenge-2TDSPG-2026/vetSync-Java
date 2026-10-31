package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.service.ClinicaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

@RestController
@RequestMapping("/admin-global/clinicas")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class GlobalClinicaController {
    private final ClinicaService clinicaService;

    public record CriarRequest(@NotBlank String nome,
                               @NotBlank @Pattern(regexp = "^\\d{14}$") String cnpj,
                               @NotBlank String cidade,
                               @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String uf) {}
    public record ClinicaCriada(Long idClinica, String nomeClinica, String statusContrato) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicaCriada criar(@Valid @RequestBody CriarRequest req) {
        Clinica clinica = clinicaService.criar(req.nome(), req.cnpj(), req.cidade(), req.uf());
        return new ClinicaCriada(clinica.getIdClinica(), clinica.getNmClinica(), "INATIVO");
    }
}
