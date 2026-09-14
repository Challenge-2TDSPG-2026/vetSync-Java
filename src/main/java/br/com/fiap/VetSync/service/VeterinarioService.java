package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.entity.Veterinario;
import br.com.fiap.VetSync.repository.ClinicaRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VeterinarioService {

    private final VeterinarioRepository veterinarioRepository;
    private final ClinicaRepository clinicaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CARACTERES_SENHA = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";


    public record NovoVeterinario(Veterinario veterinario, String senhaTemporaria) {}

    public NovoVeterinario cadastrar(String nome, String email, Long idClinica) {
        if (veterinarioRepository.existsByDsEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        Clinica clinica = clinicaRepository.findById(idClinica)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clínica não encontrada"));

        String crm = gerarCrmUnico();
        String senhaTemporaria = gerarSenhaTemporaria();

        Veterinario vet = Veterinario.builder()
                .nmVeterinario(nome)
                .nrCrmv(crm)
                .dsEmail(email)
                .dsSenha(passwordEncoder.encode(senhaTemporaria))
                .clinica(clinica)
                .build();
        vet = veterinarioRepository.save(vet);

        emailService.enviar(
                email,
                "Sua conta VetSync foi criada",
                "Olá, " + nome + "!\n\n"
                        + "Sua conta de veterinário foi criada na clínica " + clinica.getNmClinica() + ".\n\n"
                        + "CRM: " + crm + "\n"
                        + "E-mail de login: " + email + "\n"
                        + "Senha temporária: " + senhaTemporaria + "\n\n"
                        + "Recomendamos alterar essa senha assim que possível."
        );

        return new NovoVeterinario(vet, senhaTemporaria);
    }

    private String gerarCrmUnico() {
        String crm;
        do {
            crm = String.format("%06d", RANDOM.nextInt(1_000_000));
        } while (veterinarioRepository.existsByNrCrmv(crm));
        return crm;
    }

    private String gerarSenhaTemporaria() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(CARACTERES_SENHA.charAt(RANDOM.nextInt(CARACTERES_SENHA.length())));
        }
        return sb.toString();
    }

    public Veterinario buscarPorId(Long id) {
        return veterinarioRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veterinário não encontrado com id: " + id)
        );
    }

    public List<Veterinario> listarTodos() {
        return veterinarioRepository.findAll();
    }

    public Veterinario buscarAutenticado(Authentication authentication) {
        return veterinarioRepository.findByDsEmail(authentication.getName()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veterinário autenticado não encontrado")
        );
    }

    public Veterinario atualizar(Long id, String nome, Long idClinica) {
        Veterinario vet = buscarPorId(id);
        if (nome != null && !nome.isBlank()) {
            vet.setNmVeterinario(nome);
        }
        if (idClinica != null) {
            Clinica clinica = clinicaRepository.findById(idClinica)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clínica não encontrada"));
            vet.setClinica(clinica);
        }
        return veterinarioRepository.save(vet);
    }
}