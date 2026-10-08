package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.service.EventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/eventos")
@RequiredArgsConstructor
@Tag(name = "Confirmação de agendamentos", description = "A clínica confirma ou recusa solicitações de agendamento. O tutor é avisado por push.")
public class EventoConfirmacaoController {

    private final EventoService eventoService;

    public record RecusarRequest(@NotBlank(message = "motivo da recusa é obrigatório") String motivo) {}

    public record ConfirmacaoResponse(Long idEvento, String status, String statusConfirmacao, String motivoCancelamento) {}

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("(hasRole('VETERINARIO') and @eventoSecurity.isVeterinarioResponsavel(#id, authentication)) " +
            "or (hasRole('PROFISSIONAL_ESTETICA') and @eventoSecurity.isProfissionalEsteticaResponsavel(#id, authentication))")
    @Operation(summary = "Confirma um agendamento pendente", description = "Só vale para eventos AGENDADO com confirmação PENDENTE. Avisa o tutor.")
    public ConfirmacaoResponse confirmar(@PathVariable Long id) {
        return toResponse(eventoService.confirmar(id));
    }

    @PatchMapping("/{id}/recusar")
    @PreAuthorize("(hasRole('VETERINARIO') and @eventoSecurity.isVeterinarioResponsavel(#id, authentication)) " +
            "or (hasRole('PROFISSIONAL_ESTETICA') and @eventoSecurity.isProfissionalEsteticaResponsavel(#id, authentication))")
    @Operation(summary = "Recusa um agendamento pendente", description = "Cancela o evento com o motivo informado, libera o horário (e avisa a lista de espera) e avisa o tutor.")
    public ConfirmacaoResponse recusar(@PathVariable Long id, @Valid @RequestBody RecusarRequest request) {
        return toResponse(eventoService.recusar(id, request.motivo()));
    }

    private ConfirmacaoResponse toResponse(EventoSaude e) {
        return new ConfirmacaoResponse(
                e.getIdEvento(),
                e.getDsStatus().name(),
                e.getDsConfirmacao() == null ? null : e.getDsConfirmacao().name(),
                e.getDsMotivoCancelamento());
    }
}