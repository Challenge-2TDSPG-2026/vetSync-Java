package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.service.ClinicaPerfilService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/clinica-admin/perfil")
@PreAuthorize("hasRole('ADMIN_CLINICA')")
@RequiredArgsConstructor
public class ClinicaPerfilController {
    private final ClinicaPerfilService service;
    public record PerfilRequest(@NotBlank String nome, @NotBlank String endereco,
                                @NotBlank String telefone, List<ClinicaPerfilService.HorarioDados> horarios) {}

    @GetMapping
    public ClinicaPerfilService.PerfilDados perfil(Authentication auth) { return service.perfil(auth); }

    @PutMapping
    public ClinicaPerfilService.PerfilDados atualizar(Authentication auth, @Valid @RequestBody PerfilRequest req) {
        return service.atualizar(auth, req.nome(), req.endereco(), req.telefone(), req.horarios());
    }

    @PutMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logo(Authentication auth, @RequestPart("arquivo") MultipartFile arquivo) throws IOException {
        service.atualizarLogo(auth, arquivo.getContentType(), arquivo.getBytes());
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> obterLogo(Authentication auth) {
        Clinica clinica = service.logo(auth);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(clinica.getDsLogoMime()))
                .body(clinica.getDsLogo());
    }
}
