package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.BloqueioAgendaEstetica;
import br.com.fiap.VetSync.entity.DisponibilidadeEstetica;
import br.com.fiap.VetSync.entity.ProfissionalEstetica;
import br.com.fiap.VetSync.service.AgendaEsteticaService;
import br.com.fiap.VetSync.service.ProfissionalEsteticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/profissionais-estetica")
@RequiredArgsConstructor
@Tag(name = "Profissionais de Estética", description = "Cadastro (só ADMIN), perfil e agenda")
public class ProfissionalEsteticaController {

    private final ProfissionalEsteticaService profissionalEsteticaService;
    private final AgendaEsteticaService agendaEsteticaService;



    public record ProfissionalEsteticaRequest(
            @NotBlank(message = "Nome é obrigatório")
            String nome,

            @NotBlank(message = "E-mail é obrigatório")
            @Email(message = "E-mail inválido")
            String email,

            @NotNull(message = "Clínica é obrigatória")
            Long idClinica
    ) {}

    public record ProfissionalEsteticaAtualizarRequest(
            @NotBlank(message = "Nome é obrigatório")
            String nome,

            @NotNull(message = "Clínica é obrigatória")
            Long idClinica
    ) {}

    public record ProfissionalEsteticaResponse(Long idProfissionalEstetica, String nmProfissionalEstetica, String nrRegistro, String dsEmail, Long idClinica, String nmClinica) {}
    public record CadastroResponse(Long idProfissionalEstetica, String email, String nome, String registro, String senhaTemporaria) {}

    private ProfissionalEsteticaResponse toResponse(ProfissionalEstetica p) {
        return new ProfissionalEsteticaResponse(
                p.getIdProfissionalEstetica(), p.getNmProfissionalEstetica(), p.getNrRegistro(), p.getDsEmail(),
                p.getClinica() != null ? p.getClinica().getIdClinica() : null,
                p.getClinica() != null ? p.getClinica().getNmClinica() : null
        );
    }

    @GetMapping
    @Operation(summary = "Listar profissionais de estética (para o tutor escolher um)")
    public List<ProfissionalEsteticaResponse> listar() {
        return profissionalEsteticaService.listarTodos().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar profissional de estética por ID")
    public ProfissionalEsteticaResponse buscarPorId(@PathVariable Long id) {
        return toResponse(profissionalEsteticaService.buscarPorId(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastrar profissional de estética. Somente ADMIN.",
            description = "Gera registro e senha temporária automaticamente, enviados por e-mail ao profissional.")
    public CadastroResponse cadastrar(@Valid @RequestBody ProfissionalEsteticaRequest request) {
        var novo = profissionalEsteticaService.cadastrar(request.nome(), request.email(), request.idClinica());
        return new CadastroResponse(
                novo.profissional().getIdProfissionalEstetica(), novo.profissional().getDsEmail(),
                novo.profissional().getNmProfissionalEstetica(), novo.profissional().getNrRegistro(), novo.senhaTemporaria()
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("@profissionalEsteticaSecurity.isSelf(#id, authentication)")
    @Operation(summary = "Atualizar dados do profissional de estética. Só o próprio.")
    public ProfissionalEsteticaResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProfissionalEsteticaAtualizarRequest request) {
        return toResponse(profissionalEsteticaService.atualizar(id, request.nome(), request.idClinica()));
    }



    public record DisponibilidadeRequest(
            @NotNull(message = "Dia da semana é obrigatório")
            @Min(value = 1, message = "Dia da semana deve ser entre 1 (segunda) e 7 (domingo)")
            @Max(value = 7, message = "Dia da semana deve ser entre 1 (segunda) e 7 (domingo)")
            Integer nrDiaSemana,

            @NotBlank(message = "Horário de início é obrigatório")
            @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Horário deve estar no formato HH:mm")
            String hrInicio,

            @NotBlank(message = "Horário de fim é obrigatório")
            @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Horário deve estar no formato HH:mm")
            String hrFim
    ) {}
    public record DisponibilidadeResponse(Long idDisponibilidade, Integer nrDiaSemana, String hrInicio, String hrFim) {}

    private DisponibilidadeResponse toResponse(DisponibilidadeEstetica d) {
        return new DisponibilidadeResponse(d.getIdDisponibilidade(), d.getNrDiaSemana(), d.getHrInicio(), d.getHrFim());
    }

    @GetMapping("/{id}/disponibilidade")
    @Operation(summary = "Listar horários fixos de atendimento do profissional")
    public List<DisponibilidadeResponse> listarDisponibilidade(@PathVariable Long id) {
        return agendaEsteticaService.listarDisponibilidade(id).stream().map(this::toResponse).toList();
    }

    @PostMapping("/{id}/disponibilidade")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@profissionalEsteticaSecurity.isSelf(#id, authentication)")
    @Operation(summary = "Adicionar horário fixo de atendimento. nrDiaSemana: 1=segunda...7=domingo. Horários no formato HH:mm.")
    public DisponibilidadeResponse adicionarDisponibilidade(@PathVariable Long id, @Valid @RequestBody DisponibilidadeRequest request) {
        return toResponse(agendaEsteticaService.adicionarDisponibilidade(id, request.nrDiaSemana(), request.hrInicio(), request.hrFim()));
    }

    @DeleteMapping("/{id}/disponibilidade/{idDisponibilidade}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@profissionalEsteticaSecurity.isSelf(#id, authentication)")
    @Operation(summary = "Remover horário fixo de atendimento")
    public void removerDisponibilidade(@PathVariable Long id, @PathVariable Long idDisponibilidade) {
        agendaEsteticaService.removerDisponibilidade(id, idDisponibilidade);
    }



    public record BloqueioRequest(
            @NotNull(message = "Data de início é obrigatória")
            LocalDate dtInicio,

            @NotNull(message = "Data de fim é obrigatória")
            LocalDate dtFim,

            String motivo
    ) {}
    public record BloqueioResponse(Long idBloqueio, LocalDate dtInicio, LocalDate dtFim, String motivo) {}

    private BloqueioResponse toResponse(BloqueioAgendaEstetica b) {
        return new BloqueioResponse(b.getIdBloqueio(), b.getDtInicio(), b.getDtFim(), b.getDsMotivo());
    }

    @GetMapping("/{id}/bloqueios")
    @Operation(summary = "Listar bloqueios de agenda (férias, compromissos, etc.)")
    public List<BloqueioResponse> listarBloqueios(@PathVariable Long id) {
        return agendaEsteticaService.listarBloqueios(id).stream().map(this::toResponse).toList();
    }

    @PostMapping("/{id}/bloqueios")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@profissionalEsteticaSecurity.isSelf(#id, authentication)")
    @Operation(summary = "Adicionar bloqueio de agenda (um dia ou um período)")
    public BloqueioResponse adicionarBloqueio(@PathVariable Long id, @Valid @RequestBody BloqueioRequest request) {
        return toResponse(agendaEsteticaService.adicionarBloqueio(id, request.dtInicio(), request.dtFim(), request.motivo()));
    }

    @DeleteMapping("/{id}/bloqueios/{idBloqueio}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@profissionalEsteticaSecurity.isSelf(#id, authentication)")
    @Operation(summary = "Remover bloqueio de agenda")
    public void removerBloqueio(@PathVariable Long id, @PathVariable Long idBloqueio) {
        agendaEsteticaService.removerBloqueio(id, idBloqueio);
    }
}