package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.entity.StatusConfirmacao;
import br.com.fiap.VetSync.entity.StatusEvento;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/** Conclusão de um atendimento: observações, custo, histórico, pontos de fidelidade e plano de tratamento. */
@Service
@RequiredArgsConstructor
public class EventoConclusaoService {

    private static final int LIMITE_OBSERVACAO_BYTES = 500;
    private static final int LIMITE_OBSERVACAO_CLINICA_BYTES = 1000;

    private final EventoService eventoService;
    private final EventoSaudeRepository eventoSaudeRepository;
    private final EventoRegistroService registroService;
    private final PontosService pontosService;
    private final PlanoEventoSincronizador planoSincronizador;

    public EventoSaude concluir(Long id, String dsObservacao, BigDecimal vlCusto) {
        EventoSaude evento = eventoService.buscarPorId(id);
        EventoRegras.exigirStatus(evento, StatusEvento.AGENDADO, "concluir");
        exigirConfirmacaoConcluivel(evento);
        evento.setDsStatus(StatusEvento.CONCLUIDO);
        if (dsObservacao != null) {
            if (tamanhoEmBytes(dsObservacao) > LIMITE_OBSERVACAO_CLINICA_BYTES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A observação do atendimento excede o limite de " + LIMITE_OBSERVACAO_CLINICA_BYTES + " bytes");
            }
            // ds_observacao (e o histórico) comportam 500 bytes; a observação clínica comporta 1000.
            evento.setDsObservacao(resumirEmBytes(dsObservacao, LIMITE_OBSERVACAO_BYTES));
            evento.setDsObservacaoClinica(dsObservacao);
        }
        evento.setVlCusto(vlCusto != null ? vlCusto : BigDecimal.ZERO);
        EventoSaude concluido = eventoSaudeRepository.save(evento);
        registroService.registrarTransicao(concluido, "CONCLUSAO", StatusEvento.AGENDADO,
                concluido.getDsObservacao(), concluido.getVlCusto());
        registroService.auditar(concluido, "CONCLUIDO", StatusEvento.AGENDADO.name(), concluido.getDsStatus().name(), null);

        pontosService.lancarPendente(concluido);
        planoSincronizador.aoConcluirEvento(concluido);
        return concluido;
    }

    private void exigirConfirmacaoConcluivel(EventoSaude evento) {
        if (evento.getDsConfirmacao() == StatusConfirmacao.PENDENTE
                || evento.getDsConfirmacao() == StatusConfirmacao.RECUSADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Só é possível concluir um atendimento confirmado pela clínica");
        }
    }

    private static int tamanhoEmBytes(String texto) {
        return texto.getBytes(StandardCharsets.UTF_8).length;
    }

    /** Corta o texto em no máximo {@code limite} bytes (UTF-8) sem quebrar um caractere no meio. */
    private static String resumirEmBytes(String texto, int limite) {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= limite) return texto;
        int fim = limite;
        while (fim > 0 && (bytes[fim] & 0xC0) == 0x80) fim--;
        return new String(bytes, 0, fim, StandardCharsets.UTF_8);
    }
}
