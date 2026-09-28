package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.service.RelatorioClinicaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/veterinarios/me/relatorios")
@RequiredArgsConstructor
@Tag(name = "Relatórios da clínica")
public class RelatorioClinicaController {
    private final RelatorioClinicaService service;

    @GetMapping("/resumo")
    @PreAuthorize("hasRole('VETERINARIO')")
    public RelatorioClinicaService.Resumo resumo(Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return service.gerar(authentication.getName(), inicio, fim);
    }
}
