package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.service.CatalogoClinicaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/clinica-admin/servicos")
@PreAuthorize("hasRole('ADMIN_CLINICA')")
@RequiredArgsConstructor
public class CatalogoClinicaController {
    private final CatalogoClinicaService service;
    public record ServicoRequest(String codigoPadrao, String nomePersonalizado, String categoria,
                                 @NotNull Integer duracaoMinutos, List<Long> veterinarios,
                                 List<Long> profissionaisEstetica, boolean ativo) {}

    @GetMapping("/modelos")
    public List<CatalogoClinicaService.Modelo> modelos() { return service.modelos(); }

    @GetMapping
    public List<CatalogoClinicaService.ServicoDados> listar(Authentication auth) { return service.listar(auth); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogoClinicaService.ServicoDados criar(Authentication auth, @Valid @RequestBody ServicoRequest req) {
        return salvar(auth, null, req);
    }

    @PutMapping("/{idServico}")
    public CatalogoClinicaService.ServicoDados editar(Authentication auth, @PathVariable Long idServico,
                                                       @Valid @RequestBody ServicoRequest req) {
        return salvar(auth, idServico, req);
    }

    private CatalogoClinicaService.ServicoDados salvar(Authentication auth, Long id, ServicoRequest r) {
        return service.salvar(auth, id, r.codigoPadrao(), r.nomePersonalizado(), r.categoria(), r.duracaoMinutos(),
                r.veterinarios(), r.profissionaisEstetica(), r.ativo());
    }
}
