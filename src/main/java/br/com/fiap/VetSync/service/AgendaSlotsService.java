package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Disponibilidade;
import br.com.fiap.VetSync.entity.StatusEvento;
import br.com.fiap.VetSync.entity.TipoEvento;
import br.com.fiap.VetSync.entity.Veterinario;
import br.com.fiap.VetSync.repository.DisponibilidadeRepository;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import br.com.fiap.VetSync.repository.TipoEventoRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Calcula slots apresentáveis ao tutor sem reservar nenhum horário. */
@Service
@RequiredArgsConstructor
public class AgendaSlotsService {

    public record SlotDisponivel(
            Long idTipoEvento,
            String nmTipoEvento,
            Long idVeterinario,
            String nmVeterinario,
            String hrEvento
    ) {}

    private final TipoEventoRepository tipoEventoRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final DisponibilidadeRepository disponibilidadeRepository;
    private final EventoSaudeRepository eventoSaudeRepository;
    private final AgendaService agendaService;

    public List<SlotDisponivel> listarSlots(LocalDate data, String modalidade) {
        if (data == null || data.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data da disponibilidade deve ser hoje ou uma data futura");
        }

        List<TipoEvento> tipos = tipoEventoRepository
                .findByDsModalidadeAgendamentoIgnoreCase(modalidade);
        if (tipos.isEmpty()) {
            return List.of();
        }

        List<SlotDisponivel> slots = new ArrayList<>();
        Set<String> chavesGeradas = new HashSet<>();
        int diaSemana = data.getDayOfWeek().getValue();

        for (Veterinario veterinario : veterinarioRepository.findAll()) {
            Long idVeterinario = veterinario.getIdVeterinario();
            if (agendaService.estaBloqueado(idVeterinario, data)) {
                continue;
            }

            Set<String> horariosOcupados = eventoSaudeRepository
                    .findByVeterinario_IdVeterinarioAndDtEvento(idVeterinario, data)
                    .stream()
                    .filter(evento -> evento.getDsStatus() == StatusEvento.AGENDADO)
                    .map(evento -> evento.getHrEvento())
                    .collect(java.util.stream.Collectors.toSet());

            List<Disponibilidade> disponibilidades = disponibilidadeRepository
                    .findByVeterinario_IdVeterinario(idVeterinario)
                    .stream()
                    .filter(disponibilidade -> disponibilidade.getNrDiaSemana().equals(diaSemana))
                    .toList();

            for (TipoEvento tipo : tipos) {
                int duracao = tipo.getNrDuracaoMinutos() == null ? 30 : tipo.getNrDuracaoMinutos();
                if (duracao <= 0) {
                    continue;
                }

                for (Disponibilidade disponibilidade : disponibilidades) {
                    adicionarSlotsDaDisponibilidade(
                            slots,
                            chavesGeradas,
                            horariosOcupados,
                            tipo,
                            veterinario,
                            disponibilidade,
                            duracao
                    );
                }
            }
        }

        return slots.stream()
                .sorted(Comparator.comparing(SlotDisponivel::hrEvento)
                        .thenComparing(SlotDisponivel::nmVeterinario)
                        .thenComparing(SlotDisponivel::nmTipoEvento))
                .toList();
    }

    private void adicionarSlotsDaDisponibilidade(
            List<SlotDisponivel> slots,
            Set<String> chavesGeradas,
            Set<String> horariosOcupados,
            TipoEvento tipo,
            Veterinario veterinario,
            Disponibilidade disponibilidade,
            int duracao
    ) {
        LocalTime inicio = LocalTime.parse(disponibilidade.getHrInicio());
        LocalTime fim = LocalTime.parse(disponibilidade.getHrFim());

        for (LocalTime horario = inicio; !horario.plusMinutes(duracao).isAfter(fim); horario = horario.plusMinutes(duracao)) {
            String hrEvento = horario.toString();
            String chave = tipo.getIdTipoEvento() + ":" + veterinario.getIdVeterinario() + ":" + hrEvento;
            if (horariosOcupados.contains(hrEvento) || !chavesGeradas.add(chave)) {
                continue;
            }
            slots.add(new SlotDisponivel(
                    tipo.getIdTipoEvento(),
                    tipo.getNmTipoEvento(),
                    veterinario.getIdVeterinario(),
                    veterinario.getNmVeterinario(),
                    hrEvento
            ));
        }
    }
}
