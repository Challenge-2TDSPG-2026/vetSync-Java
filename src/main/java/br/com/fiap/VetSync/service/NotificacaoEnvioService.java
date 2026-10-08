package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.DispositivoPush;
import br.com.fiap.VetSync.entity.Notificacao;
import br.com.fiap.VetSync.entity.TipoNotificacao;
import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.repository.DispositivoPushRepository;
import br.com.fiap.VetSync.repository.NotificacaoRepository;
import br.com.fiap.VetSync.repository.PreferenciaNotificacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cria a notificação no app (sempre) e dispara o push (se o tutor permitir e tiver aparelho ativo).
 * O push sai em outra thread e, quando há transação aberta, só depois do commit — assim a chamada HTTP
 * não segura conexão do pool e o tutor nunca é avisado de algo que acabou sofrendo rollback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoEnvioService {

    private final NotificacaoRepository notificacaoRepository;
    private final DispositivoPushRepository dispositivoRepository;
    private final PreferenciaNotificacaoRepository preferenciaRepository;
    private final ExpoPushService expoPushService;

    public Notificacao notificar(Tutor tutor, TipoNotificacao tipo, String titulo, String mensagem,
                                 String referenciaTipo, Long referenciaId, Map<String, Object> dadosExtras) {
        Notificacao notificacao = notificacaoRepository.save(Notificacao.builder()
                .tutor(tutor)
                .tipo(tipo)
                .titulo(limitar(titulo, 150))
                .mensagem(limitar(mensagem, 500))
                .referenciaTipo(referenciaTipo)
                .referenciaId(referenciaId)
                .build());

        Map<String, Object> dados = new HashMap<>();
        if (dadosExtras != null) dados.putAll(dadosExtras);
        dados.put("notificacaoId", notificacao.getIdNotificacao());
        dados.put("tipo", tipo.name());
        if (referenciaTipo != null) dados.put("referenciaTipo", referenciaTipo);
        if (referenciaId != null) dados.put("referenciaId", referenciaId);

        Runnable envio = () -> enviarPush(tutor, notificacao.getIdNotificacao(), notificacao.getTitulo(),
                notificacao.getMensagem(), dados);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executarEmSegundoPlano(envio);
                }
            });
        } else {
            executarEmSegundoPlano(envio);
        }
        return notificacao;
    }

    private void executarEmSegundoPlano(Runnable tarefa) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                tarefa.run();
            } catch (Exception e) {
                log.warn("Falha ao enviar push: {}", e.getMessage());
            }
        });
    }

    private void enviarPush(Tutor tutor, Long idNotificacao, String titulo, String mensagem, Map<String, Object> dados) {
        boolean pushAtivo = preferenciaRepository.findByTutor(tutor)
                .map(p -> p.isPushAtivo())
                .orElse(true);
        if (!pushAtivo) return;

        List<DispositivoPush> aparelhos = dispositivoRepository.findByTutorAndAtivoTrue(tutor);
        if (aparelhos.isEmpty()) return;

        List<ExpoPushService.MensagemPush> mensagens = aparelhos.stream()
                .map(a -> new ExpoPushService.MensagemPush(a.getToken(), titulo, mensagem, dados))
                .toList();
        ExpoPushService.ResultadoEnvio resultado = expoPushService.enviar(mensagens);

        if (!resultado.tokensInvalidos().isEmpty()) {
            aparelhos.stream()
                    .filter(a -> resultado.tokensInvalidos().contains(a.getToken()))
                    .forEach(a -> a.setAtivo(false));
            dispositivoRepository.saveAll(aparelhos);
        }
        if (resultado.enviados() > 0) {
            notificacaoRepository.findById(idNotificacao).ifPresent(n -> {
                n.setEnviadaEm(LocalDateTime.now());
                notificacaoRepository.save(n);
            });
        }
    }

    private static String limitar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }
}