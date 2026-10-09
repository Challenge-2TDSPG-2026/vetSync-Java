package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.security.CarteiraPublicaRateLimiter;
import br.com.fiap.VetSync.service.ProntuarioCompartilhamentoService;
import br.com.fiap.VetSync.service.ProntuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rotas públicas (sem JWT) do prontuário compartilhado. O token é a credencial: sem ele, nada é exibido.
 * Link inexistente, expirado ou revogado resulta numa página genérica (404/410), sem indicar se o pet existe.
 */
@Controller
@RequiredArgsConstructor
@Tag(name = "Prontuário Público", description = "Visualização temporária e somente leitura do prontuário compartilhado pelo tutor")
public class ProntuarioPublicoController {

    private final ProntuarioCompartilhamentoService compartilhamentoService;
    private final CarteiraPublicaRateLimiter rateLimiter;

    @GetMapping(value = "/prontuarios-publicos/{token}", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Exibir o prontuário compartilhado (HTML)")
    public String exibir(@PathVariable String token, HttpServletRequest request, HttpServletResponse response, Model model) {
        cabecalhos(response);
        if (!rateLimiter.permitir(chave(request))) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return "carteira-publica-indisponivel";
        }
        try {
            ProntuarioService.Prontuario prontuario = compartilhamentoService.visualizar(token);
            model.addAllAttributes(ProntuarioView.variaveis(prontuario, "/prontuarios-publicos/" + token + "/exames/"));
            return "prontuario";
        } catch (ResponseStatusException ex) {
            response.setStatus(ex.getStatusCode().value());
            return "carteira-publica-indisponivel";
        }
    }

    @GetMapping(value = "/prontuarios-publicos/{token}/dados", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Operation(summary = "Prontuário compartilhado em JSON (para o outro atendimento importar)")
    public ResponseEntity<ProntuarioService.Prontuario> dados(@PathVariable String token, HttpServletRequest request) {
        if (!rateLimiter.permitir(chave(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        try {
            return ResponseEntity.ok()
                    .header("Cache-Control", "no-store")
                    .header("X-Robots-Tag", "noindex, nofollow")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(compartilhamentoService.visualizar(token));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).build();
        }
    }

    @GetMapping("/prontuarios-publicos/{token}/exames/{idExame}/arquivo")
    @ResponseBody
    @Operation(summary = "Baixar o arquivo de um exame incluído no compartilhamento")
    public ResponseEntity<byte[]> arquivoDoExame(@PathVariable String token, @PathVariable Long idExame,
                                                 HttpServletRequest request) {
        if (!rateLimiter.permitir(chave(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        try {
            ProntuarioCompartilhamentoService.ArquivoExame arquivo = compartilhamentoService.arquivoDoExame(token, idExame);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(arquivo.mimeType()))
                    .header("Content-Disposition", "attachment; filename=\"" + arquivo.nome() + "\"")
                    .header("Cache-Control", "no-store")
                    .header("X-Robots-Tag", "noindex, nofollow")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(arquivo.bytes());
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).build();
        }
    }

    private void cabecalhos(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Robots-Tag", "noindex, nofollow");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }

    /** Chave transitória só para o rate limiter (nunca persistida). */
    private String chave(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}