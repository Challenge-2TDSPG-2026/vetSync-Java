package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.SiaConversa;
import br.com.fiap.VetSync.entity.SiaMensagem;
import br.com.fiap.VetSync.service.SiaConversaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/pets/{idPet}/sia/conversas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TUTOR') and @petAccessSecurity.canView(#idPet, authentication)")
public class SiaConversaController {
    private final SiaConversaService service;

    public record ConversaResponse(String id, Long idPet, LocalDateTime criadoEm, LocalDateTime atualizadoEm) {}
    public record MensagemRequest(@NotBlank @Pattern(regexp = "TUTOR|SIA") String autoria,
                                  @NotBlank @Size(max = 4000) String texto) {}
    public record MensagemResponse(Long id, String autoria, String texto, LocalDateTime criadoEm) {}

    private static ConversaResponse response(SiaConversa item) {
        return new ConversaResponse(item.getId(), item.getPet().getIdPet(), item.getCriadoEm(), item.getAtualizadoEm());
    }
    private static MensagemResponse response(SiaMensagem item) {
        return new MensagemResponse(item.getId(), item.getAutoria(), item.getTexto(), item.getCriadoEm());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversaResponse criar(@PathVariable Long idPet, Authentication auth) {
        return response(service.criar(idPet, auth.getName()));
    }

    @GetMapping
    public List<ConversaResponse> listar(@PathVariable Long idPet, Authentication auth) {
        return service.listar(idPet, auth.getName()).stream().map(SiaConversaController::response).toList();
    }

    @GetMapping("/{id}/mensagens")
    public List<MensagemResponse> mensagens(@PathVariable Long idPet, @PathVariable String id, Authentication auth) {
        return service.mensagens(idPet, id, auth.getName()).stream().map(SiaConversaController::response).toList();
    }

    @PostMapping("/{id}/mensagens")
    @ResponseStatus(HttpStatus.CREATED)
    public MensagemResponse adicionar(@PathVariable Long idPet, @PathVariable String id,
                                      @Valid @RequestBody MensagemRequest request, Authentication auth) {
        return response(service.adicionar(idPet, id, auth.getName(), request.autoria(), request.texto()));
    }
}
