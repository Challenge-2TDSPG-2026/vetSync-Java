package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.controller.AuthController.AuthResponse;
import br.com.fiap.VetSync.service.SocialAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Social", description = "Login com Google e Apple (somente tutores)")
public class SocialAuthController {

    private final SocialAuthService socialAuthService;

    public record SocialLoginRequest(
            @NotBlank(message = "Provedor é obrigatório") String provider,
            @NotBlank(message = "Token de identidade é obrigatório") String idToken
    ) {}

    public record SocialRegistrarRequest(
            @NotBlank(message = "Provedor é obrigatório") String provider,
            @NotBlank(message = "Token de identidade é obrigatório") String idToken,

            @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
            String nome,

            @NotBlank(message = "CPF é obrigatório")
            @Pattern(regexp = "^\\d{11}$", message = "CPF deve conter 11 dígitos numéricos")
            String cpf,

            @Pattern(regexp = "^\\d{10,11}$", message = "Telefone deve conter 10 ou 11 dígitos")
            String telefone,

            @NotBlank(message = "CEP é obrigatório")
            @Pattern(regexp = "^\\d{8}$", message = "CEP deve conter 8 dígitos numéricos")
            String cep,

            @NotBlank(message = "Endereço é obrigatório") @Size(max = 150) String logradouro,
            @NotBlank(message = "Número é obrigatório") @Size(max = 20) String numero,
            @Size(max = 100) String complemento,
            @NotBlank(message = "Bairro é obrigatório") @Size(max = 100) String bairro,
            @NotBlank(message = "Cidade é obrigatória") @Size(max = 100) String cidade,

            @NotBlank(message = "UF é obrigatória")
            @Pattern(regexp = "^[A-Z]{2}$", message = "UF deve conter duas letras maiúsculas")
            String uf,

            @NotBlank(message = "Confirme o código da clínica antes de concluir o cadastro")
            String sessaoVinculo
    ) {}

    public record SocialVincularRequest(
            @NotBlank(message = "Provedor é obrigatório") String provider,
            @NotBlank(message = "Token de identidade é obrigatório") String idToken,
            @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail deve ter formato válido") String email,
            @NotBlank(message = "Senha é obrigatória") String senha
    ) {}

    @PostMapping("/social-login")
    @Operation(summary = "Login com Google/Apple",
            description = "200: identidade vinculada, devolve o mesmo AuthResponse do /auth/login. "
                    + "409: token válido, mas sem vínculo; o corpo traz status = CADASTRO_NECESSARIO, "
                    + "VINCULO_NECESSARIO ou EMAIL_EM_USO. 401: token inválido.")
    public ResponseEntity<Object> socialLogin(@Valid @RequestBody SocialLoginRequest req) {
        var resultado = socialAuthService.entrar(req.provider(), req.idToken());
        if (resultado.sessao() != null) {
            return ResponseEntity.ok(resultado.sessao());
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(resultado.pendencia());
    }

    @PostMapping("/social-registrar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar tutor a partir de uma conta Google/Apple",
            description = "Exige os mesmos dados e o código de clínica do /auth/registrar. E-mail e subject vêm do token validado.")
    public AuthResponse socialRegistrar(@Valid @RequestBody SocialRegistrarRequest req) {
        return socialAuthService.registrar(req);
    }

    @PostMapping("/social-vincular")
    @Operation(summary = "Vincular Google/Apple a uma conta de tutor existente",
            description = "Exige o e-mail e a senha da conta existente para comprovar a posse.")
    public AuthResponse socialVincular(@Valid @RequestBody SocialVincularRequest req) {
        return socialAuthService.vincular(req);
    }
}