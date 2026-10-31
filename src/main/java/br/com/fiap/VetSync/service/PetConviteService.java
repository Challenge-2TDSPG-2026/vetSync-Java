package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.security.TokenUtils;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

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
    private final ResponsavelService responsavelService;
    private final ConviteEmailService conviteEmailService;
    private final PasswordEncoder passwordEncoder;
    @Autowired(required = false)
    private AuditoriaService auditoriaService;

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
            String email, String nomePet, RelacaoPet relacao, PermissaoPet permissao, LocalDateTime expiraEm,
            boolean contaExistente
    ) {}

    @Transactional(readOnly = true)
    public List<PetConvite> listar(Long idPet) {
        return petConviteRepository.findByPet_IdPetOrderByDtCriacaoDesc(idPet);
    }

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

        return criarConvite(pet, idTutorOrigem, email, relacao, permissao, false);
    }

    @Transactional
    public PetConvite criarResponsavel(Long idTutorOrigem, String email, PermissaoPet permissao) {
        if (permissao != PermissaoPet.LEITURA && permissao != PermissaoPet.EDICAO) {
            throw new ConviteException(HttpStatus.BAD_REQUEST,
                    "Permissão deve ser LEITURA ou EDICAO");
        }
        List<Pet> pets = petRepository.findByTutor_IdTutor(idTutorOrigem);
        if (pets.isEmpty()) {
            throw new ConviteException(HttpStatus.CONFLICT,
                    "Cadastre um pet antes de convidar um responsável");
        }
        return criarConvite(
                pets.get(0), idTutorOrigem, email, RelacaoPet.CUIDADOR, permissao, true);
    }

    private PetConvite criarConvite(Pet pet, Long idTutorOrigem, String email,
                                    RelacaoPet relacao, PermissaoPet permissao,
                                    boolean todosPets) {
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

        // E-mail com conta é permitido: o convite vira um "aceite logado" (ver aceitarComoTutorLogado).
        // Continuam proibidos: convidar a si mesmo e convidar quem já tem acesso ativo.
        Tutor destinoExistente = tutorRepository.findByDsEmail(emailNormalizado).orElse(null);
        validarDestinatario(pet, idTutorOrigem, emailNormalizado, destinoExistente, todosPets);

        List<PetConvite> pendentesAnteriores = todosPets
                ? petConviteRepository
                .findByTutorOrigem_IdTutorAndDsEmailDestinoIgnoreCaseAndDsStatusAndTodosPetsTrue(
                        idTutorOrigem, emailNormalizado, StatusConvitePet.PENDENTE)
                : petConviteRepository.findByPet_IdPetAndDsEmailDestinoIgnoreCaseAndDsStatus(
                pet.getIdPet(), emailNormalizado, StatusConvitePet.PENDENTE);
        pendentesAnteriores.forEach(c -> c.setDsStatus(StatusConvitePet.CANCELADO));
        if (!pendentesAnteriores.isEmpty()) {
            petConviteRepository.saveAll(pendentesAnteriores);
        }

        String tokenPuro = TokenUtils.gerarTokenUrlSeguro(TOKEN_BYTES);
        LocalDateTime agora = LocalDateTime.now();

        PetConvite convite = PetConvite.builder()
                .pet(pet)
                .tutorOrigem(pet.getTutor())
                .dsEmailDestino(emailNormalizado)
                .dsRelacao(relacao)
                .dsPermissao(permissao)
                .dsTokenHash(TokenUtils.sha256Hex(tokenPuro))
                .dsStatus(StatusConvitePet.PENDENTE)
                .dtCriacao(agora)
                .dtExpiracao(agora.plusHours(expiracaoHoras))
                .todosPets(todosPets)
                .build();

        convite = petConviteRepository.save(convite);

        // Convite para todos os pets é gravado no primeiro pet por limitação do modelo: não entra no feed de um pet só.
        if (!todosPets) {
            auditarConvite(pet, "CONVITE_ENVIADO", null, emailNormalizado + " (" + permissao.name() + ")");
        }
        conviteEmailService.enviar(convite, tokenPuro, expiracaoHoras);

        return convite;
    }

    private void validarDestinatario(Pet pet, Long idTutorOrigem, String email, Tutor destino, boolean todosPets) {
        if (destino == null) {
            return;
        }
        if (idTutorOrigem.equals(destino.getIdTutor())) {
            throw new ConviteException(HttpStatus.CONFLICT, "Você não pode convidar a si mesmo");
        }
        boolean jaTemAcesso = todosPets
                ? responsavelService.listarAtivos(idTutorOrigem).stream()
                .anyMatch(v -> v.getTutorResponsavel() != null
                        && destino.getIdTutor().equals(v.getTutorResponsavel().getIdTutor()))
                : petAcessoService.listarAtivosDoPet(pet.getIdPet()).stream()
                .anyMatch(a -> a.getTutor() != null && destino.getIdTutor().equals(a.getTutor().getIdTutor()));
        if (jaTemAcesso) {
            throw new ConviteException(HttpStatus.CONFLICT, "Essa pessoa já tem acesso a este pet");
        }
    }

    // ---------------------------------------------------------------
    // Site consulta o convite pelo token (endpoint público)
    // ---------------------------------------------------------------

    @Transactional
    public ConvitePublico consultarPorToken(String tokenPuro) {
        PetConvite convite = buscarValidoPorToken(tokenPuro);
        return new ConvitePublico(
                convite.getDsEmailDestino(),
                convite.isTodosPets() ? "Todos os pets" : convite.getPet().getNmPet(),
                convite.getDsRelacao(),
                convite.getDsPermissao(),
                convite.getDtExpiracao(),
                tutorRepository.existsByDsEmail(convite.getDsEmailDestino())
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
        String hash = TokenUtils.sha256Hex(tokenPuro);
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
            throw new ConviteException(HttpStatus.CONFLICT,
                    "Já existe uma conta com esse e-mail. Entre na sua conta para aceitar o convite");
        }

        Tutor tutor = Tutor.builder()
                .nmTutor(nome)
                .dsEmail(email)
                .dsSenha(passwordEncoder.encode(senha))
                .dsCpf(cpf)
                .nrTelefone(telefone)
                .build();
        tutor = tutorRepository.save(tutor);

        finalizarAceite(convite, tutor);

        log.info("Convite {} aceito; escopo de acesso: {}", convite.getIdConvite(),
                convite.isTodosPets() ? "todos os pets do tutor" : "pet " + convite.getPet().getIdPet());

        return tutor;
    }

    /** Concede o acesso, marca o convite como ACEITO e audita. Compartilhado pelos dois fluxos de aceite. */
    private void finalizarAceite(PetConvite convite, Tutor tutor) {
        if (convite.isTodosPets()) {
            responsavelService.conceder(convite.getTutorOrigem(), tutor, convite.getDsPermissao());
        } else {
            petAcessoService.conceder(convite.getPet(), tutor, convite.getDsRelacao(), convite.getDsPermissao());
        }

        convite.setDsStatus(StatusConvitePet.ACEITO);
        convite.setDtAceite(LocalDateTime.now());
        convite.setTutorDestino(tutor);
        petConviteRepository.save(convite);

        if (!convite.isTodosPets()) {
            auditarConvite(convite.getPet(), "CONVITE_ACEITO", null, tutor.getNmTutor());
        }
    }

    // ---------------------------------------------------------------
    // Quem JÁ tem conta aceita/recusa logado (sem cadastro)
    // ---------------------------------------------------------------

    @Transactional
    public List<PetConvite> listarRecebidos(String emailTutorLogado) {
        String email = emailTutorLogado.trim().toLowerCase(Locale.ROOT);
        List<PetConvite> pendentes = petConviteRepository
                .findByDsEmailDestinoIgnoreCaseAndDsStatusOrderByDtCriacaoDesc(email, StatusConvitePet.PENDENTE);
        pendentes.stream().filter(PetConvite::isExpirado).forEach(c -> c.setDsStatus(StatusConvitePet.EXPIRADO));
        petConviteRepository.saveAll(pendentes);
        return pendentes.stream().filter(c -> c.getDsStatus() == StatusConvitePet.PENDENTE).toList();
    }

    @Transactional
    public Tutor aceitarComoTutorLogado(String tokenPuro, String emailTutorLogado) {
        return aceitarLogado(buscarValidoPorToken(tokenPuro), emailTutorLogado);
    }

    @Transactional
    public Tutor aceitarRecebido(Long idConvite, String emailTutorLogado) {
        return aceitarLogado(buscarRecebidoValido(idConvite, emailTutorLogado), emailTutorLogado);
    }

    @Transactional
    public void recusarRecebido(Long idConvite, String emailTutorLogado) {
        PetConvite convite = buscarRecebidoValido(idConvite, emailTutorLogado);
        convite.setDsStatus(StatusConvitePet.RECUSADO);
        petConviteRepository.save(convite);
    }

    private PetConvite buscarRecebidoValido(Long idConvite, String emailTutorLogado) {
        PetConvite convite = petConviteRepository.findById(idConvite)
                .orElseThrow(() -> new ConviteException(HttpStatus.NOT_FOUND, "Convite não encontrado"));
        // 404 (e não 403) para não revelar a existência de convites de outras pessoas.
        if (!convite.getDsEmailDestino().equalsIgnoreCase(emailTutorLogado.trim())) {
            throw new ConviteException(HttpStatus.NOT_FOUND, "Convite não encontrado");
        }
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

    private Tutor aceitarLogado(PetConvite convite, String emailTutorLogado) {
        // A conta logada precisa ser a dona do e-mail convidado: o token sozinho não basta.
        if (!convite.getDsEmailDestino().equalsIgnoreCase(emailTutorLogado.trim())) {
            throw new ConviteException(HttpStatus.FORBIDDEN, "Este convite foi enviado para outro e-mail");
        }
        Tutor tutor = tutorRepository.findByDsEmail(convite.getDsEmailDestino())
                .orElseThrow(() -> new ConviteException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
        finalizarAceite(convite, tutor);
        log.info("Convite {} aceito por conta existente", convite.getIdConvite());
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

    private void auditarConvite(Pet pet, String acao, String anterior, String novo) {
        if (auditoriaService != null && pet != null && pet.getIdPet() != null) {
            auditoriaService.registrarAcao(AuditoriaTipos.PET_CONVITE, pet.getIdPet(), acao, null, anterior, novo,
                    pet.getTutor() != null ? pet.getTutor().getDsEmail() : null, "TUTOR");
        }
    }

    // ---------------------------------------------------------------
    // Helpers de token
    // ---------------------------------------------------------------

}
