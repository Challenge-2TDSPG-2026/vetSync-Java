package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.entity.StatusEvento;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/** Cancelamento e reagendamento de eventos, incluindo o aviso à lista de espera quando um horário é liberado. */
@Service
@RequiredArgsConstructor
public class EventoCancelamentoService {

    public record ResultadoCancelamento(EventoSaude eventoCancelado, EventoSaude novoEvento) {}

    private final EventoService eventoService;
    private final EventoSaudeRepository eventoSaudeRepository;
    private final AgendaServicoClinicaService agendaServicoClinicaService;
    private final HorarioAgendaValidator horarioValidator;
    private final EventoRegistroService registroService;
    private final PoliticaConfirmacaoAgenda politicaConfirmacao;
    private final PlanoEventoSincronizador planoSincronizador;

    @Autowired(required = false)
    private ListaEsperaService listaEsperaService;

    public EventoSaude reagendar(Long id, LocalDate data, String hora) {
        EventoSaude evento = eventoService.buscarPorId(id);
        EventoRegras.exigirStatus(evento, StatusEvento.AGENDADO, "reagendar");
        if (data == null || hora == null || hora.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data e hora são obrigatórias");
        }
        if (evento.getServicoClinica() != null) {
            validarReservaDoServico(evento, data, hora);
        }
        validarHorarioLivreDoResponsavel(evento, data, hora);
        LocalDate dataAnterior = evento.getDtEvento();
        String horaAnterior = evento.getHrEvento();
        registroService.registrarReagendamento(evento, data, hora);
        evento.setDtEvento(data);
        evento.setHrEvento(hora);
        // Novo horário: se a clínica exige confirmação, ela precisa confirmar de novo.
        evento.setDsConfirmacao(politicaConfirmacao.statusInicial());
        evento.setDtConfirmacao(null);
        EventoSaude salvo = eventoSaudeRepository.save(evento);
        liberarVaga(salvo, dataAnterior, horaAnterior);
        return salvo;
    }

    @Transactional
    public ResultadoCancelamento cancelar(Long id, String motivo, LocalDate reagendarPara, String horaReagendarPara) {
        if (motivo == null || motivo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motivo do cancelamento é obrigatório");
        }
        EventoSaude evento = eventoService.buscarPorId(id);
        if (evento.getDsStatus() == StatusEvento.CONCLUIDO || evento.getDsStatus() == StatusEvento.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível cancelar um evento que já está " + evento.getDsStatus());
        }

        if (reagendarPara != null && (horaReagendarPara == null || horaReagendarPara.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "horaReagendarPara é obrigatória quando reagendarPara é informado");
        }
        if (reagendarPara != null) {
            if (evento.getServicoClinica() != null) {
                validarReservaDoServico(evento, reagendarPara, horaReagendarPara);
            } else {
                validarHorarioLivreDoResponsavel(evento, reagendarPara, horaReagendarPara);
            }
        }

        evento.setDsStatus(StatusEvento.CANCELADO);
        evento.setDsMotivoCancelamento(motivo);
        EventoSaude cancelado = eventoSaudeRepository.save(evento);
        registroService.registrarTransicao(cancelado, "CANCELAMENTO", StatusEvento.AGENDADO,
                cancelado.getDsMotivoCancelamento(), null);
        registroService.auditar(cancelado, "CANCELADO", StatusEvento.AGENDADO.name(), cancelado.getDsMotivoCancelamento(), null);
        planoSincronizador.aoCancelarEvento(cancelado);

        EventoSaude novoEvento = reagendarPara == null ? null : criarReagendamento(cancelado, reagendarPara, horaReagendarPara);
        liberarVaga(cancelado, cancelado.getDtEvento(), cancelado.getHrEvento());
        return new ResultadoCancelamento(cancelado, novoEvento);
    }

    private EventoSaude criarReagendamento(EventoSaude cancelado, LocalDate data, String hora) {
        EventoSaude novo = EventoSaude.builder()
                .pet(cancelado.getPet())
                .tipoEvento(cancelado.getTipoEvento())
                .veterinario(cancelado.getVeterinario())
                .profissionalEstetica(cancelado.getProfissionalEstetica())
                .servicoClinica(cancelado.getServicoClinica())
                .servicos(cancelado.getServicos())
                .clinica(cancelado.getClinica())
                .dtEvento(data)
                .hrEvento(hora)
                .dsObservacao("Reagendado do evento #" + cancelado.getIdEvento())
                .dsStatus(StatusEvento.AGENDADO)
                .dsConfirmacao(politicaConfirmacao.statusInicial())
                .build();
        return eventoSaudeRepository.save(novo);
    }

    private void validarReservaDoServico(EventoSaude evento, LocalDate data, String hora) {
        agendaServicoClinicaService.validarReserva(evento.getServicoClinica().getIdServicoClinica(),
                evento.getClinica().getIdClinica(),
                evento.getVeterinario() == null ? null : evento.getVeterinario().getIdVeterinario(),
                evento.getProfissionalEstetica() == null ? null : evento.getProfissionalEstetica().getIdProfissionalEstetica(),
                data, hora, evento.getIdEvento());
    }

    private void validarHorarioLivreDoResponsavel(EventoSaude evento, LocalDate data, String hora) {
        if (evento.getVeterinario() != null) {
            horarioValidator.validarVeterinarioLivre(evento.getVeterinario().getIdVeterinario(), data, hora, evento.getIdEvento());
        } else if (evento.getProfissionalEstetica() != null) {
            horarioValidator.validarProfissionalEsteticaLivre(
                    evento.getProfissionalEstetica().getIdProfissionalEstetica(), data, hora, evento.getIdEvento());
        }
    }

    /** O horário (data/hora) acabou de ficar livre: avisa quem espera uma vaga desse serviço. */
    private void liberarVaga(EventoSaude evento, LocalDate data, String hora) {
        if (listaEsperaService == null || evento.getServicoClinica() == null || data == null || hora == null) return;
        try {
            listaEsperaService.notificarVagaLiberada(new ListaEsperaService.VagaLiberada(
                    evento.getServicoClinica().getIdServicoClinica(),
                    evento.getVeterinario() != null ? evento.getVeterinario().getIdVeterinario() : null,
                    evento.getProfissionalEstetica() != null ? evento.getProfissionalEstetica().getIdProfissionalEstetica() : null,
                    evento.getPet() != null ? evento.getPet().getIdPet() : null,
                    data,
                    hora));
        } catch (Exception ignorada) {
            // A fila de espera é complementar: não pode atrapalhar cancelar/reagendar.
        }
    }
}
