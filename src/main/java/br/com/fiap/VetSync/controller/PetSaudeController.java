package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
@Tag(name = "Saúde do pet", description = "Próximas ações, carteira de vacinação, perfil clínico e histórico de peso")
public class PetSaudeController {
    private final VacinaService vacinaService;
    private final PerfilSaudeService perfilService;
    private final ProximaAcaoService acaoService;

    public record AcaoResponse(String id, String tipo, String prioridade, String titulo, String descricao,
                               Long eventoReferenciaId, LocalDate dataLimite, boolean podeAgendar) {}

    public record TipoVacinaRequest(@NotBlank String nome, @NotNull @Positive Integer periodicidadeDias,
                                    @NotNull(message = "Clínica é obrigatória") Long idClinica) {}
    public record VacinaRequest(@NotNull Long tipoVacinaId, Long eventoId, @NotNull LocalDate aplicadaEm,
                                LocalDate proximaDoseEm, String comprovanteUrl) {}
    public record VacinaResponse(Long id, String nome, LocalDate aplicadaEm, LocalDate proximaDoseEm,
                                 String status, Long eventoId, String veterinario, String comprovanteUrl,
                                 Long tipoVacinaId, boolean substituida) {}
    public record ResumoVacinasResponse(int emDia, int vencendo, int atrasadas, int futuras) {}
    public record CarteiraResponse(Long petId, List<VacinaResponse> vacinas, ResumoVacinasResponse resumo) {}

    public record PerfilRequest(BigDecimal pesoAtual, String alergias, String medicamentosContinuos,
                                String restricoesAlimentares, String condicoesPreExistentes,
                                String observacoesImportantes, String contatoEmergencia,
                                Long veterinarioPreferencialId) {}
    public record PerfilResponse(BigDecimal pesoAtual, LocalDate pesoAtualizadoEm, String alergias,
                                 String medicamentosContinuos, String restricoesAlimentares,
                                 String condicoesPreExistentes, String observacoesImportantes,
                                 String contatoEmergencia, Long veterinarioPreferencialId) {}
    public record PesoRequest(@NotNull @Positive BigDecimal pesoKg, LocalDate dataMedicao, String observacao) {}
    public record PesoResponse(Long id, BigDecimal pesoKg, LocalDate dataMedicao, String observacao) {}

    @GetMapping("/{id:\\d+}/proximas-acoes")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO') or @petAccessSecurity.canView(#id, authentication)")
    public List<AcaoResponse> proximasAcoes(@PathVariable Long id) {
        return acaoService.listar(id).stream().map(a -> new AcaoResponse(a.id(), a.tipo(), a.prioridade(),
                a.titulo(), a.descricao(), a.eventoReferenciaId(), a.dataLimite(), a.podeAgendar())).toList();
    }

    @PostMapping("/tipos-vacina")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TipoVacinaResponse criarTipoVacina(@Valid @RequestBody TipoVacinaRequest request) {
        TipoVacina tipo = vacinaService.criarTipo(request.nome(), request.periodicidadeDias(), request.idClinica());
        return new TipoVacinaResponse(tipo.getIdTipoVacina(), tipo.getNmTipoVacina(), tipo.getNrPeriodicidadeDias(),
                tipo.getClinica() != null ? tipo.getClinica().getIdClinica() : null,
                tipo.getClinica() != null ? tipo.getClinica().getNmClinica() : null);
    }

    public record TipoVacinaResponse(Long id, String nome, Integer periodicidadeDias, Long idClinica, String nmClinica) {}

    @GetMapping("/{id:\\d+}/carteira-vacinacao")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO') or @petAccessSecurity.canView(#id, authentication)")
    public CarteiraResponse carteira(@PathVariable Long id) {
        List<VacinaPet> vacinas = vacinaService.listar(id);
        LocalDate hoje = LocalDate.now();
        Set<VacinaPet> substituidas = vacinaService.substituidas(vacinas, hoje);
        int emDia = 0, vencendo = 0, atrasadas = 0, futuras = 0;
        List<VacinaResponse> respostas = new java.util.ArrayList<>();
        for (VacinaPet vacina : vacinas) {
            boolean substituida = substituidas.contains(vacina);
            StatusVacina status = vacinaService.status(vacina, hoje, substituidas);
            // O resumo conta apenas doses vigentes; doses substituídas são histórico.
            if (!substituida) {
                if (status == StatusVacina.EM_DIA) emDia++;
                if (status == StatusVacina.VENCENDO) vencendo++;
                if (status == StatusVacina.ATRASADA) atrasadas++;
                if (status == StatusVacina.FUTURA) futuras++;
            }
            respostas.add(new VacinaResponse(vacina.getIdVacina(), vacina.getTipoVacina().getNmTipoVacina(),
                    vacina.getDtAplicacao(), vacina.getDtProximaDose(), status.name(),
                    vacina.getEvento() == null ? null : vacina.getEvento().getIdEvento(),
                    vacina.getEvento() == null || vacina.getEvento().getVeterinario() == null ? null
                            : vacina.getEvento().getVeterinario().getNmVeterinario(), vacina.getComprovanteUrl(),
                    vacina.getTipoVacina().getIdTipoVacina(), substituida));
        }
        return new CarteiraResponse(id, respostas, new ResumoVacinasResponse(emDia, vencendo, atrasadas, futuras));
    }

    @PostMapping("/{id:\\d+}/carteira-vacinacao")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO') or @petAccessSecurity.canEdit(#id, authentication)")
    public VacinaResponse registrarVacina(@PathVariable Long id, @Valid @RequestBody VacinaRequest request) {
        VacinaPet vacina = vacinaService.registrar(id, request.tipoVacinaId(), request.eventoId(),
                request.aplicadaEm(), request.proximaDoseEm(), request.comprovanteUrl());
        return new VacinaResponse(vacina.getIdVacina(), vacina.getTipoVacina().getNmTipoVacina(),
                vacina.getDtAplicacao(), vacina.getDtProximaDose(), vacinaService.status(vacina, LocalDate.now()).name(),
                request.eventoId(), vacina.getEvento() == null || vacina.getEvento().getVeterinario() == null ? null
                : vacina.getEvento().getVeterinario().getNmVeterinario(), vacina.getComprovanteUrl(),
                vacina.getTipoVacina().getIdTipoVacina(), false);
    }

    @GetMapping("/{id:\\d+}/perfil-saude")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO') or @petAccessSecurity.canView(#id, authentication)")
    public PerfilResponse perfil(@PathVariable Long id) {
        return toPerfilResponse(perfilService.buscar(id));
    }

    @PutMapping("/{id:\\d+}/perfil-saude")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO') or @petAccessSecurity.canEdit(#id, authentication)")
    public PerfilResponse atualizarPerfil(@PathVariable Long id, @Valid @RequestBody PerfilRequest request) {
        PerfilSaude dados = PerfilSaude.builder().pesoAtual(request.pesoAtual()).alergias(request.alergias())
                .medicamentosContinuos(request.medicamentosContinuos()).restricoesAlimentares(request.restricoesAlimentares())
                .condicoesPreExistentes(request.condicoesPreExistentes()).observacoesImportantes(request.observacoesImportantes())
                .contatoEmergencia(request.contatoEmergencia()).build();
        return toPerfilResponse(perfilService.atualizar(id, dados, request.veterinarioPreferencialId()));
    }

    @PostMapping("/{id:\\d+}/peso")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO') or @petAccessSecurity.canEdit(#id, authentication)")
    public PesoResponse registrarPeso(@PathVariable Long id, @Valid @RequestBody PesoRequest request) {
        return toPesoResponse(perfilService.registrarPeso(id, request.pesoKg(), request.dataMedicao(), request.observacao()));
    }

    @GetMapping("/{id:\\d+}/peso/historico")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO') or @petAccessSecurity.canView(#id, authentication)")
    public List<PesoResponse> historicoPeso(@PathVariable Long id) {
        return perfilService.historicoPeso(id).stream().map(this::toPesoResponse).toList();
    }

    private PerfilResponse toPerfilResponse(PerfilSaude perfil) {
        return new PerfilResponse(perfil.getPesoAtual(), perfil.getPesoAtualizadoEm(), perfil.getAlergias(),
                perfil.getMedicamentosContinuos(), perfil.getRestricoesAlimentares(), perfil.getCondicoesPreExistentes(),
                perfil.getObservacoesImportantes(), perfil.getContatoEmergencia(),
                perfil.getVeterinarioPreferencial() == null ? null : perfil.getVeterinarioPreferencial().getIdVeterinario());
    }

    private PesoResponse toPesoResponse(HistoricoPeso peso) {
        return new PesoResponse(peso.getIdPeso(), peso.getPesoKg(), peso.getDataMedicao(), peso.getObservacao());
    }
}