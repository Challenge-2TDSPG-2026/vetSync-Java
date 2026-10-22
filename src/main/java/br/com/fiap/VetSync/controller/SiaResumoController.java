package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.SiaResumo;
import br.com.fiap.VetSync.service.SiaResumoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class SiaResumoController {
    private final SiaResumoService service;

    public record ResumoRequest(@NotBlank @Size(max = 8000) String resumo,
                                @Size(max = 4000) String perguntas, Long idEvento) {}
    public record ResumoResponse(Long id, Long idPet, Long idEvento, String resumo, String perguntas,
                                 String origem, LocalDateTime criadoEm, LocalDateTime compartilhadoEm,
                                 LocalDateTime revogadoEm) {}

    private static ResumoResponse toResponse(SiaResumo item) {
        return new ResumoResponse(item.getId(), item.getPet().getIdPet(),
                item.getEvento() == null ? null : item.getEvento().getIdEvento(),
                item.getResumo(), item.getPerguntas(), item.getOrigem(), item.getCriadoEm(),
                item.getCompartilhadoEm(), item.getRevogadoEm());
    }

    @PostMapping("/pets/{idPet}/sia/resumos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TUTOR') and @petAccessSecurity.canEdit(#idPet, authentication)")
    public ResumoResponse criar(@PathVariable Long idPet, @Valid @RequestBody ResumoRequest request, Authentication authentication) {
        return toResponse(service.criar(idPet, authentication.getName(), request.resumo(), request.perguntas(), request.idEvento()));
    }

    @GetMapping("/pets/{idPet}/sia/resumos")
    @PreAuthorize("hasRole('TUTOR') and @petAccessSecurity.canView(#idPet, authentication)")
    public List<ResumoResponse> listarTutor(@PathVariable Long idPet, Authentication authentication) {
        return service.listarTutor(idPet, authentication.getName()).stream().map(SiaResumoController::toResponse).toList();
    }

    @PutMapping("/pets/{idPet}/sia/resumos/{id}")
    @PreAuthorize("hasRole('TUTOR') and @petAccessSecurity.canEdit(#idPet, authentication)")
    public ResumoResponse atualizar(@PathVariable Long idPet, @PathVariable Long id,
                                    @Valid @RequestBody ResumoRequest request, Authentication authentication) {
        return toResponse(service.atualizar(idPet, id, authentication.getName(), request.resumo(), request.perguntas(), request.idEvento()));
    }

    @PostMapping("/pets/{idPet}/sia/resumos/{id}/compartilhar")
    @PreAuthorize("hasRole('TUTOR') and @petAccessSecurity.canEdit(#idPet, authentication)")
    public ResumoResponse compartilhar(@PathVariable Long idPet, @PathVariable Long id, Authentication authentication) {
        return toResponse(service.compartilhar(idPet, id, authentication.getName()));
    }

    @DeleteMapping("/pets/{idPet}/sia/resumos/{id}/compartilhamento")
    @PreAuthorize("hasRole('TUTOR') and @petAccessSecurity.canEdit(#idPet, authentication)")
    public ResumoResponse revogar(@PathVariable Long idPet, @PathVariable Long id, Authentication authentication) {
        return toResponse(service.revogar(idPet, id, authentication.getName()));
    }

    @GetMapping("/eventos/{idEvento}/sia/resumos")
    @PreAuthorize("hasRole('VETERINARIO') and @eventoSecurity.isVeterinarioResponsavel(#idEvento, authentication)")
    public List<ResumoResponse> listarVet(@PathVariable Long idEvento) {
        return service.listarVet(idEvento).stream().map(SiaResumoController::toResponse).toList();
    }
}
