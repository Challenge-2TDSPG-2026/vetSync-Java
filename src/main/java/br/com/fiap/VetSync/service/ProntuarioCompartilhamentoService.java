package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Pet;
import br.com.fiap.VetSync.entity.ProntuarioCompartilhado;
import br.com.fiap.VetSync.entity.ResultadoExame;
import br.com.fiap.VetSync.entity.SecaoProntuario;
import br.com.fiap.VetSync.repository.ProntuarioCompartilhadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Links temporários e somente leitura para o tutor apresentar o prontuário a outro atendimento.
 * Várias regras espelham a carteira compartilhável (hash SHA-256 do token, expiração, revogação imediata),
 * mas aqui o tutor escolhe as seções e o período, e pode ter mais de um link ativo (um por destinatário).
 */
@Service
@RequiredArgsConstructor
public class ProntuarioCompartilhamentoService {

    static final int TOKEN_BYTES = 32;
    static final int MAXIMO_LINKS_ATIVOS_POR_PET = 5;
    static final int VALIDADE_MAXIMA_DIAS = 30;

    private final ProntuarioCompartilhadoRepository repository;
    private final ProntuarioService prontuarioService;
    private final ProntuarioRegistroService registroService;
    private final PetService petService;
    private final AuditoriaService auditoriaService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.prontuario.compartilhamento.expiracao-dias:7}")
    private int expiracaoPadraoDias = 7;

    public record Criado(ProntuarioCompartilhado compartilhamento, String tokenPuro) {}

    public record ArquivoExame(byte[] bytes, String nome, String mimeType) {}

    // ----------------------------------------------------------- lado do tutor

    @Transactional
    public Criado criar(Long idPet, Long idTutorCriador, Set<SecaoProntuario> secoes, String destinatario,
                        Integer validadeDias, LocalDate de, LocalDate ate) {
        Pet pet = petService.buscarPorId(idPet);
        int dias = validadeDias == null ? expiracaoPadraoDias : validadeDias;
        if (dias < 1 || dias > VALIDADE_MAXIMA_DIAS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A validade deve estar entre 1 e " + VALIDADE_MAXIMA_DIAS + " dias");
        }
        if (de != null && ate != null && de.isAfter(ate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'periodoInicio' não pode ser depois de 'periodoFim'");
        }
        String dest = destinatario == null || destinatario.isBlank() ? null : destinatario.trim();
        if (dest != null && dest.length() > 150) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O destinatário excede 150 caracteres");
        }
        long ativos = repository.findByPet_IdPetOrderByCriadaEmDesc(idPet).stream()
                .filter(ProntuarioCompartilhado::isAtivo).count();
        if (ativos >= MAXIMO_LINKS_ATIVOS_POR_PET) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse pet já tem " + MAXIMO_LINKS_ATIVOS_POR_PET
                            + " links ativos. Revogue algum antes de criar outro.");
        }
        Set<SecaoProntuario> escopo = (secoes == null || secoes.isEmpty())
                ? ProntuarioService.todasAsSecoes() : EnumSet.copyOf(secoes);

        String tokenPuro = gerarTokenAleatorio();
        LocalDateTime agora = LocalDateTime.now();
        ProntuarioCompartilhado salvo = repository.save(ProntuarioCompartilhado.builder()
                .pet(pet).tokenHash(calcularHash(tokenPuro))
                .dsSecoes(ProntuarioCompartilhado.serializarSecoes(escopo))
                .dsDestinatario(dest).dtPeriodoInicio(de).dtPeriodoFim(ate)
                .idUsuarioCriador(idTutorCriador).criadaEm(agora).expiraEm(agora.plusDays(dias)).build());
        auditar(idPet, "PRONTUARIO_COMPARTILHADO", null,
                "compartilhamento=" + salvo.getIdCompartilhamento() + "; secoes=" + salvo.getDsSecoes()
                        + (dest != null ? "; destinatario=" + dest : ""));
        return new Criado(salvo, tokenPuro);
    }

    @Transactional(readOnly = true)
    public List<ProntuarioCompartilhado> listar(Long idPet) {
        return repository.findByPet_IdPetOrderByCriadaEmDesc(idPet);
    }

    /** Revogação imediata. Repetir a chamada é inofensivo (não duplica a auditoria). */
    @Transactional
    public void revogar(Long idPet, Long idCompartilhamento) {
        ProntuarioCompartilhado link = repository.findByIdCompartilhamentoAndPet_IdPet(idCompartilhamento, idPet)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Compartilhamento não encontrado para esse pet"));
        if (link.getRevogadaEm() == null) {
            link.setRevogadaEm(LocalDateTime.now());
            repository.save(link);
            auditar(idPet, "PRONTUARIO_COMPARTILHAMENTO_REVOGADO", "compartilhamento=" + idCompartilhamento, null);
        }
    }

    // -------------------------------------------------------- lado do destinatário

    /** Resolve o link (404 inexistente; 410 revogado/expirado), registra o acesso e devolve o prontuário. */
    @Transactional
    public ProntuarioService.Prontuario visualizar(String tokenPuro) {
        ProntuarioCompartilhado link = resolverAtivo(tokenPuro);
        link.setUltimoAcessoEm(LocalDateTime.now());
        link.setNrAcessos((link.getNrAcessos() == null ? 0 : link.getNrAcessos()) + 1);
        repository.save(link);
        auditar(link.getPet().getIdPet(), "PRONTUARIO_ACESSADO_POR_LINK", null,
                "compartilhamento=" + link.getIdCompartilhamento());
        // Externo: só receitas liberadas pela clínica.
        return prontuarioService.montar(link.getPet().getIdPet(), link.secoes(),
                link.getDtPeriodoInicio(), link.getDtPeriodoFim(), true);
    }

    @Transactional
    public ArquivoExame arquivoDoExame(String tokenPuro, Long idExame) {
        ProntuarioCompartilhado link = resolverAtivo(tokenPuro);
        if (!link.secoes().contains(SecaoProntuario.EXAMES)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        ResultadoExame exame;
        try {
            exame = registroService.buscarExameComArquivo(link.getPet().getIdPet(), idExame);
        } catch (ResponseStatusException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        LocalDate data = exame.getDtResultado();
        boolean foraDoPeriodo = (link.getDtPeriodoInicio() != null && data.isBefore(link.getDtPeriodoInicio()))
                || (link.getDtPeriodoFim() != null && data.isAfter(link.getDtPeriodoFim()));
        if (foraDoPeriodo) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        auditar(link.getPet().getIdPet(), "PRONTUARIO_ARQUIVO_ACESSADO_POR_LINK", null,
                "compartilhamento=" + link.getIdCompartilhamento() + "; exame=" + idExame);
        return new ArquivoExame(exame.getDsConteudo(), exame.getNmArquivo(), exame.getDsMimeType());
    }

    private ProntuarioCompartilhado resolverAtivo(String tokenPuro) {
        if (tokenPuro == null || tokenPuro.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        ProntuarioCompartilhado link = repository.findByTokenHash(calcularHash(tokenPuro))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!link.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.GONE);
        }
        return link;
    }

    private void auditar(Long idPet, String acao, String anterior, String novo) {
        auditoriaService.registrarAcao(AuditoriaTipos.PRONTUARIO, idPet, acao, null, anterior, novo,
                "LINK_PUBLICO", "EXTERNO");
    }

    private String gerarTokenAleatorio() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String calcularHash(String tokenPuro) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(tokenPuro.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível", e);
        }
    }
}