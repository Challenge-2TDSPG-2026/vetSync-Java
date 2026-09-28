package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.AcaoIa;
import br.com.fiap.VetSync.service.AcaoIaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ia/acoes")
@RequiredArgsConstructor
@Tag(name = "IA", description = "Pré-visualização e confirmação de ações assistidas")
public class AcaoIaController {
    private final AcaoIaService service;

    public record PreviewRequest(@NotBlank String acao, @NotBlank String resumo, @NotBlank String dados) {}
    public record PreviewResponse(Long id, String acao, String resumo, String dados, boolean requerConfirmacao) {}

    private PreviewResponse response(AcaoIa a) {
        return new PreviewResponse(a.getIdAcao(), a.getAcao(), a.getResumo(), a.getDados(), true);
    }

    @PostMapping("/preview")
    @ResponseStatus(HttpStatus.CREATED)
    public PreviewResponse preview(Authentication authentication, @Valid @RequestBody PreviewRequest request) {
        return response(service.criar(authentication.getName(), request.acao(), request.resumo(), request.dados()));
    }

    @PostMapping("/{id}/confirmar")
    public PreviewResponse confirmar(Authentication authentication, @PathVariable Long id) {
        return response(service.confirmar(id, authentication.getName()));
    }
}
