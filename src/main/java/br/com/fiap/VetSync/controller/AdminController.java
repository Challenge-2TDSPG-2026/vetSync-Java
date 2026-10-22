package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admins")
@RequiredArgsConstructor
@Tag(name = "Admins", description = "Bootstrap do primeiro admin e criação de novos admins")
public class AdminController {

    private final AdminService adminService;

    public record AdminRequest(
            @NotBlank(message = "Nome é obrigatório") String nome,
            @NotBlank(message = "E-mail é obrigatório") @Email String email
    ) {}

    public record AdminBootstrapRequest(
            @NotBlank(message = "Nome é obrigatório") String nome,
            @NotBlank(message = "E-mail é obrigatório") @Email String email,
            @NotBlank(message = "Chave de bootstrap é obrigatória") String chave
    ) {}

    public record AdminResponse(Long idAdmin, String nome, String email, String senhaTemporaria) {}

    @PostMapping("/bootstrap")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria o primeiro admin do sistema",
            description = "Público, mas só funciona se ainda não existir nenhum admin E a chave bater com ADMIN_BOOTSTRAP_KEY do ambiente. Depois do primeiro admin, esse endpoint sempre retorna 409.")
    public AdminResponse bootstrap(@Valid @RequestBody AdminBootstrapRequest request) {
        var novo = adminService.bootstrap(request.nome(), request.email(), request.chave());
        return new AdminResponse(novo.admin().getIdAdmin(), novo.admin().getNmAdmin(), novo.admin().getDsEmail(), novo.senhaTemporaria());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin autenticado cria outro admin", description = "Gera senha temporária e envia por e-mail, igual ao cadastro de veterinário.")
    public AdminResponse criar(@Valid @RequestBody AdminRequest request) {
        var novo = adminService.cadastrar(request.nome(), request.email());
        return new AdminResponse(novo.admin().getIdAdmin(), novo.admin().getNmAdmin(), novo.admin().getDsEmail(), novo.senhaTemporaria());
    }
}
