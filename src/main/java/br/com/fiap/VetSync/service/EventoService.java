package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.ProfissionalEsteticaRepository;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import br.com.fiap.VetSync.repository.PlanoItemRepository;
import br.com.fiap.VetSync.repository.PlanoTratamentoRepository;
import br.com.fiap.VetSync.repository.TipoEventoRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoSaudeRepository eventoSaudeRepository;
    private final ProfissionalEsteticaRepository profissionalEsteticaRepository;
    private final AgendaEsteticaService agendaEsteticaService;
    private final PetService petService;
    private final TipoEventoRepository tipoEventoRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final PontosService pontosService;
    private final PlanoItemRepository planoItemRepository;
    private final PlanoTratamentoRepository planoTratamentoRepository;
    private final AgendaService agendaService;
    private final ServicoEsteticaService servicoEsteticaService;


    private static final long MESES_LIMITE_ATRASO = 12;

    public record AlertaEvento(
            String nmTipoEvento, LocalDate ultimaData, long mesesDesdeUltimo, boolean atrasado, String mensagem
    ) {}


    public record ResultadoCancelamento(EventoSaude eventoCancelado, EventoSaude novoEvento) {}



    public EventoSaude agendar(EventoSaude evento, Long idPet, Long idTipoEvento, Long idVeterinario) {
        Pet pet = petService.buscarPorId(idPet);
        TipoEvento tipoEvento = tipoEventoRepository.findById(idTipoEvento).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de evento não encontrado: " + idTipoEvento));
        Veterinario vet = veterinarioRepository.findById(idVeterinario).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veterinário não encontrado: " + idVeterinario));

        validarHorarioLivre(idVeterinario, evento.getDtEvento(), evento.getHrEvento(), null);

        evento.setPet(pet);
        evento.setTipoEvento(tipoEvento);
        evento.setVeterinario(vet);
        evento.setDsStatus(StatusEvento.AGENDADO);
        return eventoSaudeRepository.save(evento);
    }
    private static final String SERVICO_ESTETICA_PALAVRA_CHAVE = "banho";

    public boolean isServicoEstetica(TipoEvento tipoEvento) {
        return tipoEvento != null && tipoEvento.getNmTipoEvento() != null
                && tipoEvento.getNmTipoEvento().toLowerCase().contains(SERVICO_ESTETICA_PALAVRA_CHAVE);
    }

    public EventoSaude agendarEstetica(EventoSaude evento, Long idPet, Long idTipoEvento, Long idProfissionalEstetica, List<Long> idsServico) {
        Pet pet = petService.buscarPorId(idPet);
        TipoEvento tipoEvento = tipoEventoRepository.findById(idTipoEvento).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de evento não encontrado: " + idTipoEvento));
        if (!isServicoEstetica(tipoEvento)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Esse tipo de evento não é um serviço de banho/tosa atendido pela estética");
        }
        ProfissionalEstetica profissional = profissionalEsteticaRepository.findById(idProfissionalEstetica).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional de estética não encontrado: " + idProfissionalEstetica));

        Set<ServicoEstetica> servicos = servicoEsteticaService.buscarVarios(idsServico);
        if (servicos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione ao menos o serviço base (banho)");
        }
        boolean profissionalAtendeTodos = profissional.getServicos().containsAll(servicos);
        if (!profissionalAtendeTodos) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Esse profissional não atende um ou mais dos serviços selecionados");
        }

        validarHorarioLivreEstetica(idProfissionalEstetica, evento.getDtEvento(), evento.getHrEvento(), null);

        evento.setPet(pet);
        evento.setTipoEvento(tipoEvento);
        evento.setProfissionalEstetica(profissional);
        evento.setServicos(servicos);
        evento.setDsStatus(StatusEvento.AGENDADO);
        return eventoSaudeRepository.save(evento);
    }

    private void validarHorarioLivreEstetica(Long idProfissionalEstetica, LocalDate dtEvento, String hrEvento, Long idEventoIgnorar) {
        if (hrEvento == null || hrEvento.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hrEvento é obrigatório");
        }
        if (agendaEsteticaService.estaBloqueado(idProfissionalEstetica, dtEvento)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A agenda do profissional de estética está bloqueada nessa data");
        }
        boolean ocupado = eventoSaudeRepository.findByProfissionalEstetica_IdProfissionalEsteticaAndDtEvento(idProfissionalEstetica, dtEvento).stream()
                .filter(e -> e.getDsStatus() == StatusEvento.AGENDADO)
                .filter(e -> idEventoIgnorar == null || !e.getIdEvento().equals(idEventoIgnorar))
                .anyMatch(e -> hrEvento.equals(e.getHrEvento()));
        if (ocupado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse profissional já tem um atendimento agendado nesse horário");
        }
    }

    public List<EventoSaude> listarParaProfissionalEstetica(String email) {
        return eventoSaudeRepository.findByProfissionalEstetica_DsEmailOrderByDtEventoDesc(email);
    }


    private void validarHorarioLivre(Long idVeterinario, LocalDate dtEvento, String hrEvento, Long idEventoIgnorar) {
        if (hrEvento == null || hrEvento.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hrEvento é obrigatório");
        }
        if (agendaService.estaBloqueado(idVeterinario, dtEvento)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A agenda do veterinário está bloqueada nessa data");
        }
        boolean ocupado = eventoSaudeRepository.findByVeterinario_IdVeterinarioAndDtEvento(idVeterinario, dtEvento).stream()
                .filter(e -> e.getDsStatus() == StatusEvento.AGENDADO)
                .filter(e -> idEventoIgnorar == null || !e.getIdEvento().equals(idEventoIgnorar))
                .anyMatch(e -> hrEvento.equals(e.getHrEvento()));
        if (ocupado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse veterinário já tem um atendimento agendado nesse horário");
        }
    }

    public EventoSaude buscarPorId(Long id) {
        return eventoSaudeRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado com id: " + id));
    }

    public List<EventoSaude> listarPorPet(Long idPet) {
        return eventoSaudeRepository.findByPet_IdPet(idPet);
    }

    public List<EventoSaude> listarParaTutor(String email) {
        return eventoSaudeRepository.findByPet_Tutor_DsEmailOrderByDtEventoDesc(email);
    }

    public List<EventoSaude> listarParaVeterinario(String email) {
        return eventoSaudeRepository.findByVeterinario_DsEmailOrderByDtEventoDesc(email);
    }

    public EventoSaude concluir(Long id, String dsObservacao, BigDecimal vlCusto) {
        EventoSaude evento = buscarPorId(id);
        exigirStatus(evento, StatusEvento.AGENDADO, "concluir");
        evento.setDsStatus(StatusEvento.CONCLUIDO);
        if (dsObservacao != null) {
            evento.setDsObservacao(dsObservacao);
        }
        evento.setVlCusto(vlCusto != null ? vlCusto : BigDecimal.ZERO);
        EventoSaude concluido = eventoSaudeRepository.save(evento);

        pontosService.lancarPendente(concluido);
        processarPlanoAoConcluir(concluido);
        return concluido;
    }


    private void processarPlanoAoConcluir(EventoSaude evento) {
        planoItemRepository.findByEvento_IdEvento(evento.getIdEvento()).ifPresent(item -> {
            item.setDsStatus(StatusPlanoItem.CONCLUIDO);
            planoItemRepository.save(item);

            PlanoTratamento plano = item.getPlano();
            if (plano.getDsStatus() != StatusPlanoTratamento.EM_ANDAMENTO) {
                return; // plano já estava QUEBRADO ou CONCLUIDO, não há mais bônus a considerar
            }

            List<PlanoItem> itens = planoItemRepository.findByPlano_IdPlanoOrderByNrOrdemAsc(plano.getIdPlano());
            boolean todosAnterioresConcluidos = itens.stream()
                    .filter(i -> i.getNrOrdem() < item.getNrOrdem())
                    .allMatch(i -> i.getDsStatus() == StatusPlanoItem.CONCLUIDO);

            if (!todosAnterioresConcluidos) {
                plano.setDsStatus(StatusPlanoTratamento.QUEBRADO);
                planoTratamentoRepository.save(plano);
                return;
            }

            boolean ehUltimoItem = itens.stream().mapToInt(PlanoItem::getNrOrdem).max().orElse(0) == item.getNrOrdem();
            if (ehUltimoItem) {
                plano.setDsStatus(StatusPlanoTratamento.CONCLUIDO);
                planoTratamentoRepository.save(plano);
                pontosService.lancarBonusPendente(plano);
            }
        });
    }

    public ResultadoCancelamento cancelar(Long id, String motivo, LocalDate reagendarPara, String horaReagendarPara) {
        if (motivo == null || motivo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motivo do cancelamento é obrigatório");
        }
        EventoSaude evento = buscarPorId(id);
        if (evento.getDsStatus() == StatusEvento.CONCLUIDO || evento.getDsStatus() == StatusEvento.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível cancelar um evento que já está " + evento.getDsStatus());
        }

        if (reagendarPara != null && (horaReagendarPara == null || horaReagendarPara.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "horaReagendarPara é obrigatória quando reagendarPara é informado");
        }
        if (reagendarPara != null) {
            validarHorarioLivre(evento.getVeterinario().getIdVeterinario(), reagendarPara, horaReagendarPara, evento.getIdEvento());
        }

        evento.setDsStatus(StatusEvento.CANCELADO);
        evento.setDsMotivoCancelamento(motivo);
        EventoSaude cancelado = eventoSaudeRepository.save(evento);
        processarPlanoAoCancelar(cancelado);

        EventoSaude novoEvento = null;
        if (reagendarPara != null) {
            EventoSaude novo = EventoSaude.builder()
                    .pet(cancelado.getPet())
                    .tipoEvento(cancelado.getTipoEvento())
                    .veterinario(cancelado.getVeterinario())
                    .dtEvento(reagendarPara)
                    .hrEvento(horaReagendarPara)
                    .dsObservacao("Reagendado do evento #" + cancelado.getIdEvento())
                    .dsStatus(StatusEvento.AGENDADO)
                    .build();
            novoEvento = eventoSaudeRepository.save(novo);

        }
        return new ResultadoCancelamento(cancelado, novoEvento);
    }


    private void processarPlanoAoCancelar(EventoSaude evento) {
        planoItemRepository.findByEvento_IdEvento(evento.getIdEvento()).ifPresent(item -> {
            item.setDsStatus(StatusPlanoItem.QUEBRADO);
            planoItemRepository.save(item);

            PlanoTratamento plano = item.getPlano();
            if (plano.getDsStatus() == StatusPlanoTratamento.EM_ANDAMENTO) {
                plano.setDsStatus(StatusPlanoTratamento.QUEBRADO);
                planoTratamentoRepository.save(plano);
            }
        });
    }

    private void exigirStatus(EventoSaude evento, StatusEvento esperado, String acao) {
        if (evento.getDsStatus() != esperado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Só é possível " + acao + " um evento que está " + esperado
                            + " (status atual: " + evento.getDsStatus() + ")");
        }
    }

    public void deletar(Long id) {
        EventoSaude evento = buscarPorId(id);
        eventoSaudeRepository.delete(evento);
    }


    public BigDecimal calcularGastoTotal(Long idPet) {
        return listarPorPet(idPet).stream()
                .filter(e -> e.getDsStatus() == StatusEvento.CONCLUIDO)
                .map(EventoSaude::getVlCusto)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<AlertaEvento> gerarAlertas(Long idPet) {
        List<EventoSaude> concluidos = listarPorPet(idPet).stream()
                .filter(e -> e.getDsStatus() == StatusEvento.CONCLUIDO)
                .toList();

        Map<TipoEvento, EventoSaude> maisRecentePorTipo = concluidos.stream()
                .collect(Collectors.toMap(
                        EventoSaude::getTipoEvento,
                        e -> e,
                        (e1, e2) -> e1.getDtEvento().isAfter(e2.getDtEvento()) ? e1 : e2
                ));

        List<AlertaEvento> alertas = new ArrayList<>();
        for (Map.Entry<TipoEvento, EventoSaude> entry : maisRecentePorTipo.entrySet()) {
            TipoEvento tipo = entry.getKey();
            EventoSaude ultimo = entry.getValue();
            long meses = ChronoUnit.MONTHS.between(ultimo.getDtEvento(), LocalDate.now());
            boolean atrasado = meses >= MESES_LIMITE_ATRASO;
            String mensagem = atrasado
                    ? "Último \"" + tipo.getNmTipoEvento() + "\" foi há " + meses + " meses — pode estar atrasado"
                    : "Último \"" + tipo.getNmTipoEvento() + "\" foi há " + meses + " meses — em dia";
            alertas.add(new AlertaEvento(tipo.getNmTipoEvento(), ultimo.getDtEvento(), meses, atrasado, mensagem));
        }
        alertas.sort(Comparator.comparing(AlertaEvento::atrasado).reversed());
        return alertas;
    }
}