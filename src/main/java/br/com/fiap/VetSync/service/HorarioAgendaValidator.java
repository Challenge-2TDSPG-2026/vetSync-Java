package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.entity.StatusEvento;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/** Garante que o horário pedido existe, não está bloqueado e não conflita com outro atendimento agendado. */
@Component
@RequiredArgsConstructor
public class HorarioAgendaValidator {

    private final EventoSaudeRepository eventoSaudeRepository;
    private final AgendaService agendaService;
    private final AgendaEsteticaService agendaEsteticaService;

    public void validarVeterinarioLivre(Long idVeterinario, LocalDate dtEvento, String hrEvento, Long idEventoIgnorar) {
        exigirHora(hrEvento);
        if (agendaService.estaBloqueado(idVeterinario, dtEvento)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A agenda do veterinário está bloqueada nessa data");
        }
        exigirSemConflito(eventoSaudeRepository.findByVeterinario_IdVeterinarioAndDtEvento(idVeterinario, dtEvento),
                hrEvento, idEventoIgnorar, "Esse veterinário já tem um atendimento agendado nesse horário");
    }

    public void validarProfissionalEsteticaLivre(Long idProfissionalEstetica, LocalDate dtEvento, String hrEvento,
                                                 Long idEventoIgnorar) {
        exigirHora(hrEvento);
        if (agendaEsteticaService.estaBloqueado(idProfissionalEstetica, dtEvento)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A agenda do profissional de estética está bloqueada nessa data");
        }
        exigirSemConflito(
                eventoSaudeRepository.findByProfissionalEstetica_IdProfissionalEsteticaAndDtEvento(idProfissionalEstetica, dtEvento),
                hrEvento, idEventoIgnorar, "Esse profissional já tem um atendimento agendado nesse horário");
    }

    private void exigirHora(String hrEvento) {
        if (hrEvento == null || hrEvento.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hrEvento é obrigatório");
        }
    }

    private void exigirSemConflito(List<EventoSaude> eventosDoDia, String hrEvento, Long idEventoIgnorar, String mensagem) {
        boolean ocupado = eventosDoDia.stream()
                .filter(e -> e.getDsStatus() == StatusEvento.AGENDADO)
                .filter(e -> idEventoIgnorar == null || !e.getIdEvento().equals(idEventoIgnorar))
                .anyMatch(e -> hrEvento.equals(e.getHrEvento()));
        if (ocupado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
        }
    }
}
