package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.service.ClinicaPerfilService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/minha-clinica")
@PreAuthorize("hasRole('TUTOR')")
@RequiredArgsConstructor
public class MinhaClinicaController {
    private final ClinicaPerfilService service;

    @GetMapping("/perfil")
    public ClinicaPerfilService.PerfilDados perfil(Authentication auth) { return service.perfilDoTutor(auth); }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo(Authentication auth) {
        Clinica clinica = service.logoDoTutor(auth);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(clinica.getDsLogoMime()))
                .body(clinica.getDsLogo());
    }
}
