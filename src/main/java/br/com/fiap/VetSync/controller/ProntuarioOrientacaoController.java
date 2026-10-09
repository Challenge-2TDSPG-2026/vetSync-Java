package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.OrientacaoClinica;
import br.com.fiap.VetSync.service.ProntuarioRegistroService;
import br.com.fiap.VetSync.service.ProntuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/eventos/{idEvento}/orientacoes")
@RequiredArgsConstructor
@Tag(name = "Prontuário - Orientações", description = "Orientações passadas ao tutor em cada atendimento")
public class ProntuarioOrientacaoController {

    private final ProntuarioRegistroService registroService;

    public record OrientacaoRequest(
            @NotBlank(message = "titulo é obrigatório") @Size(max = 120, message = "titulo deve ter no máximo 120 caracteres") String titulo,
            @NotBlank(message = "texto é obrigatório") @Size(max = 2000, message = "texto deve ter no máximo 2000 caracteres") String texto
    ) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VETERINARIO') and @eventoSecurity.isVeterinarioResponsavel(#idEvento, authentication)")
    @Operation(summary = "Veterinário responsável registra uma orientação para o tutor neste atendimento")
    public ProntuarioService.Orientacao criar(@PathVariable Long idEvento, @Valid @RequestBody OrientacaoRequest request,
                                              Authentication authentication) {
        return toResponse(registroService.criarOrientacao(idEvento, request.titulo(), request.texto(),
                authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("@eventoSecurity.isRelacionado(#idEvento, authentication)")
    @Operation(summary = "Listar as orientações de um atendimento (mesma visibilidade dos detalhes do evento)")
    public List<ProntuarioService.Orientacao> listar(@PathVariable Long idEvento) {
        return registroService.listarOrientacoes(idEvento).stream().map(ProntuarioOrientacaoController::toResponse).toList();
    }

    @DeleteMapping("/{idOrientacao}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('VETERINARIO') and @eventoSecurity.isVeterinarioResponsavel(#idEvento, authentication)")
    @Operation(summary = "Veterinário responsável remove uma orientação (a remoção fica na auditoria)")
    public void remover(@PathVariable Long idEvento, @PathVariable Long idOrientacao, Authentication authentication) {
        registroService.removerOrientacao(idEvento, idOrientacao, authentication.getName());
    }

    static ProntuarioService.Orientacao toResponse(OrientacaoClinica o) {
        return new ProntuarioService.Orientacao(o.getIdOrientacao(), o.getEvento().getIdEvento(),
                o.getEvento().getDtEvento(), o.getDsTitulo(), o.getDsTexto(),
                o.getVeterinario() != null ? o.getVeterinario().getNmVeterinario() : o.getDsAtor(),
                o.getDtCriacao());
    }
}