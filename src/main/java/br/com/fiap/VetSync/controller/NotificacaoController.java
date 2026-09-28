package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
@Tag(name = "Notificações")
public class NotificacaoController {

    private final NotificacaoService service;

    public record DispositivoRequest(
            @NotBlank String token,
            @NotNull PlataformaPush plataforma,
            String nomeDispositivo,
            String fusoHorario
    ) {}

    public record DispositivoResponse(Long id, String token, PlataformaPush plataforma,
                                      String nomeDispositivo, String fusoHorario, boolean ativo) {}

    public record NotificacaoResponse(Long id, TipoNotificacao tipo, String titulo, String mensagem,
                                      String referenciaTipo, Long referenciaId, boolean lida,
                                      java.time.LocalDateTime enviadaEm, java.time.LocalDateTime criadaEm) {}

    @PostMapping("/dispositivos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar ou reativar dispositivo push")
    public DispositivoResponse registrar(@Valid @RequestBody DispositivoRequest request, Authentication auth) {
        return toResponse(service.registrarDispositivo(auth.getName(), request.token(), request.plataforma(),
                request.nomeDispositivo(), request.fusoHorario()));
    }

    @DeleteMapping("/dispositivos/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable String token, Authentication auth) {
        service.removerDispositivo(auth.getName(), token);
    }

    @GetMapping
    public Page<NotificacaoResponse> listar(@RequestParam(required = false) Boolean lida,
                                            Pageable pageable, Authentication auth) {
        return service.listar(auth.getName(), lida, pageable).map(this::toResponse);
    }

    @PatchMapping("/{id}/lida")
    public NotificacaoResponse marcarComoLida(@PathVariable Long id, Authentication auth) {
        return toResponse(service.marcarComoLida(auth.getName(), id));
    }

    @PatchMapping("/lidas")
    public void marcarTodasComoLidas(Authentication auth) {
        service.marcarTodasComoLidas(auth.getName());
    }

    private DispositivoResponse toResponse(DispositivoPush item) {
        return new DispositivoResponse(item.getIdDispositivo(), item.getToken(), item.getPlataforma(),
                item.getNomeDispositivo(), item.getFusoHorario(), item.isAtivo());
    }

    private NotificacaoResponse toResponse(Notificacao item) {
        return new NotificacaoResponse(item.getIdNotificacao(), item.getTipo(), item.getTitulo(),
                item.getMensagem(), item.getReferenciaTipo(), item.getReferenciaId(), item.isLida(),
                item.getEnviadaEm(), item.getCriadaEm());
    }

}
