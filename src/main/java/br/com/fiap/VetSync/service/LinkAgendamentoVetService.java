package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.entity.LinkAgendamentoVet;
import br.com.fiap.VetSync.entity.Pet;
import br.com.fiap.VetSync.entity.StatusLinkAgendamento;
import br.com.fiap.VetSync.entity.TipoEvento;
import br.com.fiap.VetSync.repository.LinkAgendamentoVetRepository;
import br.com.fiap.VetSync.repository.TipoEventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class LinkAgendamentoVetService {

    private static final String NOME_TIPO_EVENTO_RETORNO = "Retorno veterinário";

    private final LinkAgendamentoVetRepository linkAgendamentoVetRepository;
    private final TipoEventoRepository tipoEventoRepository;
    private final EventoService eventoService;

    public LinkAgendamentoVet buscarLinkValido(String token) {
        LinkAgendamentoVet link = linkAgendamentoVetRepository.findByDsToken(token).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link não encontrado"));

        if (link.getDsStatus() == StatusLinkAgendamento.UTILIZADO) {
            throw new ResponseStatusException(HttpStatus.GONE, "Esse link já foi utilizado");
        }
        if (link.getDsStatus() == StatusLinkAgendamento.EXPIRADO || link.getDtExpiracao().isBefore(LocalDate.now())) {
            link.setDsStatus(StatusLinkAgendamento.EXPIRADO);
            linkAgendamentoVetRepository.save(link);
            throw new ResponseStatusException(HttpStatus.GONE, "Esse link expirou");
        }
        return link;
    }

    public EventoSaude confirmarAgendamento(String token, Long idVeterinario, LocalDate dtEvento, String hrEvento) {
        LinkAgendamentoVet link = buscarLinkValido(token);
        Pet pet = link.getRelatorioEstetica().getEvento().getPet();

        TipoEvento tipoRetorno = tipoEventoRepository.findFirstByNmTipoEventoIgnoreCase(NOME_TIPO_EVENTO_RETORNO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Tipo de evento \"" + NOME_TIPO_EVENTO_RETORNO + "\" não está cadastrado"));

        EventoSaude novoEvento = EventoSaude.builder()
                .dtEvento(dtEvento)
                .hrEvento(hrEvento)
                .dsObservacao("Retorno solicitado pela estética: " + link.getRelatorioEstetica().getDsProblema())
                .build();

        EventoSaude eventoCriado = eventoService.agendar(
                novoEvento, pet.getIdPet(), tipoRetorno.getIdTipoEvento(), idVeterinario
        );

        link.setDsStatus(StatusLinkAgendamento.UTILIZADO);
        link.setEventoCriado(eventoCriado);
        linkAgendamentoVetRepository.save(link);

        return eventoCriado;
    }
}