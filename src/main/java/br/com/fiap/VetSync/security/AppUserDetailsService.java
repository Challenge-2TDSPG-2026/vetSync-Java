package br.com.fiap.VetSync.security;

import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.repository.ProfissionalEsteticaRepository;
import br.com.fiap.VetSync.repository.TutorRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final TutorRepository tutorRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final ProfissionalEsteticaRepository profissionalEsteticaRepository;
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String emailNormalizado = normalizeEmail(email);

        return tutorRepository.findByDsEmail(emailNormalizado)
                .map(tutor -> User.builder()
                        .username(tutor.getDsEmail())
                        .password(tutor.getDsSenha() != null ? tutor.getDsSenha() : "")
                        .roles("TUTOR")
                        .build())
                .or(() -> veterinarioRepository.findByDsEmail(emailNormalizado)
                        .map(vet -> User.builder()
                                .username(vet.getDsEmail())
                                .password(vet.getDsSenha() != null ? vet.getDsSenha() : "")
                                .roles("VETERINARIO")
                                .build()))
                .or(() -> profissionalEsteticaRepository.findByDsEmail(emailNormalizado)
                        .map(prof -> User.builder()
                                .username(prof.getDsEmail())
                                .password(prof.getDsSenha() != null ? prof.getDsSenha() : "")
                                .roles("PROFISSIONAL_ESTETICA")
                                .build()))
                .or(() -> adminRepository.findByDsEmail(emailNormalizado)
                        .map(admin -> User.builder()
                                .username(admin.getDsEmail())
                                .password(admin.getDsSenha() != null ? admin.getDsSenha() : "")
                                .roles("ADMIN")
                                .build()))
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}