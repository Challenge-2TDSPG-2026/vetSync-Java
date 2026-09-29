package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {
    private final AuditoriaService auditoriaService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<AuditoriaResponse> listar(@RequestParam String entidade, @RequestParam Long entidadeId) {
        return auditoriaService.listar(entidade.toUpperCase(), entidadeId).stream()
                .map(a -> new AuditoriaResponse(a.getIdAuditoria(), a.getDsEntidade(), a.getIdEntidade(),
                        a.getDsAcao(), a.getDsAtor(), a.getDsPerfil(), a.getDsValorAnterior(),
                        a.getDsValorNovo(), a.getDsIp(), a.getDtOcorrencia())).toList();
    }

    public record AuditoriaResponse(Long id, String entidade, Long entidadeId, String acao, String ator,
                                    String perfil, String valorAnterior, String valorNovo, String ip,
                                    LocalDateTime ocorridoEm) {}
}
