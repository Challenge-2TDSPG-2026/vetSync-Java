package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.security.RegrasSenha;

import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.security.TokenBlacklist;
import br.com.fiap.VetSync.service.ContaUsuarioService;
import br.com.fiap.VetSync.service.JwtService;
import br.com.fiap.VetSync.service.VinculoClinicaService;
import br.com.fiap.VetSync.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registro (só TUTOR), login, logout e sessão do usuário")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklist tokenBlacklist;
    private final PasswordResetService passwordResetService;
    private final VinculoClinicaService vinculoClinicaService;
    private final ContaUsuarioService contaUsuarioService;

    public record LoginRequest(String email, String senha) {}

    public record RegistrarRequest(
            @NotBlank(message = "Nome é obrigatório")
            @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
            String nome,

            @NotBlank(message = "E-mail é obrigatório")
            @Email(message = "E-mail deve ter formato válido")
            String email,

            @NotBlank(message = "Senha é obrigatória")
            String senha,

            @NotBlank(message = "CPF é obrigatório")
            @Pattern(regexp = "^\\d{11}$", message = "CPF deve conter 11 dígitos numéricos")
            String cpf,

            @Pattern(regexp = "^\\d{10,11}$", message = "Telefone deve conter 10 ou 11 dígitos")
            String telefone,

            @NotBlank(message = "CEP é obrigatório")
            @Pattern(regexp = "^\\d{8}$", message = "CEP deve conter 8 dígitos numéricos")
            String cep,

            @NotBlank(message = "Endereço é obrigatório")
            @Size(max = 150)
            String logradouro,

            @NotBlank(message = "Número é obrigatório")
            @Size(max = 20)
            String numero,

            @Size(max = 100)
            String complemento,

            @NotBlank(message = "Bairro é obrigatório")
            @Size(max = 100)
            String bairro,

            @NotBlank(message = "Cidade é obrigatória")
            @Size(max = 100)
            String cidade,

            @NotBlank(message = "UF é obrigatória")
            @Pattern(regexp = "^[A-Z]{2}$", message = "UF deve conter duas letras maiúsculas")
            String uf
,

            @NotBlank(message = "Confirme o código da clínica antes de concluir o cadastro")
            String sessaoVinculo
    ) {

        public RegistrarRequest(String nome, String email, String senha, String cpf, String telefone, String cep,
                                String logradouro, String numero, String complemento, String bairro, String cidade, String uf) {
            this(nome, email, senha, cpf, telefone, cep, logradouro, numero, complemento, bairro, cidade, uf, null);
        }
    }

    public record AuthResponse(String token, Long idUsuario, String email, String nome, String perfil,
                               boolean temVinculoAtivo, Long idClinica, String cargo,
                               java.util.Set<String> permissoes, boolean trocaSenhaObrigatoria) {
        public AuthResponse(String token, Long idUsuario, String email, String nome, String perfil, boolean temVinculoAtivo) {
            this(token, idUsuario, email, nome, perfil, temVinculoAtivo, null, null, java.util.Set.of(), false);
        }
    }
    public record MeResponse(Long idUsuario, String email, String nome, String perfil,
                             boolean temVinculoAtivo, Long idClinica, String cargo,
                             java.util.Set<String> permissoes, boolean trocaSenhaObrigatoria,
                             boolean googleVinculado) {
        public MeResponse(Long idUsuario, String email, String nome, String perfil, boolean temVinculoAtivo) {
            this(idUsuario, email, nome, perfil, temVinculoAtivo, null, null, java.util.Set.of(), false, false);
        }
        public MeResponse(Long idUsuario, String email, String nome, String perfil, boolean temVinculoAtivo,
                          Long idClinica, String cargo, java.util.Set<String> permissoes,
                          boolean trocaSenhaObrigatoria) {
            this(idUsuario, email, nome, perfil, temVinculoAtivo, idClinica, cargo, permissoes,
                    trocaSenhaObrigatoria, false);
        }
    }

    public record TrocarSenhaInicialRequest(
            @NotBlank String senhaAtual,
            @NotBlank String novaSenha
    ) {}

    public record EsqueciSenhaRequest(
            @NotBlank(message = "E-mail é obrigatório")
            @Email(message = "E-mail deve ter formato válido")
            String email
    ) {}

    public record ValidarCodigoRequest(
            @NotBlank(message = "E-mail é obrigatório")
            @Email(message = "E-mail deve ter formato válido")
            String email,

            @NotBlank(message = "Código é obrigatório")
            String codigo
    ) {}

    public record RedefinirSenhaRequest(
            @NotBlank(message = "E-mail é obrigatório")
            @Email(message = "E-mail deve ter formato válido")
            String email,

            @NotBlank(message = "Código é obrigatório")
            String codigo,

            @NotBlank(message = "Nova senha é obrigatória")
            String novaSenha,

            @NotBlank(message = "Confirmação de senha é obrigatória")
            String confirmarSenha
    ) {}

    @PostMapping("/login")
    @Operation(summary = "Login — e-mail e senha. Funciona para tutor, veterinário, profissional de estética ou admin.")
    public AuthResponse login(@RequestBody LoginRequest req) {
        String identificador = req.email() == null ? null : req.email().trim();
        String senha = req.senha();

        if (identificador == null || identificador.isBlank() || senha == null || senha.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail ou CRMV e senha são obrigatórios");
        }

        String login = contaUsuarioService.resolverLogin(identificador);

        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(login, senha)
            );
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não foi possível autenticar o usuário");
        }
        return montarAuthResponse(login, jwtService.gerarToken(login));
    }

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar novo tutor",
            description = "Registro público é só para TUTOR. Contas de veterinário e de profissional de estética são criadas pelo ADMIN.")
    public AuthResponse registrar(@Valid @RequestBody RegistrarRequest req) {
        if (RegrasSenha.invalida(req.senha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A senha deve ter pelo menos " + RegrasSenha.TAMANHO_MINIMO + " caracteres");
        }

        String emailNormalizado = req.email() == null ? null : req.email().trim().toLowerCase(Locale.ROOT);
        if (emailNormalizado == null || emailNormalizado.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail é obrigatório");
        }
        if (contaUsuarioService.emailJaCadastrado(emailNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        var tutor = Tutor.builder()
                .nmTutor(req.nome())
                .dsEmail(emailNormalizado)
                .dsSenha(passwordEncoder.encode(req.senha()))
                .dsCpf(req.cpf())
                .nrTelefone(req.telefone())
                .nrCep(req.cep())
                .dsLogradouro(req.logradouro())
                .nrEndereco(req.numero())
                .dsComplemento(req.complemento())
                .dsBairro(req.bairro())
                .nmCidade(req.cidade())
                .sgUf(req.uf())
                .build();
        tutor = vinculoClinicaService.criarTutorComVinculo(tutor, req.sessaoVinculo());
        return new AuthResponse(jwtService.gerarToken(emailNormalizado), tutor.getIdTutor(),
                emailNormalizado, req.nome(), "TUTOR", true);
    }

    @PostMapping("/esqueci-senha")
    @Operation(summary = "Passo 1 — solicitar código de redefinição de senha",
            description = "Se o e-mail estiver cadastrado (tutor, veterinário, profissional de estética ou admin), " +
                    "um código de 6 dígitos é enviado para ele, válido por 15 minutos.")
    public ResponseEntity<Map<String, String>> esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest req) {
        passwordResetService.solicitarCodigo(req.email());
        return ResponseEntity.ok(Map.of("mensagem",
                "Se este e-mail estiver cadastrado, você receberá um código de verificação em instantes."));
    }

    @PostMapping("/validar-codigo")
    @Operation(summary = "Passo 2 (opcional) — validar o código recebido por e-mail",
            description = "Usado pelo front para liberar a tela de nova senha somente após um código correto, " +
                    "sem ainda gastar a tentativa de redefinição.")
    public ResponseEntity<Map<String, Object>> validarCodigo(@Valid @RequestBody ValidarCodigoRequest req) {
        passwordResetService.validarCodigo(req.email(), req.codigo());
        return ResponseEntity.ok(Map.of("valido", true));
    }

    @PostMapping("/redefinir-senha")
    @Operation(summary = "Passo 3 — redefinir a senha com o código de verificação",
            description = "Confere novamente o código e, se válido, troca a senha do usuário pela nova senha informada.")
    public ResponseEntity<Map<String, String>> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest req) {
        passwordResetService.redefinirSenha(req.email(), req.codigo(), req.novaSenha(), req.confirmarSenha());
        return ResponseEntity.ok(Map.of("mensagem", "Senha redefinida com sucesso."));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidar o token atual")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tokenBlacklist.revogar(authHeader.substring(7));
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/trocar-senha-inicial")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Trocar a senha temporária da conta de clínica no primeiro acesso")
    public void trocarSenhaInicial(@Valid @RequestBody TrocarSenhaInicialRequest req, Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária");
        }
        contaUsuarioService.trocarSenhaInicial(authentication.getName(), req.senhaAtual(), req.novaSenha());
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado (para restaurar a sessão no app)")
    public MeResponse me(Authentication authentication) {
        String email = authentication.getName();
        var p = contaUsuarioService.buscarPerfil(email);
        boolean googleVinculado = "TUTOR".equals(p.perfil()) && contaUsuarioService.googleVinculado(p.id());
        return new MeResponse(p.id(), email, p.nome(), p.perfil(), p.temVinculoAtivo(), p.idClinica(), p.cargo(),
                p.permissoes(), p.trocaSenhaObrigatoria(), googleVinculado);
    }

    private AuthResponse montarAuthResponse(String email, String token) {
        var p = contaUsuarioService.buscarPerfil(email);
        return new AuthResponse(token, p.id(), email, p.nome(), p.perfil(), p.temVinculoAtivo(), p.idClinica(),
                p.cargo(), p.permissoes(), p.trocaSenhaObrigatoria());
    }
}
