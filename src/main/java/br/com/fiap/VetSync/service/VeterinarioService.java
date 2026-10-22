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

    public record NovoVeterinario(Veterinario veterinario) {}

    public NovoVeterinario cadastrar(String nome, String email, String senha, String crmv, Long idClinica, String especialidade) {
        email = email.trim().toLowerCase(Locale.ROOT);
        crmv = crmv.trim().toUpperCase(Locale.ROOT);
        if (veterinarioRepository.existsByDsEmail(email) || tutorRepository.existsByDsEmail(email)
                || esteticaRepository.existsByDsEmail(email) || adminRepository.findByDsEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        if (veterinarioRepository.existsByNrCrmv(crmv)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CRMV já cadastrado");
        }
        Clinica clinica = clinicaRepository.findById(idClinica)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clínica não encontrada"));

        Veterinario vet = Veterinario.builder()
                .nmVeterinario(nome)
                .nrCrmv(crmv)
                .dsEmail(email)
                .dsSenha(passwordEncoder.encode(senha))
                .clinica(clinica)
                .dsEspecialidade(especialidade != null && !especialidade.isBlank() ? especialidade : null)
                .build();
        vet = veterinarioRepository.save(vet);

        return new NovoVeterinario(vet);
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
