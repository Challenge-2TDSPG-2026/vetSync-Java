package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.service.AgendaSlotsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/agenda")
@RequiredArgsConstructor
@Tag(name = "Agenda", description = "Disponibilidade real para o fluxo de agendamento do tutor")
public class AgendaController {

    private final AgendaSlotsService agendaSlotsService;

    public record SlotsResponse(LocalDate data, List<AgendaSlotsService.SlotDisponivel> slots) {}

    @GetMapping("/slots")
    @PreAuthorize("hasRole('TUTOR')")
    @Operation(summary = "Listar slots livres", description = "Calcula horários livres por modalidade usando disponibilidade semanal, bloqueios e eventos agendados. Não cria reserva.")
    public SlotsResponse listarSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(defaultValue = "CLINICO_GERAL") String modalidade
    ) {
        return new SlotsResponse(data, agendaSlotsService.listarSlots(data, modalidade));
    }
}
