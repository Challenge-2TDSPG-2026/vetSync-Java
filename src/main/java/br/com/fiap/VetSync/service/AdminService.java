package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Admin;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.repository.TutorRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import br.com.fiap.VetSync.repository.ProfissionalEsteticaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final TutorRepository tutorRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final ProfissionalEsteticaRepository esteticaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.admin.bootstrap-key}")
    private String bootstrapKey;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CARACTERES_SENHA = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    public record NovoAdmin(Admin admin, String senhaTemporaria) {}


    public NovoAdmin bootstrap(String nome, String email, String chaveFornecida) {
        if (adminRepository.count() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe pelo menos um admin cadastrado. Use POST /admins autenticado como admin.");
        }
        if (bootstrapKey == null || bootstrapKey.isBlank() || !bootstrapKey.equals(chaveFornecida)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chave de bootstrap inválida");
        }
        return criarInterno(nome, email);
    }

    public NovoAdmin cadastrar(String nome, String email) {
        return criarInterno(nome, email);
    }

    private NovoAdmin criarInterno(String nome, String email) {
        email = email.trim().toLowerCase(Locale.ROOT);
        if (adminRepository.findByDsEmail(email).isPresent() || tutorRepository.existsByDsEmail(email)
                || veterinarioRepository.existsByDsEmail(email) || esteticaRepository.existsByDsEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        String senhaTemporaria = gerarSenhaTemporaria();

        Admin admin = Admin.builder()
                .nmAdmin(nome)
                .dsEmail(email)
                .dsSenha(passwordEncoder.encode(senhaTemporaria))
                .build();
        admin = adminRepository.save(admin);

        emailService.enviar(
                email,
                "Sua conta de administrador VetSync foi criada",
                "Olá, " + nome + "!\n\n"
                        + "Sua conta de administrador foi criada no VetSync.\n\n"
                        + "E-mail de login: " + email + "\n"
                        + "Senha temporária: " + senhaTemporaria + "\n\n"
                        + "Recomendamos alterar essa senha assim que possível."
        );

        return new NovoAdmin(admin, senhaTemporaria);
    }

    private String gerarSenhaTemporaria() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(CARACTERES_SENHA.charAt(RANDOM.nextInt(CARACTERES_SENHA.length())));
        }
        return sb.toString();
    }
}
