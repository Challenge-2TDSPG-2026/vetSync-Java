package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.Admin;
import br.com.fiap.VetSync.entity.LancamentoPontos;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.security.PerfilUtils;
import br.com.fiap.VetSync.service.PontosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/pontos")
@RequiredArgsConstructor
@Tag(name = "Pontos", description = "Lançamentos de pontos (por evento concluído ou bônus de plano de tratamento) — ficam PENDENTE até o ADMIN liberar")
public class PontosController {

    private final PontosService pontosService;
    private final AdminRepository adminRepository;

    public record LancamentoPontosResponse(
            Long idLancamento,
            String status,
            String origem,
            Integer nrPontos,
            LocalDate dtLancamento,
            Long idEvento,
            String nmTipoEvento,
            Long idPlano,
            String nmPet,
            String nmTutor,
            Long idClinica,
            String nmClinica
    ) {}

    private LancamentoPontosResponse toResponse(LancamentoPontos l) {
        var evento = l.getEvento();
        var plano = l.getPlanoTratamento();
        var pet = evento != null ? evento.getPet() : (plano != null ? plano.getPet() : null);
        String origem = evento != null ? "EVENTO" : "BONUS_PLANO";

        return new LancamentoPontosResponse(
                l.getIdLancamento(),
                l.getDsStatus().name(),
                origem,
                l.getNrPontos(),
                l.getDtLancamento(),
                evento != null ? evento.getIdEvento() : null,
                evento != null && evento.getTipoEvento() != null ? evento.getTipoEvento().getNmTipoEvento() : null,
                plano != null ? plano.getIdPlano() : null,
                pet != null ? pet.getNmPet() : null,
                pet != null && pet.getTutor() != null ? pet.getTutor().getNmTutor() : null,
                l.getClinica() != null ? l.getClinica().getIdClinica() : null,
                l.getClinica() != null ? l.getClinica().getNmClinica() : null
        );
    }

    private Long idAdminAutenticado(Authentication authentication) {
        return adminRepository.findByDsEmail(authentication.getName())
                .map(Admin::getIdAdmin)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin autenticado não encontrado"));
    }

    @GetMapping
    @Operation(summary = "Listar lançamentos de pontos", description = "Tutor vê os próprios (pendentes e liberados, de evento ou bônus de plano), cada um com a clínica em que foi gerado; admin vê a fila de pendentes, também com a clínica.")
    public List<LancamentoPontosResponse> listar(Authentication authentication) {
        List<LancamentoPontos> lancamentos = PerfilUtils.isAdmin(authentication)
                ? pontosService.listarPendentes()
                : pontosService.listarParaTutor(authentication.getName());
        return lancamentos.stream().map(this::toResponse).toList();
    }

    @PatchMapping("/{id}/liberar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin libera um lançamento de pontos pendente", description = "Só depois disso os pontos entram no saldo do tutor.")
    public LancamentoPontosResponse liberar(@PathVariable Long id, Authentication authentication) {
        return toResponse(pontosService.liberar(id, idAdminAutenticado(authentication)));
    }
}