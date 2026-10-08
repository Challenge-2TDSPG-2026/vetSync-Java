package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.service.AgendaDoDiaService;
import br.com.fiap.VetSync.service.AgendaSlotsService;
import br.com.fiap.VetSync.service.AgendaServicoClinicaService;
import br.com.fiap.VetSync.service.EventoService;
import br.com.fiap.VetSync.security.PetAccessSecurity;
import br.com.fiap.VetSync.entity.EventoSaude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
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
@Tag(name = "Agenda", description = "Disponibilidade para o agendamento do tutor e agenda do dia para o admin")
public class AgendaController {

    private final AgendaSlotsService agendaSlotsService;
    private final AgendaDoDiaService agendaDoDiaService;
    private final AgendaServicoClinicaService agendaServicoClinicaService;
    private final EventoService eventoService;
    private final PetAccessSecurity petAccessSecurity;

    public record SlotsResponse(LocalDate data, List<AgendaSlotsService.SlotDisponivel> slots) {}

    @GetMapping("/slots")
    @PreAuthorize("hasRole('TUTOR')")
    @Operation(summary = "Listar slots livres", description = "Calcula horários livres por modalidade usando disponibilidade semanal, bloqueios e eventos agendados. Não cria reserva.")
    public SlotsResponse listarSlots(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(defaultValue = "CLINICO_GERAL") String modalidade
    ) {
        if (!"CLINICO_GERAL".equalsIgnoreCase(modalidade)) return new SlotsResponse(data, List.of());
        return new SlotsResponse(data, agendaServicoClinicaService.servicosDoTutor(authentication.getName()).stream()
                .filter(s -> "Consulta de rotina".equalsIgnoreCase(s.nome()))
                .flatMap(s -> agendaServicoClinicaService.slots(authentication.getName(), s.id(), data).stream()
                        .filter(slot -> "VETERINARIO".equals(slot.tipoProfissional()))
                        .map(slot -> new AgendaSlotsService.SlotDisponivel(
                                eventoTipoConsulta(), s.nome(), slot.idProfissional(), slot.nomeProfissional(), slot.hora())))
                .toList());
    }

    private Long eventoTipoConsulta() {
        return tipoEventoRepository.findFirstByNmTipoEventoIgnoreCaseAndClinicaIsNull("Consulta de rotina")
                .map(t -> t.getIdTipoEvento()).orElse(null);
    }

    private final br.com.fiap.VetSync.repository.TipoEventoRepository tipoEventoRepository;

    public record AgendarServicoRequest(@NotNull Long idPet, Long idVeterinario,
                                        Long idProfissionalEstetica, @NotNull LocalDate data,
                                        @NotBlank String hora, String observacao) {}
    public record AgendamentoCriado(Long idEvento, Long idServico, LocalDate data, String hora) {}

    @GetMapping("/servicos")
    @PreAuthorize("hasRole('TUTOR')")
    public List<AgendaServicoClinicaService.ServicoDisponivel> servicos(Authentication auth) {
        return agendaServicoClinicaService.servicosDoTutor(auth.getName());
    }

    @GetMapping("/servicos/{idServico}/slots")
    @PreAuthorize("hasRole('TUTOR')")
    public List<AgendaServicoClinicaService.SlotServico> slotsServico(Authentication auth, @PathVariable Long idServico,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return agendaServicoClinicaService.slots(auth.getName(), idServico, data);
    }

    @PostMapping("/servicos/{idServico}/agendar")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TUTOR')")
    public AgendamentoCriado agendarServico(Authentication auth, @PathVariable Long idServico,
                                            @Valid @RequestBody AgendarServicoRequest req) {
        if (!petAccessSecurity.canEdit(req.idPet(), auth)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para agendar este pet");
        }
        EventoSaude evento = EventoSaude.builder().dtEvento(req.data()).hrEvento(req.hora())
                .dsObservacao(req.observacao()).dsObservacaoTutor(req.observacao()).build();
        EventoSaude salvo = eventoService.agendarServico(evento, req.idPet(), idServico,
                req.idVeterinario(), req.idProfissionalEstetica());
        return new AgendamentoCriado(salvo.getIdEvento(), idServico, salvo.getDtEvento(), salvo.getHrEvento());
    }

    @GetMapping("/dia")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atendimentos do dia (admin)", description = "Todos os eventos da data informada, de qualquer status, ordenados por horário. Usado no painel do admin.")
    public List<AgendaDoDiaService.AtendimentoDia> atendimentosDoDia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        return agendaDoDiaService.listar(data);
    }
}
