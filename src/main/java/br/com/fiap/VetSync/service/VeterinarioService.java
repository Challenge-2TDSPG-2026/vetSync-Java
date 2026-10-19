package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.entity.Veterinario;
import br.com.fiap.VetSync.repository.ClinicaRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.repository.TutorRepository;
import br.com.fiap.VetSync.repository.ProfissionalEsteticaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VeterinarioService {

    private final VeterinarioRepository veterinarioRepository;
    private final AdminRepository adminRepository;
    private final TutorRepository tutorRepository;
    private final ProfissionalEsteticaRepository esteticaRepository;
    private final ClinicaRepository clinicaRepository;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom RANDOM = new SecureRandom();

    public record NovoVeterinario(Veterinario veterinario) {}

    public NovoVeterinario cadastrar(String nome, String email, String senha, Long idClinica, String especialidade) {
        email = email.trim().toLowerCase(Locale.ROOT);
        if (veterinarioRepository.existsByDsEmail(email) || tutorRepository.existsByDsEmail(email)
                || esteticaRepository.existsByDsEmail(email) || adminRepository.findByDsEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        Clinica clinica = clinicaRepository.findById(idClinica)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clínica não encontrada"));

        String crm = gerarCrmUnico();

        Veterinario vet = Veterinario.builder()
                .nmVeterinario(nome)
                .nrCrmv(crm)
                .dsEmail(email)
                .dsSenha(passwordEncoder.encode(senha))
                .clinica(clinica)
                .dsEspecialidade(especialidade != null && !especialidade.isBlank() ? especialidade : null)
                .build();
        vet = veterinarioRepository.save(vet);

        return new NovoVeterinario(vet);
    }

    private String gerarCrmUnico() {
        String crm;
        do {
            crm = String.format("%06d", RANDOM.nextInt(1_000_000));
        } while (veterinarioRepository.existsByNrCrmv(crm));
        return crm;
    }

    public Veterinario buscarPorId(Long id) {
        return veterinarioRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veterinário não encontrado com id: " + id)
        );
    }

    public List<Veterinario> listarTodos() {
        return veterinarioRepository.findAll();
    }

    public List<Veterinario> listarTodos(String especialidade) {
        if (especialidade == null || especialidade.isBlank()) {
            return listarTodos();
        }
        return veterinarioRepository.findByDsEspecialidade(especialidade);
    }

    public Veterinario buscarAutenticado(Authentication authentication) {
        return veterinarioRepository.findByDsEmail(authentication.getName()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veterinário autenticado não encontrado")
        );
    }

    public Veterinario atualizar(Long id, String nome, Long idClinica) {
        return atualizar(id, nome, idClinica, null);
    }

    public Veterinario atualizar(Long id, String nome, Long idClinica, String especialidade) {
        Veterinario vet = buscarPorId(id);
        if (nome != null && !nome.isBlank()) {
            vet.setNmVeterinario(nome);
        }
        if (idClinica != null) {
            Clinica clinica = clinicaRepository.findById(idClinica)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clínica não encontrada"));
            vet.setClinica(clinica);
        }
        if (especialidade != null && !especialidade.isBlank()) {
            vet.setDsEspecialidade(especialidade);
        }
        return veterinarioRepository.save(vet);
    }
}
