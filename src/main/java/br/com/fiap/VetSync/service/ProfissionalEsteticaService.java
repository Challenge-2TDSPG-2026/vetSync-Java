package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.security.TokenUtils;
import br.com.fiap.VetSync.exception.Erros;
import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.entity.ProfissionalEstetica;
import br.com.fiap.VetSync.repository.ClinicaRepository;
import br.com.fiap.VetSync.repository.ProfissionalEsteticaRepository;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.repository.TutorRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProfissionalEsteticaService {

    private final ProfissionalEsteticaRepository profissionalEsteticaRepository;
    private final AdminRepository adminRepository;
    private final TutorRepository tutorRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final ClinicaRepository clinicaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ServicoEsteticaService servicoEsteticaService;



    public record NovoProfissionalEstetica(ProfissionalEstetica profissional, String senhaTemporaria) {}

    public NovoProfissionalEstetica cadastrar(String nome, String email, Long idClinica) {
        email = email.trim().toLowerCase(Locale.ROOT);
        if (profissionalEsteticaRepository.existsByDsEmail(email) || tutorRepository.existsByDsEmail(email)
                || veterinarioRepository.existsByDsEmail(email) || adminRepository.findByDsEmail(email).isPresent()) {
            throw Erros.emailJaCadastrado();
        }
        Clinica clinica = clinicaRepository.findById(idClinica)
                .orElseThrow(Erros::clinicaNaoEncontrada);

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
            registro = "PE" + String.format("%06d", TokenUtils.inteiroAleatorio(1_000_000));
        } while (profissionalEsteticaRepository.existsByNrRegistro(registro));
        return registro;
    }

    private String gerarSenhaTemporaria() {
        return TokenUtils.gerarSenhaTemporaria(10);
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
                    .orElseThrow(Erros::clinicaNaoEncontrada);
            profissional.setClinica(clinica);
        }
        return profissionalEsteticaRepository.save(profissional);
    }

    public ProfissionalEstetica definirServicos(Long id, List<Long> idsServico) {
        ProfissionalEstetica profissional = buscarPorId(id);
        profissional.setServicos(servicoEsteticaService.buscarVarios(idsServico));
        return profissionalEsteticaRepository.save(profissional);
    }

    public List<ProfissionalEstetica> listarPorServicos(List<Long> idsServico) {
        if (idsServico == null || idsServico.isEmpty()) {
            return listarTodos();
        }
        List<Long> unicos = idsServico.stream().distinct().toList();
        return profissionalEsteticaRepository.findQueAtendemTodosOsServicos(unicos, unicos.size());
    }
}
