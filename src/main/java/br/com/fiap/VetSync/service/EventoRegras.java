package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.entity.StatusEvento;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Regras de estado compartilhadas pelos casos de uso do ciclo de vida do evento. */
final class EventoRegras {

    private EventoRegras() {
    }

    static void exigirStatus(EventoSaude evento, StatusEvento esperado, String acao) {
        if (evento.getDsStatus() != esperado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Só é possível " + acao + " um evento que está " + esperado
                            + " (status atual: " + evento.getDsStatus() + ")");
        }
    }
}
