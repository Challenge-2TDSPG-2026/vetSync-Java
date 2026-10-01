package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Visão do admin: todos os atendimentos (eventos) de um dia, ordenados por horário. */
@Service
@RequiredArgsConstructor
public class AgendaDoDiaService {

    private final EventoSaudeRepository eventoSaudeRepository;

    public record AtendimentoDia(
            Long idEvento,
            String hrEvento,
            String status,
            Long idPet,
            String nmPet,
            String raca,
            String nmTutor,
            String nmTipoEvento,
            String dsCategoria,
            String nmProfissional,
            String nmClinica
    ) {}

    @Transactional(readOnly = true)
    public List<AtendimentoDia> listar(LocalDate data) {
        return eventoSaudeRepository.findAgendaDoDia(data).stream()
                // hr_evento é "HH:mm", então a ordem alfabética é a cronológica; sem horário vai para o fim.
                .sorted(Comparator
                        .comparing(EventoSaude::getHrEvento, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(EventoSaude::getIdEvento))
                .map(this::toDto)
                .toList();
    }

    private AtendimentoDia toDto(EventoSaude e) {
        var pet = e.getPet();
        return new AtendimentoDia(
                e.getIdEvento(),
                e.getHrEvento(),
                e.getDsStatus().name(),
                pet.getIdPet(),
                pet.getNmPet(),
                pet.getRaca() != null ? pet.getRaca().getNmRaca() : null,
                pet.getTutor() != null ? pet.getTutor().getNmTutor() : null,
                e.getTipoEvento().getNmTipoEvento(),
                e.getTipoEvento().getDsCategoria(),
                e.getVeterinario() != null ? e.getVeterinario().getNmVeterinario()
                        : e.getProfissionalEstetica() != null ? e.getProfissionalEstetica().getNmProfissionalEstetica()
                          : null,
                e.getClinica() != null ? e.getClinica().getNmClinica() : null
        );
    }
}