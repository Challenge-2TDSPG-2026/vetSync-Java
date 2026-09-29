package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.PreferenciaNotificacao;
import br.com.fiap.VetSync.service.NotificacaoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios/preferencias/notificacoes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TUTOR')")
@Tag(name = "Notificações")
public class PreferenciaNotificacaoController {

    private final NotificacaoService service;

    public record PreferenciasRequest(boolean pushAtivo, boolean lembreteSeteDias, boolean lembreteUmDia,
                                      boolean lembreteDuasHoras, boolean vacinasVencendo,
                                      boolean retornosPendentes, boolean convitesDeAcesso, boolean resgates) {}

    public record PreferenciasResponse(boolean pushAtivo, boolean lembreteSeteDias, boolean lembreteUmDia,
                                       boolean lembreteDuasHoras, boolean vacinasVencendo,
                                       boolean retornosPendentes, boolean convitesDeAcesso, boolean resgates) {}

    @GetMapping
    public PreferenciasResponse buscar(Authentication auth) {
        return toResponse(service.buscarPreferencias(auth.getName()));
    }

    @PutMapping
    public PreferenciasResponse atualizar(@Valid @RequestBody PreferenciasRequest request,
                                          Authentication auth) {
        PreferenciaNotificacao preferencias = PreferenciaNotificacao.builder()
                .pushAtivo(request.pushAtivo()).lembreteSeteDias(request.lembreteSeteDias())
                .lembreteUmDia(request.lembreteUmDia()).lembreteDuasHoras(request.lembreteDuasHoras())
                .vacinasVencendo(request.vacinasVencendo()).retornosPendentes(request.retornosPendentes())
                .convitesDeAcesso(request.convitesDeAcesso()).resgates(request.resgates()).build();
        return toResponse(service.atualizarPreferencias(auth.getName(), preferencias));
    }

    private PreferenciasResponse toResponse(PreferenciaNotificacao item) {
        return new PreferenciasResponse(item.isPushAtivo(), item.isLembreteSeteDias(), item.isLembreteUmDia(),
                item.isLembreteDuasHoras(), item.isVacinasVencendo(), item.isRetornosPendentes(),
                item.isConvitesDeAcesso(), item.isResgates());
    }
}
