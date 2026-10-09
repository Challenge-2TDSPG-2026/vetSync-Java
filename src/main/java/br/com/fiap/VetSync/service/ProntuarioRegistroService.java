package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Escrita do prontuário: orientações (por atendimento) e resultados de exames (por pet). */
@Service
@RequiredArgsConstructor
public class ProntuarioRegistroService {

    static final long TAMANHO_MAXIMO_ARQUIVO = 10L * 1024 * 1024; // 10 MB (igual aos anexos de evento)
    static final Set<String> TIPOS_ARQUIVO_PERMITIDOS = Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/webp");

    private final OrientacaoClinicaRepository orientacaoRepository;
    private final ResultadoExameRepository exameRepository;
    private final EventoSaudeRepository eventoSaudeRepository;
    private final PetRepository petRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final AuditoriaService auditoriaService;

    public record DadosExame(Long idEvento, String nome, String laboratorio, LocalDate coletadoEm,
                             LocalDate resultadoEm, String resultado, String interpretacao) {}

    // ------------------------------------------------------------ orientações

    @Transactional
    public OrientacaoClinica criarOrientacao(Long idEvento, String titulo, String texto, String emailVeterinario) {
        EventoSaude evento = buscarEvento(idEvento);
        if (evento.getDsStatus() == StatusEvento.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível registrar orientações em um atendimento cancelado");
        }
        Veterinario vet = veterinarioResponsavel(evento, emailVeterinario);
        String tituloLimpo = obrigatorio(titulo, "título da orientação", 120);
        String textoLimpo = obrigatorio(texto, "texto da orientação", 2000);

        OrientacaoClinica salva = orientacaoRepository.save(OrientacaoClinica.builder()
                .evento(evento).veterinario(vet).dsTitulo(tituloLimpo).dsTexto(textoLimpo)
                .dsAtor(emailVeterinario).dtCriacao(LocalDateTime.now()).build());
        auditar(evento.getPet().getIdPet(), "ORIENTACAO_CRIADA", null, tituloLimpo);
        return salva;
    }

    @Transactional(readOnly = true)
    public List<OrientacaoClinica> listarOrientacoes(Long idEvento) {
        buscarEvento(idEvento);
        return orientacaoRepository.findByEvento_IdEventoOrderByDtCriacaoDesc(idEvento);
    }

    @Transactional
    public void removerOrientacao(Long idEvento, Long idOrientacao, String emailVeterinario) {
        EventoSaude evento = buscarEvento(idEvento);
        veterinarioResponsavel(evento, emailVeterinario);
        OrientacaoClinica orientacao = orientacaoRepository
                .findByIdOrientacaoAndEvento_IdEvento(idOrientacao, idEvento)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orientação não encontrada"));
        orientacaoRepository.delete(orientacao);
        auditar(evento.getPet().getIdPet(), "ORIENTACAO_REMOVIDA", orientacao.getDsTitulo(), null);
    }

    // ----------------------------------------------------------------- exames

    @Transactional
    public ResultadoExame registrarExame(Long idPet, DadosExame dados, String emailVeterinario) {
        Pet pet = petRepository.findById(idPet)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet não encontrado"));
        Veterinario vet = veterinarioRepository.findByDsEmail(emailVeterinario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Veterinário não encontrado"));

        String nome = obrigatorio(dados.nome(), "nome do exame", 120);
        if (dados.resultadoEm() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data do resultado é obrigatória");
        }
        if (dados.resultadoEm().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data do resultado não pode ser futura");
        }
        if (dados.coletadoEm() != null && dados.coletadoEm().isAfter(dados.resultadoEm())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Data da coleta não pode ser depois da data do resultado");
        }
        EventoSaude evento = null;
        if (dados.idEvento() != null) {
            evento = buscarEvento(dados.idEvento());
            if (!idPet.equals(evento.getPet().getIdPet())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O atendimento informado não pertence a esse pet");
            }
        }

        ResultadoExame salvo = exameRepository.save(ResultadoExame.builder()
                .pet(pet).evento(evento).veterinario(vet).nmExame(nome)
                .nmLaboratorio(opcional(dados.laboratorio(), "laboratório", 120))
                .dtColeta(dados.coletadoEm()).dtResultado(dados.resultadoEm())
                .dsResultado(opcional(dados.resultado(), "resultado", 2000))
                .dsInterpretacao(opcional(dados.interpretacao(), "interpretação", 1000))
                .dsAtor(emailVeterinario).dtCriacao(LocalDateTime.now()).build());
        auditar(idPet, "EXAME_REGISTRADO", null, nome);
        return salvo;
    }

    @Transactional
    public ResultadoExame anexarArquivo(Long idPet, Long idExame, MultipartFile arquivo, String emailVeterinario) {
        ResultadoExame exame = buscarExame(idPet, idExame);
        exigirAutor(exame, emailVeterinario);
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo é obrigatório");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_ARQUIVO) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo excede o limite de 10 MB");
        }
        String tipo = arquivo.getContentType() == null ? "" : arquivo.getContentType().toLowerCase();
        if (!TIPOS_ARQUIVO_PERMITIDOS.contains(tipo)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Tipo de arquivo não permitido (use PDF, JPEG, PNG ou WebP)");
        }
        try {
            byte[] bytes = arquivo.getBytes();
            if (tipo.equals("application/pdf") && !comecaComoPdf(bytes)) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "O arquivo enviado não é um PDF válido");
            }
            exame.setNmArquivo(nomeSeguro(arquivo.getOriginalFilename()));
            exame.setDsMimeType(tipo);
            exame.setNrTamanho((long) bytes.length);
            exame.setDsConteudo(bytes);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo", e);
        }
        ResultadoExame salvo = exameRepository.save(exame);
        auditar(idPet, "EXAME_ARQUIVO_ANEXADO", null, exame.getNmExame());
        return salvo;
    }

    @Transactional(readOnly = true)
    public List<ResultadoExame> listarExames(Long idPet) {
        return exameRepository.findByPet_IdPetOrderByDtResultadoDescIdResultadoDesc(idPet);
    }

    @Transactional(readOnly = true)
    public ResultadoExame buscarExame(Long idPet, Long idExame) {
        return exameRepository.findByIdResultadoAndPet_IdPet(idExame, idPet)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exame não encontrado"));
    }

    @Transactional(readOnly = true)
    public ResultadoExame buscarExameComArquivo(Long idPet, Long idExame) {
        ResultadoExame exame = buscarExame(idPet, idExame);
        if (!exame.temArquivo() || exame.getDsConteudo() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esse exame não possui arquivo");
        }
        return exame;
    }

    @Transactional
    public void removerExame(Long idPet, Long idExame, String emailVeterinario) {
        ResultadoExame exame = buscarExame(idPet, idExame);
        exigirAutor(exame, emailVeterinario);
        exameRepository.delete(exame);
        auditar(idPet, "EXAME_REMOVIDO", exame.getNmExame(), null);
    }

    // ---------------------------------------------------------------- helpers

    private EventoSaude buscarEvento(Long idEvento) {
        return eventoSaudeRepository.findById(idEvento)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado"));
    }

    private Veterinario veterinarioResponsavel(EventoSaude evento, String email) {
        Veterinario vet = evento.getVeterinario();
        if (vet == null || email == null || vet.getDsEmail() == null || !vet.getDsEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não é o veterinário responsável por esse atendimento");
        }
        return vet;
    }

    /** Só quem registrou o exame (ou, em dados legados sem autor, ninguém) pode alterá-lo/removê-lo. */
    private void exigirAutor(ResultadoExame exame, String email) {
        boolean autor = exame.getVeterinario() != null && exame.getVeterinario().getDsEmail() != null
                && exame.getVeterinario().getDsEmail().equalsIgnoreCase(email);
        if (!autor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente o veterinário que registrou o exame pode alterá-lo");
        }
    }

    private void auditar(Long idPet, String acao, String anterior, String novo) {
        auditoriaService.registrarAcao(AuditoriaTipos.PRONTUARIO, idPet, acao, null, anterior, novo,
                "SISTEMA", "SISTEMA");
    }

    private static String obrigatorio(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O campo " + campo + " é obrigatório");
        }
        String limpo = valor.trim();
        if (limpo.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O campo " + campo + " excede " + max + " caracteres");
        }
        return limpo;
    }

    private static String opcional(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) return null;
        String limpo = valor.trim();
        if (limpo.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O campo " + campo + " excede " + max + " caracteres");
        }
        return limpo;
    }

    private static boolean comecaComoPdf(byte[] bytes) {
        return bytes.length >= 5 && new String(bytes, 0, 5, StandardCharsets.ISO_8859_1).equals("%PDF-");
    }

    /** Remove caminhos e caracteres de controle/aspas do nome enviado (ele volta em Content-Disposition). */
    static String nomeSeguro(String original) {
        if (original == null || original.isBlank()) return "exame";
        String nome = original.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}\"]", "").trim();
        if (nome.isEmpty()) return "exame";
        return nome.length() > 255 ? nome.substring(nome.length() - 255) : nome;
    }
}