package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Auditoria;
import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.entity.PetAcesso;
import br.com.fiap.VetSync.repository.AuditoriaRepository;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import br.com.fiap.VetSync.repository.PetAcessoRepository;
import br.com.fiap.VetSync.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Histórico de atividade de um pet ("quem fez o quê"), montado a partir da auditoria que já existe.
 * Não expõe e-mails nem IPs: o autor aparece pelo nome (tutores) ou por um rótulo genérico (equipe/sistema).
 */
@Service
@RequiredArgsConstructor
public class PetAtividadeService {

    static final int LIMITE_PADRAO = 30;
    static final int LIMITE_MAXIMO = 100;
    private static final List<Long> SEM_IDS = List.of(-1L);

    private final AuditoriaRepository auditoriaRepository;
    private final PetAcessoRepository petAcessoRepository;
    private final EventoSaudeRepository eventoSaudeRepository;
    private final TutorRepository tutorRepository;

    public record Atividade(Long idAtividade, String tipo, String descricao, String autorNome,
                            LocalDateTime dtAtividade) {}

    @Transactional(readOnly = true)
    public List<Atividade> listar(Long idPet, Integer limite) {
        int tamanho = limite == null ? LIMITE_PADRAO : Math.max(1, Math.min(limite, LIMITE_MAXIMO));

        List<PetAcesso> acessos = petAcessoRepository.findByPet_IdPet(idPet);
        Map<Long, String> nomePorAcesso = new HashMap<>();
        acessos.forEach(a -> nomePorAcesso.put(a.getIdAcesso(),
                a.getTutor() != null && a.getTutor().getNmTutor() != null ? a.getTutor().getNmTutor() : "um cuidador"));
        List<Long> idsAcesso = acessos.isEmpty() ? SEM_IDS : acessos.stream().map(PetAcesso::getIdAcesso).toList();

        List<Long> idsEvento = eventoSaudeRepository.findByPet_IdPet(idPet).stream()
                .map(EventoSaude::getIdEvento).toList();
        if (idsEvento.isEmpty()) idsEvento = SEM_IDS;

        Map<String, String> nomePorEmail = new HashMap<>();
        return auditoriaRepository.findAtividadeDoPet(idPet, idsAcesso, idsEvento, PageRequest.of(0, tamanho))
                .stream()
                .map(a -> converter(a, nomePorAcesso, nomePorEmail))
                .toList();
    }

    private Atividade converter(Auditoria a, Map<Long, String> nomePorAcesso, Map<String, String> nomePorEmail) {
        String acao = a.getDsAcao() == null ? "" : a.getDsAcao();
        String tipo;
        String descricao;
        switch (a.getDsEntidade()) {
            case AuditoriaTipos.ACESSO_PET -> {
                String alvo = nomePorAcesso.getOrDefault(a.getIdEntidade(), "um cuidador");
                switch (acao) {
                    case "ACESSO_CONCEDIDO" -> { tipo = "ACESSO_CONCEDIDO"; descricao = alvo + " passou a ter acesso ao pet"; }
                    case "ACESSO_REVOGADO" -> { tipo = "ACESSO_REVOGADO"; descricao = "Acesso de " + alvo + " removido"; }
                    case "ACESSO_ATUALIZADO" -> { tipo = "OUTRO"; descricao = "Permissão de " + alvo + " alterada"; }
                    default -> { tipo = "OUTRO"; descricao = "Acesso de " + alvo + " alterado"; }
                }
            }
            case AuditoriaTipos.PET_CONVITE -> {
                if ("CONVITE_ACEITO".equals(acao)) {
                    tipo = "CONVITE_ACEITO";
                    descricao = (a.getDsValorNovo() == null ? "Alguém" : a.getDsValorNovo()) + " aceitou o convite";
                } else {
                    tipo = "CONVITE_ENVIADO";
                    descricao = "Convite enviado para " + emailDoConvite(a.getDsValorNovo());
                }
            }
            case AuditoriaTipos.EVENTO -> {
                switch (acao) {
                    case "CRIADO" -> { tipo = "EVENTO_AGENDADO"; descricao = "Evento de saúde agendado"; }
                    case "CONCLUIDO" -> { tipo = "EVENTO_CONCLUIDO"; descricao = "Evento de saúde concluído"; }
                    case "CANCELADO" -> { tipo = "EVENTO_CANCELADO"; descricao = "Evento de saúde cancelado"; }
                    default -> { tipo = "OUTRO"; descricao = "Evento de saúde atualizado"; }
                }
            }
            case AuditoriaTipos.PRONTUARIO -> {
                if (acao.startsWith("PRONTUARIO_ACESSADO") || acao.startsWith("PRONTUARIO_ARQUIVO_ACESSADO")
                        || "PRONTUARIO_EXPORTADO".equals(acao)) {
                    tipo = "PRONTUARIO_VISUALIZADO";
                    descricao = "PRONTUARIO_EXPORTADO".equals(acao) ? "Prontuário exportado" : "Prontuário acessado por link";
                } else if (acao.startsWith("PRONTUARIO_COMPARTILH")) {
                    tipo = "OUTRO";
                    descricao = acao.contains("REVOGADO") ? "Compartilhamento do prontuário encerrado" : "Prontuário compartilhado";
                } else {
                    tipo = "OUTRO";
                    descricao = "Prontuário atualizado";
                }
            }
            default -> { tipo = "OUTRO"; descricao = "Alteração registrada"; }
        }
        return new Atividade(a.getIdAuditoria(), tipo, descricao, autor(a, nomePorEmail), a.getDtOcorrencia());
    }

    /** O destino do convite é gravado como "email (PERMISSAO)"; só o e-mail aparece para o tutor proprietário. */
    private static String emailDoConvite(String valor) {
        if (valor == null || valor.isBlank()) return "um novo cuidador";
        int fim = valor.indexOf(" (");
        return fim > 0 ? valor.substring(0, fim) : valor;
    }

    private String autor(Auditoria a, Map<String, String> cache) {
        String perfil = a.getDsPerfil() == null ? "" : a.getDsPerfil();
        if ("SISTEMA".equals(perfil) || a.getDsAtor() == null || a.getDsAtor().isBlank()) return "VetSync";
        if (!"TUTOR".equals(perfil)) return "Equipe da clínica";
        return cache.computeIfAbsent(a.getDsAtor(), email ->
                tutorRepository.findByDsEmail(email).map(t -> t.getNmTutor()).orElse("Um tutor"));
    }
}