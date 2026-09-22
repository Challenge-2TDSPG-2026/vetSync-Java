package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Pet;
import br.com.fiap.VetSync.entity.PermissaoPet;
import br.com.fiap.VetSync.entity.PetConvite;
import br.com.fiap.VetSync.entity.RelacaoPet;
import br.com.fiap.VetSync.entity.StatusConvitePet;
import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.repository.PetConviteRepository;
import br.com.fiap.VetSync.repository.PetRepository;
import br.com.fiap.VetSync.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * Convite de acesso (cuidador/cônjuge) a um pet já cadastrado.
 * O token puro NUNCA é persistido — só o hash SHA-256 é gravado — e NUNCA é logado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetConviteService {

    private static final int TOKEN_BYTES = 32;

    private final PetConviteRepository petConviteRepository;
    private final PetRepository petRepository;
    private final TutorRepository tutorRepository;
    private final PetAcessoService petAcessoService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.convite.web-invite-url}")
    private String webInviteUrl;

    @Value("${app.convite.expiracao-horas:72}")
    private int expiracaoHoras;

    /** Lançada para os erros do fluxo público de convite; vira {"message": "..."} — ver PetConviteController. */
    public static class ConviteException extends RuntimeException {
        private final HttpStatus status;

        public ConviteException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }

    public record ConvitePublico(
            String email, String nomePet, RelacaoPet relacao, PermissaoPet permissao, LocalDateTime expiraEm
    ) {}

    // ---------------------------------------------------------------
    // Tutor proprietário cria o convite
    // ---------------------------------------------------------------

    @Transactional
    public PetConvite criar(Long idPet, Long idTutorOrigem, String email, RelacaoPet relacao, PermissaoPet permissao) {
        Pet pet = petRepository.findById(idPet)
                .orElseThrow(() -> new ConviteException(HttpStatus.NOT_FOUND, "Pet não encontrado"));

        if (pet.getTutor() == null || !idTutorOrigem.equals(pet.getTutor().getIdTutor())) {
            throw new ConviteException(HttpStatus.FORBIDDEN, "Somente o proprietário do pet pode convidar");
        }

        String emailNormalizado = email.trim().toLowerCase();

        if (tutorRepository.existsByDsEmail(emailNormalizado)) {
            throw new ConviteException(HttpStatus.CONFLICT, "Já existe uma conta cadastrada com esse e-mail");
        }

        // Convites pendentes anteriores para o mesmo pet/e-mail perdem validade: só um fica ativo.
        List<PetConvite> pendentesAnteriores = petConviteRepository
                .findByPet_IdPetAndDsEmailDestinoIgnoreCaseAndDsStatus(idPet, emailNormalizado, StatusConvitePet.PENDENTE);
        pendentesAnteriores.forEach(c -> c.setDsStatus(StatusConvitePet.CANCELADO));
        if (!pendentesAnteriores.isEmpty()) {
            petConviteRepository.saveAll(pendentesAnteriores);
        }

        String tokenPuro = gerarTokenAleatorio();
        LocalDateTime agora = LocalDateTime.now();

        PetConvite convite = PetConvite.builder()
                .pet(pet)
                .tutorOrigem(pet.getTutor())
                .dsEmailDestino(emailNormalizado)
                .dsRelacao(relacao)
                .dsPermissao(permissao)
                .dsTokenHash(calcularHash(tokenPuro))
                .dsStatus(StatusConvitePet.PENDENTE)
                .dtCriacao(agora)
                .dtExpiracao(agora.plusHours(expiracaoHoras))
                .build();

        convite = petConviteRepository.save(convite);

        enviarEmailConvite(convite, tokenPuro);

        return convite;
    }

    private void enviarEmailConvite(PetConvite convite, String tokenPuro) {
        String link = webInviteUrl + "?token=" + tokenPuro;
        String assunto = "Você foi convidado para cuidar de " + convite.getPet().getNmPet() + " no VetSync";
        String corpo = "Olá!\n\n"
                + convite.getTutorOrigem().getNmTutor() + " convidou você para acompanhar "
                + convite.getPet().getNmPet() + " no VetSync, como " + rotuloRelacao(convite.getDsRelacao()) + ".\n\n"
                + "Para aceitar o convite, acesse o link abaixo e finalize seu cadastro:\n" + link + "\n\n"
                + "Este link expira em " + expiracaoHoras + " horas.\n"
                + "Se você não esperava este convite, pode ignorar este e-mail.";
        emailService.enviar(convite.getDsEmailDestino(), assunto, corpo);
    }

    private String rotuloRelacao(RelacaoPet relacao) {
        return switch (relacao) {
            case CUIDADOR -> "cuidador(a)";
            case CONJUGE -> "cônjuge";
            case OUTRO -> "pessoa de confiança";
        };
    }

    // ---------------------------------------------------------------
    // Site consulta o convite pelo token (endpoint público)
    // ---------------------------------------------------------------

    @Transactional
    public ConvitePublico consultarPorToken(String tokenPuro) {
        PetConvite convite = buscarValidoPorToken(tokenPuro);
        return new ConvitePublico(
                convite.getDsEmailDestino(),
                convite.getPet().getNmPet(),
                convite.getDsRelacao(),
                convite.getDsPermissao(),
                convite.getDtExpiracao()
        );
    }

    /**
     * Busca o convite pelo token e garante que ele ainda está PENDENTE e não expirou.
     * Nunca revela ao chamador se o token não existe, já expirou ou já foi usado — sempre
     * 404/410 com mensagem genérica, para não vazar informação sobre convites de terceiros.
     */
    private PetConvite buscarValidoPorToken(String tokenPuro) {
        if (tokenPuro == null || tokenPuro.isBlank()) {
            throw new ConviteException(HttpStatus.NOT_FOUND, "Convite não encontrado");
        }
        String hash = calcularHash(tokenPuro);
        PetConvite convite = petConviteRepository.findByDsTokenHash(hash)
                .orElseThrow(() -> new ConviteException(HttpStatus.NOT_FOUND, "Convite não encontrado"));

        if (convite.getDsStatus() == StatusConvitePet.PENDENTE && convite.isExpirado()) {
            convite.setDsStatus(StatusConvitePet.EXPIRADO);
            petConviteRepository.save(convite);
        }

        if (convite.getDsStatus() == StatusConvitePet.EXPIRADO) {
            throw new ConviteException(HttpStatus.GONE, "Convite expirado");
        }
        if (convite.getDsStatus() != StatusConvitePet.PENDENTE) {
            throw new ConviteException(HttpStatus.GONE, "Convite não está mais disponível");
        }
        return convite;
    }

    // ---------------------------------------------------------------
    // Site registra o cuidador (cadastro + aceite em uma única transação)
    // ---------------------------------------------------------------

    @Transactional
    public Tutor aceitar(String tokenPuro, String nome, String cpf, String telefone, String senha) {
        PetConvite convite = buscarValidoPorToken(tokenPuro);

        // O e-mail vem exclusivamente do convite: nunca do body do cadastro.
        String email = convite.getDsEmailDestino();

        if (tutorRepository.existsByDsEmail(email)) {
            throw new ConviteException(HttpStatus.CONFLICT, "Já existe uma conta cadastrada com esse e-mail");
        }

        Tutor tutor = Tutor.builder()
                .nmTutor(nome)
                .dsEmail(email)
                .dsSenha(passwordEncoder.encode(senha))
                .dsCpf(cpf)
                .nrTelefone(telefone)
                .build();
        tutor = tutorRepository.save(tutor);

        petAcessoService.conceder(convite.getPet(), tutor, convite.getDsRelacao(), convite.getDsPermissao());

        convite.setDsStatus(StatusConvitePet.ACEITO);
        convite.setDtAceite(LocalDateTime.now());
        convite.setTutorDestino(tutor);
        petConviteRepository.save(convite);

        log.info("Convite {} aceito; acesso concedido ao pet {}", convite.getIdConvite(),
                convite.getPet().getIdPet());

        return tutor;
    }

    // ---------------------------------------------------------------
    // Tutor proprietário cancela um convite pendente
    // ---------------------------------------------------------------

    @Transactional
    public void cancelar(Long idPet, Long idConvite) {
        PetConvite convite = petConviteRepository.findById(idConvite)
                .orElseThrow(() -> new ConviteException(HttpStatus.NOT_FOUND, "Convite não encontrado"));

        if (convite.getPet() == null || !idPet.equals(convite.getPet().getIdPet())) {
            throw new ConviteException(HttpStatus.NOT_FOUND, "Convite não encontrado para esse pet");
        }
        if (convite.getDsStatus() != StatusConvitePet.PENDENTE) {
            throw new ConviteException(HttpStatus.CONFLICT, "Somente convites pendentes podem ser cancelados");
        }
        convite.setDsStatus(StatusConvitePet.CANCELADO);
        petConviteRepository.save(convite);
    }

    // ---------------------------------------------------------------
    // Helpers de token
    // ---------------------------------------------------------------

    private String gerarTokenAleatorio() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String calcularHash(String tokenPuro) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(tokenPuro.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível", e);
        }
    }
}