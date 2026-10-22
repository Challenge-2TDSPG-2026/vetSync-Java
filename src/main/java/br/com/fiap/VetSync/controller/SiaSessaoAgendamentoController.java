package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.SiaSessaoAgendamento;
import br.com.fiap.VetSync.repository.SiaSessaoAgendamentoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/sia/agendamentos/sessoes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TUTOR')")
public class SiaSessaoAgendamentoController {
    private final SiaSessaoAgendamentoRepository repository;

    public record SessaoRequest(@NotBlank @Pattern(regexp = "[a-f0-9-]{36}") String id,
                                @NotBlank @Size(max = 250000) String dados, LocalDateTime expiraEm) {}
    public record SessaoResponse(String id, String dados, LocalDateTime expiraEm) {}

    @PutMapping("/{id}")
    @Transactional
    public SessaoResponse salvar(@PathVariable String id, @Valid @RequestBody SessaoRequest request, Authentication auth) {
        if (!id.equals(request.id()) || request.expiraEm() == null
                || !request.expiraEm().isAfter(LocalDateTime.now(ZoneOffset.UTC))
                || request.expiraEm().isAfter(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(16))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sessão inválida");
        }
        SiaSessaoAgendamento item = repository.findById(id).orElseGet(SiaSessaoAgendamento::new);
        if (item.getTutorEmail() != null && !item.getTutorEmail().equalsIgnoreCase(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        item.setId(id);
        item.setTutorEmail(auth.getName());
        item.setDados(request.dados());
        item.setExpiraEm(request.expiraEm());
        repository.save(item);
        return new SessaoResponse(id, item.getDados(), item.getExpiraEm());
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public SessaoResponse buscar(@PathVariable String id, Authentication auth) {
        SiaSessaoAgendamento item = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE));
        if (!item.getTutorEmail().equalsIgnoreCase(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (!item.getExpiraEm().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new ResponseStatusException(HttpStatus.GONE);
        }
        return new SessaoResponse(id, item.getDados(), item.getExpiraEm());
    }
}
