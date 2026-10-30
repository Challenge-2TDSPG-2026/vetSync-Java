package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.EventoHistorico;
import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.entity.StatusEvento;
import br.com.fiap.VetSync.repository.EventoHistoricoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Registra o histórico e a auditoria das mudanças de um evento de saúde. */
@Service
@RequiredArgsConstructor
public class EventoRegistroService {

    private final EventoHistoricoRepository eventoHistoricoRepository;
    private final ObjectProvider<AuditoriaService> auditoriaService;

    /** Histórico + auditoria do evento recém-criado. */
    public void registrarCriacao(EventoSaude salvo) {
        registrarHistorico(salvo, "CRIACAO", null, salvo.getDsStatus(), null, salvo.getDtEvento(),
                null, salvo.getHrEvento(), null, salvo.getDsObservacao(), null, salvo.getVlCusto());
        auditar(salvo, "CRIADO", null, salvo.getDsObservacao(), null);
    }

    /** Histórico de uma mudança de status que não altera data nem hora (conclusão, cancelamento, confirmação). */
    public void registrarTransicao(EventoSaude evento, String acao, StatusEvento statusAnterior,
                                   String observacaoNova, BigDecimal custoNovo) {
        registrarHistorico(evento, acao, statusAnterior, evento.getDsStatus(),
                evento.getDtEvento(), evento.getDtEvento(), evento.getHrEvento(), evento.getHrEvento(),
                null, observacaoNova, null, custoNovo);
    }

    /** Deve ser chamado antes de aplicar a nova data/hora: o evento ainda guarda os valores anteriores. */
    public void registrarReagendamento(EventoSaude evento, LocalDate dataNova, String horaNova) {
        registrarHistorico(evento, "REAGENDAMENTO", evento.getDsStatus(), evento.getDsStatus(),
                evento.getDtEvento(), dataNova, evento.getHrEvento(), horaNova, null, null, null, null);
    }

    public void auditar(EventoSaude evento, String acao, String anterior, String novo, String ator) {
        AuditoriaService auditoria = auditoriaService.getIfAvailable();
        if (auditoria != null && evento != null && evento.getIdEvento() != null) {
            // Quem agiu (e o perfil/IP) vem da requisição autenticada; "ator" só vale como reserva fora de uma requisição.
            auditoria.registrarAcao(AuditoriaTipos.EVENTO, evento.getIdEvento(), acao,
                    evento.getClinica(), anterior, novo, ator, null);
        }
    }

    private void registrarHistorico(EventoSaude evento, String acao, StatusEvento statusAnterior, StatusEvento statusNovo,
                                    LocalDate dataAnterior, LocalDate dataNova, String horaAnterior, String horaNova,
                                    String obsAnterior, String obsNova, BigDecimal custoAnterior, BigDecimal custoNovo) {
        eventoHistoricoRepository.save(EventoHistorico.builder().evento(evento).dsAcao(acao)
                .dsStatusAnterior(statusAnterior == null ? null : statusAnterior.name())
                .dsStatusNovo(statusNovo == null ? null : statusNovo.name())
                .dtEventoAnterior(dataAnterior).dtEventoNovo(dataNova).hrEventoAnterior(horaAnterior)
                .hrEventoNovo(horaNova).dsObservacaoAnterior(obsAnterior).dsObservacaoNova(obsNova)
                .vlCustoAnterior(custoAnterior).vlCustoNovo(custoNovo).dsAtor("SISTEMA")
                .dtOcorrencia(LocalDateTime.now()).build());
    }
}
