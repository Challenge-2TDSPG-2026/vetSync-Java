package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.service.EquipeClinicaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class EquipeClinicaController {
    private final EquipeClinicaService service;

    public record ConviteDono(@NotBlank String nome, @NotBlank @Email String email) {}
    public record ConviteMembro(@NotBlank String nome, @NotBlank @Email String email,
                                @NotNull Long idCargo, boolean senhaTemporaria, String senha) {}
    public record CargoRequest(@NotBlank String nome, Set<String> permissoes) {}
    public record MembroRequest(@NotNull Long idCargo, boolean ativo) {}

    @PostMapping("/admin-global/clinicas/{idClinica}/dono")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public EquipeClinicaService.MembroDados criarDono(@PathVariable Long idClinica,
                                                       @Valid @RequestBody ConviteDono req) {
        return service.criarDono(idClinica, req.nome(), req.email());
    }

    @GetMapping("/clinica-admin/equipe")
    @PreAuthorize("hasRole('ADMIN_CLINICA')")
    public List<EquipeClinicaService.MembroDados> equipe(Authentication authentication) {
        return service.listarMembros(authentication);
    }

    @PostMapping("/clinica-admin/equipe")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_CLINICA')")
    public EquipeClinicaService.MembroDados convidar(Authentication authentication,
                                                      @Valid @RequestBody ConviteMembro req) {
        return service.convidar(authentication, req.nome(), req.email(), req.idCargo(),
                req.senhaTemporaria(), req.senha());
    }

    @PatchMapping("/clinica-admin/equipe/{idAdmin}")
    @PreAuthorize("hasRole('ADMIN_CLINICA')")
    public EquipeClinicaService.MembroDados alterar(Authentication authentication, @PathVariable Long idAdmin,
                                                     @Valid @RequestBody MembroRequest req) {
        return service.alterarMembro(authentication, idAdmin, req.idCargo(), req.ativo());
    }

    @GetMapping("/clinica-admin/cargos")
    @PreAuthorize("hasRole('ADMIN_CLINICA')")
    public List<EquipeClinicaService.CargoDados> cargos(Authentication authentication) {
        return service.listarCargos(authentication);
    }

    @PostMapping("/clinica-admin/cargos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_CLINICA')")
    public EquipeClinicaService.CargoDados criarCargo(Authentication authentication,
                                                       @Valid @RequestBody CargoRequest req) {
        return service.salvarCargo(authentication, null, req.nome(), req.permissoes());
    }

    @PutMapping("/clinica-admin/cargos/{idCargo}")
    @PreAuthorize("hasRole('ADMIN_CLINICA')")
    public EquipeClinicaService.CargoDados editarCargo(Authentication authentication, @PathVariable Long idCargo,
                                                        @Valid @RequestBody CargoRequest req) {
        return service.salvarCargo(authentication, idCargo, req.nome(), req.permissoes());
    }
}
