package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.service.MensagensClinicaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/mensagens-clinica")
@PreAuthorize("hasAnyRole('TUTOR','ADMIN_CLINICA')")
@RequiredArgsConstructor
public class MensagensClinicaController {
    private final MensagensClinicaService service;
    public record IniciarRequest(Long idTutor) {}
    public record EnviarRequest(@NotBlank String texto) {}

    @GetMapping("/conversas")
    public List<MensagensClinicaService.ConversaDados> conversas(Authentication auth) {
        return service.listar(auth);
    }

    @PostMapping("/conversas")
    @ResponseStatus(HttpStatus.CREATED)
    public MensagensClinicaService.ConversaDados iniciar(Authentication auth, @RequestBody IniciarRequest req) {
        return service.iniciar(auth, req.idTutor());
    }

    @GetMapping("/conversas/{id}/mensagens")
    public List<MensagensClinicaService.MensagemDados> mensagens(Authentication auth, @PathVariable Long id) {
        return service.historico(auth, id);
    }

    @PostMapping("/conversas/{id}/mensagens")
    @ResponseStatus(HttpStatus.CREATED)
    public MensagensClinicaService.MensagemDados enviar(Authentication auth, @PathVariable Long id,
                                                         @Valid @RequestBody EnviarRequest req) {
        return service.enviar(auth, id, req.texto());
    }
}
