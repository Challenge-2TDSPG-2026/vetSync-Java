package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.security.CarteiraPublicaRateLimiter;
import br.com.fiap.VetSync.service.CarteiraCompartilhadaService;
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
 * Rota pública (sem JWT) que renderiza a carteira de vacinação do pet a partir de um
 * token de uso único. A página é gerada dinamicamente a cada acesso — nada é salvo em
 * disco — para sempre refletir o estado atual das vacinas.
 */
@Controller
@RequiredArgsConstructor
@Tag(name = "Carteira Pública", description = "Página pública, temporária e somente leitura da carteira de vacinação")
public class CarteiraPublicaController {

    private final CarteiraCompartilhadaService carteiraCompartilhadaService;
    private final CarteiraPublicaRateLimiter rateLimiter;

    @GetMapping(value = "/carteiras-publicas/{token}", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Exibir a carteira de vacinação pública (HTML)",
            description = "Não exige autenticação. Token inválido, expirado ou revogado resulta em 404/410 " +
                    "genéricos (página HTML), sem indicar se o pet existe.")
    public String exibir(@PathVariable String token, HttpServletRequest request, HttpServletResponse response, Model model) {
        aplicarCabecalhosDeSeguranca(response);

        // Tratado aqui (e não deixado subir para o GlobalExceptionHandler) para que o
        // resultado seja sempre uma página HTML genérica, nunca um JSON de erro que
        // possa insinuar se o token/pet existe.
        if (!rateLimiter.permitir(chaveDoCliente(request))) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return "carteira-publica-indisponivel";
        }

        try {
            CarteiraCompartilhadaService.CarteiraPublica carteira = carteiraCompartilhadaService.resolverParaExibicaoPublica(token);
            model.addAttribute("numeroPet", carteira.numeroPet());
            model.addAttribute("nomePet", carteira.nomePet());
            model.addAttribute("especie", carteira.especie());
            model.addAttribute("raca", carteira.raca());
            model.addAttribute("atualizadaEm", carteira.atualizadaEm());
            model.addAttribute("vacinas", carteira.vacinas());
            model.addAttribute("fotoUrl", carteira.temFoto() ? "/carteiras-publicas/" + token + "/foto" : null);
            return "carteira-publica";
        } catch (ResponseStatusException ex) {
            response.setStatus(ex.getStatusCode().value());
            return "carteira-publica-indisponivel";
        }
    }

    // NOVO: foto do pet para a página pública. Só é servida com um token de carteira válido.
    @GetMapping("/carteiras-publicas/{token}/foto")
    @ResponseBody
    @Operation(summary = "Foto do pet exibida na carteira pública",
            description = "Não exige autenticação, mas exige token de carteira válido (não expirado nem revogado).")
    public ResponseEntity<byte[]> foto(@PathVariable String token, HttpServletRequest request) {
        if (!rateLimiter.permitir(chaveDoCliente(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        try {
            CarteiraCompartilhadaService.FotoPublica foto = carteiraCompartilhadaService.resolverFotoPublica(token);
            MediaType mediaType = foto.tipo() != null
                    ? MediaType.parseMediaType(foto.tipo())
                    : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header("Cache-Control", "no-store")
                    .header("X-Robots-Tag", "noindex, nofollow")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(foto.bytes());
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).build();
        }
    }

    private void aplicarCabecalhosDeSeguranca(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Robots-Tag", "noindex, nofollow");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }

    /**
     * Chave transitória só para o rate limiter (nunca persistida). Considera o cabeçalho
     * de proxy quando presente, caindo para o IP remoto da requisição.
     */
    private String chaveDoCliente(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}