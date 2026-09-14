package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.entity.ProfissionalEstetica;
import br.com.fiap.VetSync.repository.ClinicaRepository;
import br.com.fiap.VetSync.repository.ProfissionalEsteticaRepository;
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
public class ProfissionalEsteticaService {

    private final ProfissionalEsteticaRepository profissionalEsteticaRepository;
    private final ClinicaRepository clinicaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CARACTERES_SENHA = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";


    public record NovoProfissionalEstetica(ProfissionalEstetica profissional, String senhaTemporaria) {}

    public NovoProfissionalEstetica cadastrar(String nome, String email, Long idClinica) {
        if (profissionalEsteticaRepository.existsByDsEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        Clinica clinica = clinicaRepository.findById(idClinica)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clínica não encontrada"));

        String registro = gerarRegistroUnico();
        String senhaTemporaria = gerarSenhaTemporaria();

        ProfissionalEstetica profissional = ProfissionalEstetica.builder()
                .nmProfissionalEstetica(nome)
                .nrRegistro(registro)
                .dsEmail(email)
                .dsSenha(passwordEncoder.encode(senhaTemporaria))
                .clinica(clinica)
                .build();
        profissional = profissionalEsteticaRepository.save(profissional);

        emailService.enviar(
                email,
                "Sua conta VetSync foi criada",
                "Olá, " + nome + "!\n\n"
                        + "Sua conta de profissional de estética foi criada na clínica " + clinica.getNmClinica() + ".\n\n"
                        + "Registro: " + registro + "\n"
                        + "E-mail de login: " + email + "\n"
                        + "Senha temporária: " + senhaTemporaria + "\n\n"
                        + "Recomendamos alterar essa senha assim que possível."
        );

        return new NovoProfissionalEstetica(profissional, senhaTemporaria);
    }

    private String gerarRegistroUnico() {
        String registro;
        do {
            registro = "PE" + String.format("%06d", RANDOM.nextInt(1_000_000));
        } while (profissionalEsteticaRepository.existsByNrRegistro(registro));
        return registro;
    }

    private String gerarSenhaTemporaria() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(CARACTERES_SENHA.charAt(RANDOM.nextInt(CARACTERES_SENHA.length())));
        }
        return sb.toString();
    }

    public ProfissionalEstetica buscarPorId(Long id) {
        return profissionalEsteticaRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional de estética não encontrado com id: " + id)
        );
    }

    public List<ProfissionalEstetica> listarTodos() {
        return profissionalEsteticaRepository.findAll();
    }

    public ProfissionalEstetica buscarAutenticado(Authentication authentication) {
        return profissionalEsteticaRepository.findByDsEmail(authentication.getName()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional de estética autenticado não encontrado")
        );
    }

    public ProfissionalEstetica atualizar(Long id, String nome, Long idClinica) {
        ProfissionalEstetica profissional = buscarPorId(id);
        if (nome != null && !nome.isBlank()) {
            profissional.setNmProfissionalEstetica(nome);
        }
        if (idClinica != null) {
            Clinica clinica = clinicaRepository.findById(idClinica)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clínica não encontrada"));
            profissional.setClinica(clinica);
        }
        return profissionalEsteticaRepository.save(profissional);
    }
}