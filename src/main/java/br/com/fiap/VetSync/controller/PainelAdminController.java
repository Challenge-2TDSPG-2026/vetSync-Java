package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.service.PainelAdminService;
import br.com.fiap.VetSync.service.PainelAdminService.ResumoPainel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/painel")
@RequiredArgsConstructor
@Tag(name = "Painel do Admin", description = "Indicadores agregados do painel inicial do admin")
public class PainelAdminController {

    private final PainelAdminService painelAdminService;

    @GetMapping("/resumo")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Indicadores de pontos e recompensas do painel inicial. Somente ADMIN.",
            description = "Com ?idClinica= devolve só os números daquela clínica (escopo CLINICA). "
                    + "Sem o filtro devolve os totais de TODAS as clínicas (escopo GLOBAL, totaisGlobais=true, "
                    + "rotuloEscopo='Totais de todas as clínicas') e o detalhamento de pontos em porClinica. "
                    + "pontosDisponiveis é o saldo resgatável: não inclui pontos pendentes, bloqueados, vencidos "
                    + "nem reservados por resgates pendentes.")
    public ResumoPainel resumo(@RequestParam(value = "idClinica", required = false) Long idClinica) {
        return painelAdminService.resumo(idClinica);
    }
}