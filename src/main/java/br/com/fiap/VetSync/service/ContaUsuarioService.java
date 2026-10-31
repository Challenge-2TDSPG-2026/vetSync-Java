package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.repository.IdentidadeSocialRepository;
import br.com.fiap.VetSync.repository.ProfissionalEsteticaRepository;
import br.com.fiap.VetSync.repository.TutorRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import br.com.fiap.VetSync.security.RegrasSenha;
import br.com.fiap.VetSync.social.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;

/** Identifica a conta (tutor, veterinario, estetica ou admin) por e-mail e concentra as regras de login/senha inicial. */
@Service
@RequiredArgsConstructor
public class ContaUsuarioService {

    private final TutorRepository tutorRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final ProfissionalEsteticaRepository profissionalEsteticaRepository;
    private final AdminRepository adminRepository;
    private final IdentidadeSocialRepository identidadeSocialRepository;
    private final VinculoClinicaService vinculoClinicaService;
    private final PasswordEncoder passwordEncoder;

    /** Dados comuns do perfil, independentes do tipo de conta. */
    public record PerfilConta(Long id, String nome, String perfil, boolean temVinculoAtivo, Long idClinica,
                              String cargo, Set<String> permissoes, boolean trocaSenhaObrigatoria) {}

    /** Aceita e-mail ou CRMV no login e devolve sempre o e-mail normalizado. */
    public String resolverLogin(String identificador) {
        return veterinarioRepository.findByNrCrmv(identificador.toUpperCase(Locale.ROOT))
                .map(v -> v.getDsEmail()).orElse(identificador.toLowerCase(Locale.ROOT));
    }

    public boolean emailJaCadastrado(String email) {
        return tutorRepository.existsByDsEmail(email)
                || veterinarioRepository.existsByDsEmail(email)
                || profissionalEsteticaRepository.existsByDsEmail(email)
                || adminRepository.findByDsEmail(email).isPresent();
    }

    public boolean googleVinculado(Long idTutor) {
        return identidadeSocialRepository.existsByTutor_IdTutorAndProvedor(idTutor, SocialProvider.GOOGLE);
    }

    public PerfilConta buscarPerfil(String email) {
        var tutor = tutorRepository.findByDsEmail(email);
        if (tutor.isPresent()) {
            Long id = tutor.get().getIdTutor();
            return perfilSimples(id, tutor.get().getNmTutor(), "TUTOR", vinculoClinicaService.temVinculoAtivo(id));
        }
        var vet = veterinarioRepository.findByDsEmail(email);
        if (vet.isPresent()) {
            return perfilSimples(vet.get().getIdVeterinario(), vet.get().getNmVeterinario(), "VETERINARIO", true);
        }
        var profissional = profissionalEsteticaRepository.findByDsEmail(email);
        if (profissional.isPresent()) {
            return perfilSimples(profissional.get().getIdProfissionalEstetica(),
                    profissional.get().getNmProfissionalEstetica(), "PROFISSIONAL_ESTETICA", true);
        }
        var admin = adminRepository.findByDsEmail(email);
        if (admin.isPresent()) {
            var a = admin.get();
            return new PerfilConta(a.getIdAdmin(), a.getNmAdmin(), a.ehGlobal() ? "ADMIN" : "ADMIN_CLINICA", true,
                    a.ehGlobal() ? null : a.getClinica().getIdClinica(),
                    a.getCargo() == null ? (Boolean.TRUE.equals(a.getDono()) ? "Dono" : null) : a.getCargo().getNmCargo(),
                    a.getCargo() == null ? Set.of() : a.getCargo().getPermissoes(),
                    Boolean.TRUE.equals(a.getTrocaSenhaObrigatoria()));
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
    }

    public void trocarSenhaInicial(String emailAdmin, String senhaAtual, String novaSenha) {
        var admin = adminRepository.findByDsEmail(emailAdmin).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta de administrador necessária"));
        if (admin.ehGlobal() || !Boolean.TRUE.equals(admin.getTrocaSenhaObrigatoria())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não há troca inicial pendente");
        }
        if (!passwordEncoder.matches(senhaAtual, admin.getDsSenha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual inválida");
        }
        if (RegrasSenha.invalida(novaSenha) || novaSenha.equals(senhaAtual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe uma senha nova com pelo menos " + RegrasSenha.TAMANHO_MINIMO + " caracteres");
        }
        admin.setDsSenha(passwordEncoder.encode(novaSenha));
        admin.setTrocaSenhaObrigatoria(false);
        admin.setDtSenhaAlteradaEm(LocalDateTime.now(ZoneOffset.UTC));
        adminRepository.save(admin);
    }

    private static PerfilConta perfilSimples(Long id, String nome, String perfil, boolean temVinculoAtivo) {
        return new PerfilConta(id, nome, perfil, temVinculoAtivo, null, null, Set.of(), false);
    }
}
