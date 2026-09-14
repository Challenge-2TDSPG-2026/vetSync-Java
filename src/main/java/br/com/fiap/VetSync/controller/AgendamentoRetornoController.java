package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.entity.LinkAgendamentoVet;
import br.com.fiap.VetSync.service.LinkAgendamentoVetService;
import br.com.fiap.VetSync.service.VeterinarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/agendamentos-retorno")
@RequiredArgsConstructor
@Tag(name = "Agendamento de Retorno (link público)", description = "Endpoint acessado pelo link enviado por e-mail ao tutor, sem necessidade de login")
public class AgendamentoRetornoController {

    private final LinkAgendamentoVetService linkAgendamentoVetService;
    private final VeterinarioService veterinarioService;

    public record VeterinarioOpcao(Long idVeterinario, String nmVeterinario, String nrCrmv) {}

    public record LinkDetalheResponse(
            String status,
            String nmPet,
            String dsProblema,
            List<VeterinarioOpcao> veterinariosDisponiveis
    ) {}

    public record ConfirmarAgendamentoRequest(
            @NotNull(message = "idVeterinario é obrigatório") Long idVeterinario,
            @NotNull(message = "dtEvento é obrigatória") LocalDate dtEvento,
            @NotBlank(message = "hrEvento é obrigatória")
            @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "hrEvento deve estar no formato HH:mm")
            String hrEvento
    ) {}

    public record AgendamentoConfirmadoResponse(Long idEvento, LocalDate dtEvento, String hrEvento, String nmVeterinario) {}

    @GetMapping("/{token}")
    @Operation(summary = "Ver detalhes de uma observação relatada pela estética e a lista de veterinários para escolher")
    public LinkDetalheResponse detalhar(@PathVariable String token) {
        LinkAgendamentoVet link = linkAgendamentoVetService.buscarLinkValido(token);
        var evento = link.getRelatorioEstetica().getEvento();
        var pet = evento.getPet();

        List<VeterinarioOpcao> opcoes = veterinarioService.listarTodos().stream()
                .map(v -> new VeterinarioOpcao(v.getIdVeterinario(), v.getNmVeterinario(), v.getNrCrmv()))
                .toList();

        return new LinkDetalheResponse(
                link.getDsStatus().name(),
                pet != null ? pet.getNmPet() : null,
                link.getRelatorioEstetica().getDsProblema(),
                opcoes
        );
    }

    @PostMapping("/{token}/confirmar")
    @Operation(summary = "Tutor escolhe veterinário, data e horário para levar o pet após a observação da estética",
            description = "Cria um evento de saúde AGENDADO com o veterinário escolhido e marca o link como utilizado.")
    public AgendamentoConfirmadoResponse confirmar(@PathVariable String token, @Valid @RequestBody ConfirmarAgendamentoRequest request) {
        EventoSaude evento = linkAgendamentoVetService.confirmarAgendamento(
                token, request.idVeterinario(), request.dtEvento(), request.hrEvento()
        );
        return new AgendamentoConfirmadoResponse(
                evento.getIdEvento(), evento.getDtEvento(), evento.getHrEvento(),
                evento.getVeterinario() != null ? evento.getVeterinario().getNmVeterinario() : null
        );
    }
}