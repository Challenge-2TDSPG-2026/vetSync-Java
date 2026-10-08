package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.ListaEspera;
import br.com.fiap.VetSync.entity.StatusListaEspera;
import br.com.fiap.VetSync.security.PetAccessSecurity;
import br.com.fiap.VetSync.service.ListaEsperaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/agenda/espera")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TUTOR')")
@Tag(name = "Lista de espera", description = "O tutor entra na fila de um serviço e é avisado (push + notificação) quando surge uma vaga compatível.")
public class ListaEsperaController {

    private final ListaEsperaService service;
    private final PetAccessSecurity petAccessSecurity;

    public record EntrarRequest(
            @NotNull(message = "idPet é obrigatório") Long idPet,
            @NotNull(message = "idServico é obrigatório") Long idServico,
            @NotNull(message = "dataInicio é obrigatória") LocalDate dataInicio,
            @NotNull(message = "dataFim é obrigatória") LocalDate dataFim,
            @Pattern(regexp = "^$|^([01]\\d|2[0-3]):[0-5]\\d$", message = "horaMin deve estar no formato HH:mm") String horaMin,
            @Pattern(regexp = "^$|^([01]\\d|2[0-3]):[0-5]\\d$", message = "horaMax deve estar no formato HH:mm") String horaMax,
            Long idVeterinario,
            Long idProfissionalEstetica
    ) {}

    public record EsperaResponse(
            Long id,
            StatusListaEspera status,
            Long idPet,
            String nmPet,
            Long idServico,
            String nmServico,
            LocalDate dataInicio,
            LocalDate dataFim,
            String horaMin,
            String horaMax,
            Long idVeterinario,
            Long idProfissionalEstetica,
            LocalDate dataVaga,
            String horaVaga,
            String tipoProfissionalVaga,
            Long idProfissionalVaga,
            LocalDateTime notificadaEm,
            LocalDateTime criadaEm
    ) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Entrar na lista de espera de um serviço",
            description = "Informe o período desejado e, opcionalmente, faixa de horário e profissional. Quando surgir uma vaga compatível o tutor recebe uma notificação do tipo VAGA_DISPONIVEL.")
    public EsperaResponse entrar(Authentication authentication, @Valid @RequestBody EntrarRequest request) {
        if (!petAccessSecurity.canEdit(request.idPet(), authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não tem permissão para agendar serviços para esse pet");
        }
        ListaEspera entrada = service.entrar(authentication.getName(), new ListaEsperaService.EntradaListaEspera(
                request.idPet(), request.idServico(), request.dataInicio(), request.dataFim(),
                request.horaMin(), request.horaMax(), request.idVeterinario(), request.idProfissionalEstetica()));
        return toResponse(entrada);
    }

    @GetMapping
    @Operation(summary = "Listar as entradas ativas (AGUARDANDO ou NOTIFICADO) do tutor autenticado")
    public List<EsperaResponse> listar(Authentication authentication) {
        return service.listarAtivas(authentication.getName()).stream().map(this::toResponse).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Sair da lista de espera")
    public void cancelar(Authentication authentication, @PathVariable Long id) {
        service.cancelar(authentication.getName(), id);
    }

    private EsperaResponse toResponse(ListaEspera e) {
        return new EsperaResponse(
                e.getIdEspera(),
                e.getStatus(),
                e.getPet().getIdPet(),
                e.getPet().getNmPet(),
                e.getServicoClinica().getIdServicoClinica(),
                e.getServicoClinica().getNmServico(),
                e.getDataInicio(),
                e.getDataFim(),
                e.getHoraMin(),
                e.getHoraMax(),
                e.getIdVeterinario(),
                e.getIdProfissionalEstetica(),
                e.getDataVaga(),
                e.getHoraVaga(),
                e.getTipoProfissionalVaga(),
                e.getIdProfissionalVaga(),
                e.getNotificadaEm(),
                e.getCriadaEm());
    }
}