package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.ResultadoExame;
import br.com.fiap.VetSync.service.ProntuarioRegistroService;
import br.com.fiap.VetSync.service.ProntuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/pets/{idPet}/exames")
@RequiredArgsConstructor
@Tag(name = "Prontuário - Exames", description = "Resultados de exames do pet (laudo em texto + arquivo opcional)")
public class ProntuarioExameController {

    private final ProntuarioRegistroService registroService;
    private final ProntuarioService prontuarioService;

    public record ExameRequest(
            Long idEvento,
            @NotBlank(message = "nome é obrigatório") @Size(max = 120, message = "nome deve ter no máximo 120 caracteres") String nome,
            @Size(max = 120, message = "laboratorio deve ter no máximo 120 caracteres") String laboratorio,
            LocalDate coletadoEm,
            @NotNull(message = "resultadoEm é obrigatório") LocalDate resultadoEm,
            @Size(max = 2000, message = "resultado deve ter no máximo 2000 caracteres") String resultado,
            @Size(max = 1000, message = "interpretacao deve ter no máximo 1000 caracteres") String interpretacao
    ) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VETERINARIO') and @prontuarioSecurity.canRegistrar(#idPet, authentication)")
    @Operation(summary = "Veterinário registra o resultado de um exame do pet",
            description = "O arquivo do laudo (PDF/JPEG/PNG/WebP, até 10 MB) é enviado depois, em POST /pets/{idPet}/exames/{idExame}/arquivo.")
    public ProntuarioService.Exame registrar(@PathVariable Long idPet, @Valid @RequestBody ExameRequest request,
                                             Authentication authentication) {
        ResultadoExame exame = registroService.registrarExame(idPet,
                new ProntuarioRegistroService.DadosExame(request.idEvento(), request.nome(), request.laboratorio(),
                        request.coletadoEm(), request.resultadoEm(), request.resultado(), request.interpretacao()),
                authentication.getName());
        return prontuarioService.toExame(exame);
    }

    @PostMapping(value = "/{idExame}/arquivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('VETERINARIO') and @prontuarioSecurity.canRegistrar(#idPet, authentication)")
    @Operation(summary = "Anexar (ou substituir) o arquivo do laudo de um exame")
    public ProntuarioService.Exame anexar(@PathVariable Long idPet, @PathVariable Long idExame,
                                          @RequestPart("arquivo") MultipartFile arquivo, Authentication authentication) {
        return prontuarioService.toExame(registroService.anexarArquivo(idPet, idExame, arquivo, authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("@prontuarioSecurity.canView(#idPet, authentication)")
    @Operation(summary = "Listar os resultados de exames do pet")
    public List<ProntuarioService.Exame> listar(@PathVariable Long idPet) {
        return registroService.listarExames(idPet).stream().map(prontuarioService::toExame).toList();
    }

    @GetMapping("/{idExame}/arquivo")
    @PreAuthorize("@prontuarioSecurity.canView(#idPet, authentication)")
    @Operation(summary = "Baixar o arquivo do laudo")
    public ResponseEntity<byte[]> baixar(@PathVariable Long idPet, @PathVariable Long idExame) {
        ResultadoExame exame = registroService.buscarExameComArquivo(idPet, idExame);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(exame.getDsMimeType()))
                .header("Content-Disposition", "attachment; filename=\"" + exame.getNmArquivo() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "no-store")
                .body(exame.getDsConteudo());
    }

    @DeleteMapping("/{idExame}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('VETERINARIO') and @prontuarioSecurity.canRegistrar(#idPet, authentication)")
    @Operation(summary = "Remover um exame (só quem o registrou; a remoção fica na auditoria)")
    public void remover(@PathVariable Long idPet, @PathVariable Long idExame, Authentication authentication) {
        registroService.removerExame(idPet, idExame, authentication.getName());
    }
}