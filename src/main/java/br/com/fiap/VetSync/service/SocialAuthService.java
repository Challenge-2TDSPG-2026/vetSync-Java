package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.controller.AuthController.AuthResponse;
import br.com.fiap.VetSync.controller.SocialAuthController.SocialRegistrarRequest;
import br.com.fiap.VetSync.controller.SocialAuthController.SocialVincularRequest;
import br.com.fiap.VetSync.entity.IdentidadeSocial;
import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.repository.IdentidadeSocialRepository;
import br.com.fiap.VetSync.repository.ProfissionalEsteticaRepository;
import br.com.fiap.VetSync.repository.TutorRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import br.com.fiap.VetSync.social.SocialIdentity;
import br.com.fiap.VetSync.social.SocialProvider;
import br.com.fiap.VetSync.social.SocialTokenVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

/**
 * Login social (Google/Apple) para TUTOR. Regras:
 * - a identidade é (provedor, subject) de um token validado, nunca o e-mail;
 * - nenhuma conta é criada ou vinculada automaticamente por coincidência de e-mail;
 * - vincular a uma conta existente exige provar a posse dela (e-mail e senha);
 * - criar conta nova exige os mesmos dados e o código de clínica do cadastro tradicional.
 */
@Service
@RequiredArgsConstructor
public class SocialAuthService {

    public static final String CADASTRO_NECESSARIO = "CADASTRO_NECESSARIO";
    public static final String VINCULO_NECESSARIO = "VINCULO_NECESSARIO";
    public static final String EMAIL_EM_USO = "EMAIL_EM_USO";

    /** Estado devolvido (HTTP 409) quando o token é válido, mas a identidade ainda não está vinculada. */
    public record PendenciaSocial(String status, String provider, String email, boolean emailVerificado,
                                  String nome) {}

    /** Exatamente um dos dois campos é não nulo. */
    public record ResultadoLogin(AuthResponse sessao, PendenciaSocial pendencia) {}

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SocialTokenVerifier verifier;
    private final IdentidadeSocialRepository identidadeRepository;
    private final TutorRepository tutorRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final ProfissionalEsteticaRepository profissionalEsteticaRepository;
    private final AdminRepository adminRepository;
    private final VinculoClinicaService vinculoClinicaService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;

    @Transactional(readOnly = true)
    public ResultadoLogin entrar(String provider, String idToken) {
        SocialIdentity identidade = verifier.verificar(parseProvider(provider), idToken);
        var vinculada = identidadeRepository.findByProvedorAndSubject(identidade.provider(), identidade.subject());
        if (vinculada.isPresent()) {
            return new ResultadoLogin(autenticado(vinculada.get().getTutor()), null);
        }
        return new ResultadoLogin(null, pendencia(identidade));
    }

    @Transactional
    public AuthResponse registrar(SocialRegistrarRequest req) {
        SocialIdentity identidade = verifier.verificar(parseProvider(req.provider()), req.idToken());
        if (identidadeRepository.findByProvedorAndSubject(identidade.provider(), identidade.subject()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta conta já está cadastrada. Faça login.");
        }
        if (identidade.email() == null || !identidade.emailVerified()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "O provedor não informou um e-mail verificado para criar a conta");
        }
        if (emailEmUso(identidade.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "E-mail já cadastrado. Entre com e-mail e senha e vincule sua conta social.");
        }
        Tutor tutor = Tutor.builder()
                .nmTutor(escolherNome(identidade.nome(), req.nome()))
                .dsEmail(identidade.email())
                .dsSenha(passwordEncoder.encode(senhaAleatoria()))
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
        try {
            tutor = vinculoClinicaService.criarTutorComVinculo(tutor, req.sessaoVinculo());
            identidadeRepository.saveAndFlush(novaIdentidade(tutor, identidade));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF ou conta social já cadastrados");
        }
        return autenticado(tutor);
    }

    @Transactional
    public AuthResponse vincular(SocialVincularRequest req) {
        SocialIdentity identidade = verifier.verificar(parseProvider(req.provider()), req.idToken());
        if (identidadeRepository.findByProvedorAndSubject(identidade.provider(), identidade.subject()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta conta social já está vinculada a um usuário");
        }
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.senha()));
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
        }
        Tutor tutor = tutorRepository.findByDsEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente contas de tutor podem usar login social"));
        if (identidadeRepository.existsByTutor_IdTutorAndProvedor(tutor.getIdTutor(), identidade.provider())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta conta já possui uma conta " + identidade.provider() + " vinculada");
        }
        try {
            identidadeRepository.saveAndFlush(novaIdentidade(tutor, identidade));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta conta social já está vinculada");
        }
        return autenticado(tutor);
    }

    private PendenciaSocial pendencia(SocialIdentity identidade) {
        String status = CADASTRO_NECESSARIO;
        // Só revela que existe conta com o e-mail se o provedor garante que ele pertence a quem está logando.
        if (identidade.email() != null && identidade.emailVerified()) {
            if (tutorRepository.existsByDsEmail(identidade.email())) {
                status = VINCULO_NECESSARIO;
            } else if (emailEmUso(identidade.email())) {
                status = EMAIL_EM_USO;
            }
        }
        return new PendenciaSocial(status, identidade.provider().name(), identidade.email(),
                identidade.emailVerified(), identidade.nome());
    }

    private boolean emailEmUso(String email) {
        return tutorRepository.existsByDsEmail(email) || veterinarioRepository.existsByDsEmail(email)
                || profissionalEsteticaRepository.existsByDsEmail(email)
                || adminRepository.findByDsEmail(email).isPresent();
    }

    private AuthResponse autenticado(Tutor tutor) {
        return new AuthResponse(jwtService.gerarToken(tutor.getDsEmail()), tutor.getIdTutor(), tutor.getDsEmail(),
                tutor.getNmTutor(), "TUTOR", vinculoClinicaService.temVinculoAtivo(tutor.getIdTutor()));
    }

    private IdentidadeSocial novaIdentidade(Tutor tutor, SocialIdentity identidade) {
        return IdentidadeSocial.builder().tutor(tutor).provedor(identidade.provider())
                .subject(identidade.subject()).dsEmailProvedor(identidade.email()).build();
    }

    /** O nome do token tem prioridade. A Apple só o envia ao app na 1ª autorização; ele é só dado de exibição. */
    private String escolherNome(String nomeToken, String nomeInformado) {
        String nome = nomeToken != null ? nomeToken : (nomeInformado == null ? null : nomeInformado.trim());
        if (nome == null || nome.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe seu nome para concluir o cadastro");
        }
        return nome.length() > 100 ? nome.substring(0, 100) : nome;
    }

    /** Senha inutilizável: a conta social não entra por senha (pode definir uma via "esqueci minha senha"). */
    private String senhaAleatoria() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private SocialProvider parseProvider(String provider) {
        return SocialProvider.from(provider).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provedor não suportado. Use GOOGLE ou APPLE"));
    }
}