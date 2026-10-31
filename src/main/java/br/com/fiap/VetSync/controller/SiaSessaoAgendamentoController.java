package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.SiaSessaoAgendamento;
import br.com.fiap.VetSync.service.SiaSessaoAgendamentoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/sia/agendamentos/sessoes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TUTOR')")
public class SiaSessaoAgendamentoController {
    private final SiaSessaoAgendamentoService service;

    public record SessaoRequest(@NotBlank @Pattern(regexp = "[a-f0-9-]{36}") String id,
                                @NotBlank @Size(max = 250000) String dados, LocalDateTime expiraEm) {}
    public record SessaoResponse(String id, String dados, LocalDateTime expiraEm) {}

    @PutMapping("/{id}")
    public SessaoResponse salvar(@PathVariable String id, @Valid @RequestBody SessaoRequest request, Authentication auth) {
        SiaSessaoAgendamento item = service.salvar(id, request.id(), request.dados(), request.expiraEm(), auth.getName());
        return new SessaoResponse(id, item.getDados(), item.getExpiraEm());
    }

    @GetMapping("/{id}")
    public SessaoResponse buscar(@PathVariable String id, Authentication auth) {
        SiaSessaoAgendamento item = service.buscar(id, auth.getName());
        return new SessaoResponse(id, item.getDados(), item.getExpiraEm());
    }
}
