package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.entity.PlanoItem;
import br.com.fiap.VetSync.entity.PlanoTratamento;
import br.com.fiap.VetSync.security.PerfilUtils;
import br.com.fiap.VetSync.service.PlanoTratamentoService;
import br.com.fiap.VetSync.service.VeterinarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/planos")
@RequiredArgsConstructor
@Tag(name = "Planos de Tratamento", description = "Veterinário prescreve uma sequência de itens futuros; tutor agenda um de cada vez e ganha bônus ao completar tudo em ordem")
public class PlanoTratamentoController {

    private final PlanoTratamentoService planoTratamentoService;
    private final VeterinarioService veterinarioService;

    public record PlanoTratamentoRequest(
            @NotNull(message = "idPet é obrigatório") Long idPet,
            @PositiveOrZero(message = "nrPontosBonus não pode ser negativo") Integer nrPontosBonus,
            @NotEmpty(message = "idsTipoEvento precisa ter pelo menos 2 itens") List<Long> idsTipoEvento
    ) {}

    public record PlanoItemAgendarRequest(
            @NotNull(message = "idVeterinario é obrigatório") Long idVeterinario,
            @NotNull(message = "dtEvento é obrigatória") LocalDate dtEvento,
            @NotBlank(message = "hrEvento é obrigatória")
            @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "hrEvento deve estar no formato HH:mm, ex: 14:30")
            String hrEvento,
            String dsObservacao
    ) {}

    public record PlanoItemResponse(
            Long idItem, Integer nrOrdem, String nmTipoEvento, String status, Long idEvento, LocalDate dtEvento, String hrEvento
    ) {}

    public record PlanoTratamentoResponse(
            Long idPlano, String status, Integer nrPontosBonus,
            String nmPet, String nmVeterinario, LocalDate dtCriacao,
            List<PlanoItemResponse> itens
    ) {}

    private PlanoItemResponse toResponse(PlanoItem item) {
        EventoSaude evento = item.getEvento();
        return new PlanoItemResponse(
                item.getIdItem(),
                item.getNrOrdem(),
                item.getTipoEvento() != null ? item.getTipoEvento().getNmTipoEvento() : null,
                item.getDsStatus().name(),
                evento != null ? evento.getIdEvento() : null,
                evento != null ? evento.getDtEvento() : null,
                evento != null ? evento.getHrEvento() : null
        );
    }

    private PlanoTratamentoResponse toResponse(PlanoTratamento plano) {
        List<PlanoItemResponse> itens = planoTratamentoService.listarItens(plano.getIdPlano())
                .stream().map(this::toResponse).toList();
        return new PlanoTratamentoResponse(
                plano.getIdPlano(),
                plano.getDsStatus().name(),
                plano.getNrPontosBonus(),
                plano.getPet() != null ? plano.getPet().getNmPet() : null,
                plano.getVeterinario() != null ? plano.getVeterinario().getNmVeterinario() : null,
                plano.getDtCriacao(),
                itens
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VETERINARIO')")
    @Operation(summary = "Veterinário prescreve um plano de tratamento",
            description = "idsTipoEvento define a sequência (ordem da lista = ordem do plano). Mínimo 2 itens.")
    public PlanoTratamentoResponse criar(Authentication authentication, @Valid @RequestBody PlanoTratamentoRequest request) {
        Long idVeterinario = veterinarioService.buscarAutenticado(authentication).getIdVeterinario();
        PlanoTratamento plano = planoTratamentoService.criar(
                request.idPet(), idVeterinario, request.nrPontosBonus(), request.idsTipoEvento());
        return toResponse(plano);
    }

    @GetMapping
    @Operation(summary = "Listar planos de tratamento", description = "Tutor vê os dos próprios pets; veterinário vê os que ele prescreveu.")
    public List<PlanoTratamentoResponse> listar(Authentication authentication) {
        List<PlanoTratamento> planos = PerfilUtils.isVeterinario(authentication)
                ? planoTratamentoService.listarParaVeterinario(authentication.getName())
                : planoTratamentoService.listarParaTutor(authentication.getName());
        return planos.stream().map(this::toResponse).toList();
    }

    @GetMapping("/pet/{idPet}")
    @PreAuthorize("hasRole('TUTOR') and @petAccessSecurity.canView(#idPet, authentication)")
    @Operation(summary = "Listar planos registrados pelo veterinário para um pet do tutor")
    public List<PlanoTratamentoResponse> listarPorPet(@PathVariable Long idPet, Authentication authentication) {
        return planoTratamentoService.listarParaTutor(authentication.getName()).stream()
                .filter(plano -> plano.getPet() != null && idPet.equals(plano.getPet().getIdPet()))
                .map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@planoTratamentoSecurity.isRelacionadoAoPlano(#id, authentication)")
    @Operation(summary = "Buscar plano por ID, com os itens da sequência (só tutor dono ou veterinário que prescreveu)")
    public PlanoTratamentoResponse buscarPorId(@PathVariable Long id) {
        return toResponse(planoTratamentoService.buscarPorId(id));
    }

    @PatchMapping("/itens/{idItem}/agendar")
    @PreAuthorize("hasRole('TUTOR') and @planoTratamentoSecurity.isTutorDoItem(#idItem, authentication)")
    @Operation(summary = "Tutor agenda o próximo item pendente do plano",
            description = "Escolhe só veterinário e horário livre — o tipo de evento e o pet já vêm do item do plano.")
    public PlanoItemResponse agendarItem(@PathVariable Long idItem, @Valid @RequestBody PlanoItemAgendarRequest request) {
        planoTratamentoService.agendarItem(idItem, request.idVeterinario(), request.dtEvento(), request.hrEvento(), request.dsObservacao());
        return toResponse(planoTratamentoService.buscarItemPorId(idItem));
    }
}
