package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.exception.Erros;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import br.com.fiap.VetSync.repository.ProfissionalEsteticaRepository;
import br.com.fiap.VetSync.repository.ServicoClinicaRepository;
import br.com.fiap.VetSync.repository.TipoEventoRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Agendamento e acesso aos eventos de saúde. O restante do ciclo de vida fica em serviços próprios:
 * {@link EventoCancelamentoService}, {@link EventoConclusaoService}, {@link EventoConfirmacaoService},
 * {@link EventoAnexoService} e {@link EventoConsultaService}.
 */
@Service
@RequiredArgsConstructor
public class EventoService {

    private static final String SERVICO_ESTETICA_PALAVRA_CHAVE = "banho";

    private final EventoSaudeRepository eventoSaudeRepository;
    private final ProfissionalEsteticaRepository profissionalEsteticaRepository;
    private final PetService petService;
    private final TipoEventoRepository tipoEventoRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final ServicoEsteticaService servicoEsteticaService;
    private final VinculoClinicaService vinculoClinicaService;
    private final ServicoClinicaRepository servicoClinicaRepository;
    private final AgendaServicoClinicaService agendaServicoClinicaService;
    private final HorarioAgendaValidator horarioValidator;
    private final EventoRegistroService registroService;
    private final PoliticaConfirmacaoAgenda politicaConfirmacao;

    @Transactional
    public EventoSaude agendar(EventoSaude evento, Long idPet, Long idTipoEvento, Long idVeterinario) {
        Optional<EventoSaude> repetido = buscarAgendamentoRepetido(evento, idPet, idTipoEvento, idVeterinario);
        if (repetido.isPresent()) {
            return repetido.get();
        }
        Pet pet = petService.buscarPorId(idPet);
        TipoEvento tipoEvento = buscarTipoEvento(idTipoEvento);
        Veterinario vet = veterinarioRepository.findByIdForBooking(idVeterinario).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veterinário não encontrado: " + idVeterinario));
        exigirSolicitacaoNaoProcessada(evento);

        Long idClinica = vet.getClinica().getIdClinica();
        vinculoClinicaService.exigirClinicaAtivaDoTutor(pet.getTutor().getIdTutor(), idClinica);
        evento.setClinica(vet.getClinica());
        evento.setServicoClinica(escolherOferta(evento, idClinica, idTipoEvento, s -> true, idVeterinario, null));

        horarioValidator.validarVeterinarioLivre(idVeterinario, evento.getDtEvento(), evento.getHrEvento(), null);

        evento.setPet(pet);
        evento.setTipoEvento(tipoEvento);
        evento.setVeterinario(vet);
        return salvarNovoAgendamento(evento);
    }

    public boolean isServicoEstetica(TipoEvento tipoEvento) {
        return tipoEvento != null && tipoEvento.getNmTipoEvento() != null
                && tipoEvento.getNmTipoEvento().toLowerCase().contains(SERVICO_ESTETICA_PALAVRA_CHAVE);
    }

    public EventoSaude agendarEstetica(EventoSaude evento, Long idPet, Long idTipoEvento, Long idProfissionalEstetica, List<Long> idsServico) {
        Pet pet = petService.buscarPorId(idPet);
        TipoEvento tipoEvento = buscarTipoEvento(idTipoEvento);
        if (!isServicoEstetica(tipoEvento)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Esse tipo de evento não é um serviço de banho/tosa atendido pela estética");
        }
        ProfissionalEstetica profissional = profissionalEsteticaRepository.findById(idProfissionalEstetica).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional de estética não encontrado: " + idProfissionalEstetica));

        Long idClinica = profissional.getClinica().getIdClinica();
        vinculoClinicaService.exigirClinicaAtivaDoTutor(pet.getTutor().getIdTutor(), idClinica);
        evento.setClinica(profissional.getClinica());
        evento.setServicoClinica(escolherOferta(evento, idClinica, idTipoEvento,
                s -> s.getBaseEstetica() == null || idsServico.contains(s.getBaseEstetica().getIdServico()),
                null, idProfissionalEstetica));

        Set<ServicoEstetica> servicos = servicoEsteticaService.buscarVarios(idsServico);
        if (servicos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione ao menos o serviço base (banho)");
        }
        if (!profissional.getServicos().containsAll(servicos)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Esse profissional não atende um ou mais dos serviços selecionados");
        }

        horarioValidator.validarProfissionalEsteticaLivre(idProfissionalEstetica, evento.getDtEvento(), evento.getHrEvento(), null);

        evento.setPet(pet);
        evento.setTipoEvento(tipoEvento);
        evento.setProfissionalEstetica(profissional);
        evento.setServicos(servicos);
        return salvarNovoAgendamento(evento);
    }

    @Transactional
    public EventoSaude agendarServico(EventoSaude evento, Long idPet, Long idServico,
                                      Long idVeterinario, Long idProfissionalEstetica) {
        ServicoClinica oferta = servicoClinicaRepository.findById(idServico).orElseThrow(
                Erros::servicoNaoEncontrado);
        Pet pet = petService.buscarPorId(idPet);
        Long idClinica = oferta.getClinica().getIdClinica();
        vinculoClinicaService.exigirClinicaAtivaDoTutor(pet.getTutor().getIdTutor(), idClinica);
        agendaServicoClinicaService.validarReserva(idServico, idClinica, idVeterinario, idProfissionalEstetica,
                evento.getDtEvento(), evento.getHrEvento(), null);
        evento.setPet(pet);
        evento.setClinica(oferta.getClinica());
        evento.setTipoEvento(oferta.getTipoEvento());
        evento.setServicoClinica(oferta);
        if (idVeterinario != null) {
            evento.setVeterinario(veterinarioRepository.findById(idVeterinario).orElseThrow());
        } else {
            evento.setProfissionalEstetica(profissionalEsteticaRepository.findById(idProfissionalEstetica).orElseThrow());
            if (oferta.getBaseEstetica() != null) {
                evento.setServicos(Set.of(oferta.getBaseEstetica()));
            }
        }
        return salvarNovoAgendamento(evento);
    }

    public EventoSaude buscarPorId(Long id) {
        return eventoSaudeRepository.findById(id).orElseThrow(() -> Erros.eventoNaoEncontrado(id));
    }

    public void deletar(Long id) {
        eventoSaudeRepository.delete(buscarPorId(id));
    }

    /** Persiste o agendamento já montado como AGENDADO, no estado de confirmação inicial, e registra histórico e auditoria. */
    private EventoSaude salvarNovoAgendamento(EventoSaude evento) {
        evento.setDsStatus(StatusEvento.AGENDADO);
        evento.setDsConfirmacao(politicaConfirmacao.statusInicial());
        EventoSaude salvo = eventoSaudeRepository.save(evento);
        registroService.registrarCriacao(salvo);
        return salvo;
    }

    /** Reenvio da mesma solicitação (mesmo requestId): devolve o agendamento já criado, se for idêntico. */
    private Optional<EventoSaude> buscarAgendamentoRepetido(EventoSaude evento, Long idPet, Long idTipoEvento, Long idVeterinario) {
        if (evento.getRequestId() == null) {
            return Optional.empty();
        }
        Optional<EventoSaude> anterior = eventoSaudeRepository.findByRequestId(evento.getRequestId());
        if (anterior.isEmpty()) {
            return Optional.empty();
        }
        EventoSaude salvo = anterior.get();
        if (!salvo.getPet().getIdPet().equals(idPet)
                || !salvo.getTipoEvento().getIdTipoEvento().equals(idTipoEvento)
                || !salvo.getVeterinario().getIdVeterinario().equals(idVeterinario)
                || !salvo.getDtEvento().equals(evento.getDtEvento())
                || !salvo.getHrEvento().equals(evento.getHrEvento())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chave de agendamento já utilizada");
        }
        if (salvo.getDsStatus() != StatusEvento.AGENDADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse agendamento já mudou de estado; atualize a agenda");
        }
        return Optional.of(salvo);
    }

    private void exigirSolicitacaoNaoProcessada(EventoSaude evento) {
        if (evento.getRequestId() != null && eventoSaudeRepository.findByRequestId(evento.getRequestId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solicitação de agendamento já processada; atualize a agenda");
        }
    }

    private TipoEvento buscarTipoEvento(Long idTipoEvento) {
        return tipoEventoRepository.findById(idTipoEvento).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de evento não encontrado: " + idTipoEvento));
    }

    /** Primeira oferta ativa da clínica para o tipo de evento que aceita a reserva no dia e hora pedidos. */
    private ServicoClinica escolherOferta(EventoSaude evento, Long idClinica, Long idTipoEvento,
                                          Predicate<ServicoClinica> filtro,
                                          Long idVeterinario, Long idProfissionalEstetica) {
        return servicoClinicaRepository.findByClinica_IdClinicaAndTipoEvento_IdTipoEventoAndAtivoTrue(idClinica, idTipoEvento).stream()
                .filter(filtro)
                .filter(oferta -> aceitaReserva(oferta, evento, idClinica, idVeterinario, idProfissionalEstetica))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Serviço não oferecido pela clínica ou horário indisponível"));
    }

    private boolean aceitaReserva(ServicoClinica oferta, EventoSaude evento, Long idClinica,
                                  Long idVeterinario, Long idProfissionalEstetica) {
        try {
            agendaServicoClinicaService.validarReserva(oferta.getIdServicoClinica(), idClinica,
                    idVeterinario, idProfissionalEstetica, evento.getDtEvento(), evento.getHrEvento(), null);
            return true;
        } catch (ResponseStatusException e) {
            return false;
        }
    }
}
