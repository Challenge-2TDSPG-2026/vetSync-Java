package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.Admin;
import br.com.fiap.VetSync.entity.RelatorioEstetica;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.security.PerfilUtils;
import br.com.fiap.VetSync.service.ProfissionalEsteticaService;
import br.com.fiap.VetSync.service.RelatorioEsteticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/relatorios-estetica")
@RequiredArgsConstructor
@Tag(name = "Relatórios de Estética", description = "Profissional de estética relata um problema visto durante o banho/tosa; fica pendente até o ADMIN liberar ou negar")
public class RelatorioEsteticaController {

    private final RelatorioEsteticaService relatorioEsteticaService;
    private final ProfissionalEsteticaService profissionalEsteticaService;
    private final AdminRepository adminRepository;

    public record RelatorioRequest(
            @NotNull(message = "idEvento é obrigatório") Long idEvento,
            @NotBlank(message = "dsProblema é obrigatório") String dsProblema
    ) {}

    public record RelatorioLiberarRequest(boolean aprovado) {}

    public record RelatorioResponse(
            Long idRelatorio,
            String status,
            Long idEvento,
            String dsProblema,
            String nmPet,
            String nmTutor,
            String nmProfissionalEstetica
    ) {}

    private RelatorioResponse toResponse(RelatorioEstetica r) {
        var evento = r.getEvento();
        var pet = evento != null ? evento.getPet() : null;
        return new RelatorioResponse(
                r.getIdRelatorio(),
                r.getDsStatus().name(),
                evento != null ? evento.getIdEvento() : null,
                r.getDsProblema(),
                pet != null ? pet.getNmPet() : null,
                pet != null && pet.getTutor() != null ? pet.getTutor().getNmTutor() : null,
                r.getProfissionalEstetica() != null ? r.getProfissionalEstetica().getNmProfissionalEstetica() : null
        );
    }

    private Long idAdminAutenticado(Authentication authentication) {
        return adminRepository.findByDsEmail(authentication.getName())
                .map(Admin::getIdAdmin)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin autenticado não encontrado"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROFISSIONAL_ESTETICA')")
    @Operation(summary = "Profissional de estética relata um problema encontrado no pet durante o banho/tosa",
            description = "Cria o relatório com status SOLICITADO, aguardando liberação do ADMIN.")
    public RelatorioResponse solicitar(Authentication authentication, @Valid @RequestBody RelatorioRequest request) {
        Long idProfissional = profissionalEsteticaService.buscarAutenticado(authentication).getIdProfissionalEstetica();
        RelatorioEstetica relatorio = relatorioEsteticaService.solicitar(request.idEvento(), request.dsProblema(), idProfissional);
        return toResponse(relatorio);
    }

    @GetMapping
    @Operation(summary = "Listar relatórios", description = "Tutor vê os dos próprios pets; profissional de estética vê os que ele relatou; admin vê a fila de pendentes (SOLICITADO).")
    public List<RelatorioResponse> listar(Authentication authentication) {
        List<RelatorioEstetica> relatorios;
        if (PerfilUtils.isAdmin(authentication)) {
            relatorios = relatorioEsteticaService.listarPendentes();
        } else if (PerfilUtils.isProfissionalEstetica(authentication)) {
            relatorios = relatorioEsteticaService.listarPorProfissional(authentication.getName());
        } else {
            relatorios = relatorioEsteticaService.listarPorTutor(authentication.getName());
        }
        return relatorios.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar relatório por ID", description = "Tutor dono do pet, profissional responsável pelo evento ou qualquer admin.")
    public RelatorioResponse buscarPorId(@PathVariable Long id, Authentication authentication) {
        RelatorioEstetica relatorio = relatorioEsteticaService.buscarPorId(id);
        var evento = relatorio.getEvento();
        boolean ehProfissional = evento != null && evento.getProfissionalEstetica() != null
                && evento.getProfissionalEstetica().getDsEmail().equalsIgnoreCase(authentication.getName());
        boolean ehTutor = evento != null && evento.getPet() != null && evento.getPet().getTutor() != null
                && evento.getPet().getTutor().getDsEmail().equalsIgnoreCase(authentication.getName());
        if (!PerfilUtils.isAdmin(authentication) && !ehProfissional && !ehTutor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem acesso a esse relatório");
        }
        return toResponse(relatorio);
    }

    @PatchMapping("/{id}/liberar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin libera ou nega um relatório pendente",
            description = "Se aprovado=true, gera um link de agendamento e envia e-mail ao tutor com a observação e o link para escolher data/veterinário.")
    public RelatorioResponse liberar(@PathVariable Long id, Authentication authentication,
                                     @RequestBody RelatorioLiberarRequest request) {
        RelatorioEstetica relatorio = relatorioEsteticaService.liberar(id, idAdminAutenticado(authentication), request.aprovado());
        return toResponse(relatorio);
    }
}