package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.ProntuarioCompartilhado;
import br.com.fiap.VetSync.entity.SecaoProntuario;
import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.service.AuditoriaService;
import br.com.fiap.VetSync.service.AuditoriaTipos;
import br.com.fiap.VetSync.service.ProntuarioCompartilhamentoService;
import br.com.fiap.VetSync.service.ProntuarioService;
import br.com.fiap.VetSync.service.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/pets/{idPet}/prontuario")
@RequiredArgsConstructor
@Tag(name = "Prontuário clínico",
        description = "Histórico completo do pet (atendimentos, orientações, receitas e exames) com exportação e compartilhamento")
public class ProntuarioController {

    private final ProntuarioService prontuarioService;
    private final ProntuarioCompartilhamentoService compartilhamentoService;
    private final TutorService tutorService;
    private final AuditoriaService auditoriaService;
    private final SpringTemplateEngine templateEngine;

    public enum FormatoExportacao { JSON, HTML }

    public record CompartilharRequest(
            Set<SecaoProntuario> secoes,
            @Size(max = 150, message = "destinatario deve ter no máximo 150 caracteres") String destinatario,
            @Min(value = 1, message = "validadeDias deve ser no mínimo 1")
            @Max(value = 30, message = "validadeDias deve ser no máximo 30") Integer validadeDias,
            LocalDate periodoInicio,
            LocalDate periodoFim
    ) {}

    public record CompartilhamentoCriadoResponse(Long id, String urlPublica, LocalDateTime expiraEm,
                                                 Set<SecaoProntuario> secoes) {}

    public record CompartilhamentoResponse(Long id, String destinatario, Set<SecaoProntuario> secoes,
                                           LocalDate periodoInicio, LocalDate periodoFim, LocalDateTime criadaEm,
                                           LocalDateTime expiraEm, LocalDateTime revogadaEm,
                                           LocalDateTime ultimoAcessoEm, int totalAcessos, boolean ativo) {}

    // ------------------------------------------------------------------ consulta

    @GetMapping
    @PreAuthorize("@prontuarioSecurity.canView(#idPet, authentication)")
    @Operation(summary = "Prontuário completo do pet",
            description = "Reúne atendimentos concluídos, orientações, receitas e exames numa só resposta, com linha do tempo. "
                    + "Filtros opcionais: secoes (PERFIL_SAUDE, ATENDIMENTOS, ORIENTACOES, RECEITAS, EXAMES — separadas por vírgula) e "
                    + "período (de/ate, inclusivos). Não inclui custos, observações do tutor nem dados pessoais do tutor.")
    public ProntuarioService.Prontuario consultar(@PathVariable Long idPet,
                                                  @RequestParam(required = false) Set<SecaoProntuario> secoes,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return prontuarioService.montar(idPet, secoes, de, ate, false);
    }

    // ----------------------------------------------------------------- exportação

    @GetMapping("/exportar")
    @PreAuthorize("@prontuarioSecurity.canView(#idPet, authentication)")
    @Operation(summary = "Exportar o prontuário como arquivo (JSON ou HTML imprimível)",
            description = "Apenas receitas já liberadas pela clínica entram na exportação. "
                    + "O HTML pode ser aberto no navegador e impresso/salvo como PDF. A exportação fica registrada na auditoria.")
    public ResponseEntity<?> exportar(@PathVariable Long idPet,
                                      @RequestParam(defaultValue = "JSON") FormatoExportacao formato,
                                      @RequestParam(required = false) Set<SecaoProntuario> secoes,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        ProntuarioService.Prontuario prontuario = prontuarioService.montar(idPet, secoes, de, ate, true);
        auditoriaService.registrarAcao(AuditoriaTipos.PRONTUARIO, idPet, "PRONTUARIO_EXPORTADO", null, null,
                "formato=" + formato + "; secoes=" + ProntuarioCompartilhado.serializarSecoes(prontuario.secoes()),
                "SISTEMA", "SISTEMA");

        String base = "prontuario-" + prontuario.pet().numero() + "-"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        if (formato == FormatoExportacao.HTML) {
            Context contexto = new Context(new Locale("pt", "BR"));
            contexto.setVariables(ProntuarioView.variaveis(prontuario, null));
            String html = templateEngine.process("prontuario", contexto);
            return ResponseEntity.ok()
                    .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                    .header("Content-Disposition", "attachment; filename=\"" + base + ".html\"")
                    .header("Cache-Control", "no-store")
                    .body(html);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Disposition", "attachment; filename=\"" + base + ".json\"")
                .header("Cache-Control", "no-store")
                .body(prontuario);
    }

    // ------------------------------------------------------------- compartilhamento

    @PostMapping("/compartilhamentos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TUTOR') and @prontuarioSecurity.isProprietario(#idPet, authentication)")
    @Operation(summary = "Gerar um link temporário e somente leitura do prontuário para outro atendimento",
            description = "O tutor escolhe as seções e, opcionalmente, o período e a validade (1 a 30 dias, padrão 7). "
                    + "O link só é exibido nesta resposta. No máximo 5 links ativos por pet.")
    public CompartilhamentoCriadoResponse compartilhar(@PathVariable Long idPet,
                                                       @Valid @RequestBody(required = false) CompartilharRequest request,
                                                       Authentication authentication, HttpServletRequest http) {
        CompartilharRequest dados = request != null ? request
                : new CompartilharRequest(null, null, null, null, null);
        Tutor tutor = tutorService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor autenticado não encontrado"));
        var criado = compartilhamentoService.criar(idPet, tutor.getIdTutor(), dados.secoes(), dados.destinatario(),
                dados.validadeDias(), dados.periodoInicio(), dados.periodoFim());
        String url = UriComponentsBuilder.newInstance()
                .scheme(http.getScheme()).host(http.getServerName()).port(http.getServerPort())
                .path("/prontuarios-publicos/{token}").buildAndExpand(criado.tokenPuro()).toUriString();
        return new CompartilhamentoCriadoResponse(criado.compartilhamento().getIdCompartilhamento(), url,
                criado.compartilhamento().getExpiraEm(), criado.compartilhamento().secoes());
    }

    @GetMapping("/compartilhamentos")
    @PreAuthorize("hasRole('TUTOR') and @prontuarioSecurity.isProprietario(#idPet, authentication)")
    @Operation(summary = "Listar os links de compartilhamento do pet, com quantidade de acessos e último acesso",
            description = "Não devolve o link/token (só é exibido na criação).")
    public List<CompartilhamentoResponse> listar(@PathVariable Long idPet) {
        return compartilhamentoService.listar(idPet).stream().map(ProntuarioController::toResponse).toList();
    }

    @DeleteMapping("/compartilhamentos/{idCompartilhamento}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TUTOR') and @prontuarioSecurity.isProprietario(#idPet, authentication)")
    @Operation(summary = "Revogar um link de compartilhamento imediatamente")
    public void revogar(@PathVariable Long idPet, @PathVariable Long idCompartilhamento) {
        compartilhamentoService.revogar(idPet, idCompartilhamento);
    }

    private static CompartilhamentoResponse toResponse(ProntuarioCompartilhado c) {
        return new CompartilhamentoResponse(c.getIdCompartilhamento(), c.getDsDestinatario(), c.secoes(),
                c.getDtPeriodoInicio(), c.getDtPeriodoFim(), c.getCriadaEm(), c.getExpiraEm(), c.getRevogadaEm(),
                c.getUltimoAcessoEm(), c.getNrAcessos() == null ? 0 : c.getNrAcessos(), c.isAtivo());
    }
}