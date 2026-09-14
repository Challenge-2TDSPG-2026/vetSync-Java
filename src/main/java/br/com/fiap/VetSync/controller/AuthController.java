package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.Admin;
import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.repository.ProfissionalEsteticaRepository;
import br.com.fiap.VetSync.repository.TutorRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import br.com.fiap.VetSync.security.TokenBlacklist;
import br.com.fiap.VetSync.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registro (só TUTOR), login, logout e sessão do usuário")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final TutorRepository tutorRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklist tokenBlacklist;
    private final ProfissionalEsteticaRepository profissionalEsteticaRepository;

    private static final int SENHA_MIN_LENGTH = 6;

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
            String telefone
    ) {}

    public record AuthResponse(String token, Long idUsuario, String email, String nome, String perfil) {}
    public record MeResponse(Long idUsuario, String email, String nome, String perfil) {}

    @PostMapping("/login")
    @Operation(summary = "Login — e-mail e senha. Funciona para tutor, veterinário, profissional de estética ou admin.")
    public AuthResponse login(@RequestBody LoginRequest req) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.senha())
            );
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
        }
        return montarAuthResponse(req.email(), jwtService.gerarToken(req.email()));
    }

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar novo tutor",
            description = "Registro público é só para TUTOR. Contas de veterinário e de profissional de estética são criadas pelo ADMIN.")
    public AuthResponse registrar(@Valid @RequestBody RegistrarRequest req) {
        if (req.senha() == null || req.senha().length() < SENHA_MIN_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A senha deve ter pelo menos " + SENHA_MIN_LENGTH + " caracteres");
        }
        if (tutorRepository.existsByDsEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        var tutor = Tutor.builder()
                .nmTutor(req.nome())
                .dsEmail(req.email())
                .dsSenha(passwordEncoder.encode(req.senha()))
                .dsCpf(req.cpf())
                .nrTelefone(req.telefone())
                .build();
        tutor = tutorRepository.save(tutor);
        return new AuthResponse(jwtService.gerarToken(req.email()), tutor.getIdTutor(),
                req.email(), req.nome(), "TUTOR");
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidar o token atual")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tokenBlacklist.revogar(authHeader.substring(7));
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado (para restaurar a sessão no app)")
    public MeResponse me(Authentication authentication) {
        String email = authentication.getName();

        var tutor = tutorRepository.findByDsEmail(email);
        if (tutor.isPresent()) {
            return new MeResponse(tutor.get().getIdTutor(), email, tutor.get().getNmTutor(), "TUTOR");
        }
        var vet = veterinarioRepository.findByDsEmail(email);
        if (vet.isPresent()) {
            return new MeResponse(vet.get().getIdVeterinario(), email, vet.get().getNmVeterinario(), "VETERINARIO");
        }
        var profissional = profissionalEsteticaRepository.findByDsEmail(email);
        if (profissional.isPresent()) {
            return new MeResponse(profissional.get().getIdProfissionalEstetica(), email,
                    profissional.get().getNmProfissionalEstetica(), "PROFISSIONAL_ESTETICA");
        }
        var admin = adminRepository.findByDsEmail(email);
        if (admin.isPresent()) {
            return new MeResponse(admin.get().getIdAdmin(), email, admin.get().getNmAdmin(), "ADMIN");
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
    }

    private AuthResponse montarAuthResponse(String email, String token) {
        var tutor = tutorRepository.findByDsEmail(email);
        if (tutor.isPresent()) {
            return new AuthResponse(token, tutor.get().getIdTutor(), email, tutor.get().getNmTutor(), "TUTOR");
        }
        var vet = veterinarioRepository.findByDsEmail(email);
        if (vet.isPresent()) {
            return new AuthResponse(token, vet.get().getIdVeterinario(), email, vet.get().getNmVeterinario(), "VETERINARIO");
        }
        var profissional = profissionalEsteticaRepository.findByDsEmail(email);
        if (profissional.isPresent()) {
            return new AuthResponse(token, profissional.get().getIdProfissionalEstetica(), email,
                    profissional.get().getNmProfissionalEstetica(), "PROFISSIONAL_ESTETICA");
        }
        var admin = adminRepository.findByDsEmail(email);
        if (admin.isPresent()) {
            return new AuthResponse(token, admin.get().getIdAdmin(), email, admin.get().getNmAdmin(), "ADMIN");
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
    }
}