package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.entity.StatusConfirmacao;
import br.com.fiap.VetSync.entity.StatusEvento;
import br.com.fiap.VetSync.entity.TipoNotificacao;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** Confirmação ou recusa de um agendamento pela clínica, com aviso ao tutor. */
@Service
@RequiredArgsConstructor
public class EventoConfirmacaoService {

    private final EventoService eventoService;
    private final EventoCancelamentoService cancelamentoService;
    private final EventoSaudeRepository eventoSaudeRepository;
    private final EventoRegistroService registroService;

    @Autowired(required = false)
    private NotificacaoEnvioService notificacaoEnvioService;

    @Transactional
    public EventoSaude confirmar(Long id) {
        EventoSaude evento = eventoService.buscarPorId(id);
        EventoRegras.exigirStatus(evento, StatusEvento.AGENDADO, "confirmar");
        exigirConfirmacaoPendente(evento);
        evento.setDsConfirmacao(StatusConfirmacao.CONFIRMADO);
        evento.setDtConfirmacao(LocalDateTime.now());
        EventoSaude salvo = eventoSaudeRepository.save(evento);
        registroService.registrarTransicao(salvo, "CONFIRMACAO", StatusEvento.AGENDADO, null, null);
        registroService.auditar(salvo, "CONFIRMADO", StatusConfirmacao.PENDENTE.name(), StatusConfirmacao.CONFIRMADO.name(), null);
        avisarTutor(salvo, TipoNotificacao.EVENTO_CONFIRMADO, "Agendamento confirmado",
                "Seu agendamento de " + nomeDoPet(salvo) + " em " + quando(salvo) + " foi confirmado pela clínica.");
        return salvo;
    }

    @Transactional
    public EventoSaude recusar(Long id, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motivo da recusa é obrigatório");
        }
        EventoSaude evento = eventoService.buscarPorId(id);
        EventoRegras.exigirStatus(evento, StatusEvento.AGENDADO, "recusar");
        exigirConfirmacaoPendente(evento);
        evento.setDsConfirmacao(StatusConfirmacao.RECUSADO);
        evento.setDtConfirmacao(LocalDateTime.now());
        eventoSaudeRepository.save(evento);
        // Recusar = cancelar com o motivo: libera o horário, avisa a lista de espera e registra histórico.
        EventoSaude cancelado = cancelamentoService.cancelar(id, motivo, null, null).eventoCancelado();
        avisarTutor(cancelado, TipoNotificacao.EVENTO_RECUSADO, "Agendamento não confirmado",
                "A clínica não pôde confirmar o agendamento de " + nomeDoPet(cancelado) + " em " + quando(cancelado)
                        + ". Motivo: " + motivo + ". Escolha outro horário.");
        return cancelado;
    }

    private void exigirConfirmacaoPendente(EventoSaude evento) {
        if (evento.getDsConfirmacao() != StatusConfirmacao.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse agendamento não está aguardando confirmação (situação atual: " + evento.getDsConfirmacao() + ")");
        }
    }

    private String nomeDoPet(EventoSaude evento) {
        return evento.getPet() != null ? evento.getPet().getNmPet() : "seu pet";
    }

    private String quando(EventoSaude evento) {
        String data = evento.getDtEvento().format(DateTimeFormatter.ofPattern("dd/MM"));
        return evento.getHrEvento() == null ? data : data + " às " + evento.getHrEvento();
    }

    private void avisarTutor(EventoSaude evento, TipoNotificacao tipo, String titulo, String mensagem) {
        if (notificacaoEnvioService == null || evento.getPet() == null || evento.getPet().getTutor() == null) return;
        try {
            notificacaoEnvioService.notificar(evento.getPet().getTutor(), tipo, titulo, mensagem,
                    "EVENTO", evento.getIdEvento(), Map.of("rota", "/evento/" + evento.getIdEvento()));
        } catch (Exception ignorada) {
            // Aviso é complementar: não pode desfazer a confirmação/recusa.
        }
    }
}
