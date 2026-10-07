package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.security.PerfilUtils;
import br.com.fiap.VetSync.service.AuditoriaService;
import br.com.fiap.VetSync.service.AuditoriaTipos;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {
    private final AuditoriaService auditoriaService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar auditoria",
            description = "ADMIN: todos os filtros são opcionais (idClinica, de, ate, acao, entidade, entidadeId, limite). "
                    + "Demais perfis: apenas entidade (EVENTO ou ACESSO_PET) + entidadeId, como antes.")
    public List<AuditoriaResponse> listar(Authentication authentication,
                                          @RequestParam(required = false) String entidade,
                                          @RequestParam(required = false) Long entidadeId,
                                          @RequestParam(required = false) Long idClinica,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                                          @RequestParam(required = false) String acao,
                                          @RequestParam(required = false) Integer limite) {
        if (de != null && ate != null && de.isAfter(ate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'de' não pode ser depois de 'ate'");
        }
        if (!PerfilUtils.isAdmin(authentication)) {
            // Compatibilidade: sem perfil ADMIN só vale a consulta antiga, por entidade+id, dos tipos que já existiam.
            if (entidade == null || entidadeId == null
                    || !AuditoriaTipos.ENTIDADES_LEGADO.contains(entidade.trim().toUpperCase())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Consulta de auditoria restrita ao administrador");
            }
            return auditoriaService.listar(entidade.trim().toUpperCase(), entidadeId).stream().map(this::toResponse).toList();
        }
        return auditoriaService.buscar(idClinica, de, ate, acao, entidade, entidadeId, limite).stream()
                .map(this::toResponse).toList();
    }

    @GetMapping("/tipos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Entidades e ações auditáveis (para montar os filtros da tela)")
    public TiposResponse tipos() {
        return new TiposResponse(AuditoriaTipos.ENTIDADES, AuditoriaTipos.ACOES);
    }

    private AuditoriaResponse toResponse(br.com.fiap.VetSync.entity.Auditoria a) {
        return new AuditoriaResponse(a.getIdAuditoria(), a.getDsEntidade(), a.getIdEntidade(), a.getDsAcao(),
                a.getDsAtor(), a.getDsPerfil(), a.getIdClinica(), a.getNmClinica(), a.getDsValorAnterior(),
                a.getDsValorNovo(), a.getDsIp(), a.getDtOcorrencia());
    }

    public record TiposResponse(List<String> entidades, List<String> acoes) {}

    public record AuditoriaResponse(Long id, String entidade, Long entidadeId, String acao, String ator,
                                    String perfil, Long clinicaId, String clinicaNome, String valorAnterior,
                                    String valorNovo, String ip, LocalDateTime ocorridoEm) {}
}