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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * Prontuário anterior: o tutor envia consultas antigas (dados + arquivo opcional) para o veterinário consultar.
 * Fica na linha do tempo do prontuário como exame (prefixo "Prontuário anterior · ", sem veterinário). A leitura e o
 * download do arquivo usam os endpoints de exames ({@code GET /pets/{idPet}/exames/...}).
 */
@RestController
@RequestMapping("/pets/{idPet}/prontuario-anterior")
@RequiredArgsConstructor
@Tag(name = "Prontuário - Anterior", description = "Consultas antigas enviadas pelo tutor")
public class ProntuarioAnteriorController {

    private final ProntuarioRegistroService registroService;
    private final ProntuarioService prontuarioService;

    public record ProntuarioAnteriorRequest(
            @NotBlank(message = "nome é obrigatório") @Size(max = 120, message = "nome deve ter no máximo 120 caracteres") String nome,
            @Size(max = 120, message = "laboratorio deve ter no máximo 120 caracteres") String laboratorio,
            @NotNull(message = "resultadoEm é obrigatório") LocalDate resultadoEm,
            @Size(max = 2000, message = "resultado deve ter no máximo 2000 caracteres") String resultado,
            @Size(max = 1000, message = "interpretacao deve ter no máximo 1000 caracteres") String interpretacao
    ) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TUTOR') and @prontuarioSecurity.canEnviarAnterior(#idPet, authentication)")
    @Operation(summary = "Tutor envia uma consulta antiga (prontuário anterior)",
            description = "O arquivo (PDF/JPEG/PNG/WebP, até 10 MB) é enviado depois, em POST /pets/{idPet}/prontuario-anterior/{id}/arquivo.")
    public ProntuarioService.Exame enviar(@PathVariable Long idPet, @Valid @RequestBody ProntuarioAnteriorRequest request,
                                          Authentication authentication) {
        ResultadoExame exame = registroService.registrarProntuarioAnterior(idPet,
                new ProntuarioRegistroService.DadosExame(null, request.nome(), request.laboratorio(), null,
                        request.resultadoEm(), request.resultado(), request.interpretacao()),
                authentication.getName());
        return prontuarioService.toExame(exame);
    }

    @PostMapping(value = "/{idExame}/arquivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TUTOR') and @prontuarioSecurity.canEnviarAnterior(#idPet, authentication)")
    @Operation(summary = "Anexar (ou substituir) o arquivo de um prontuário anterior enviado por você")
    public ProntuarioService.Exame anexar(@PathVariable Long idPet, @PathVariable Long idExame,
                                          @RequestPart("arquivo") MultipartFile arquivo, Authentication authentication) {
        return prontuarioService.toExame(registroService.anexarArquivoProntuarioAnterior(
                idPet, idExame, arquivo, authentication.getName()));
    }

    @DeleteMapping("/{idExame}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TUTOR') and @prontuarioSecurity.canEnviarAnterior(#idPet, authentication)")
    @Operation(summary = "Remover um prontuário anterior enviado por você (a remoção fica na auditoria)")
    public void remover(@PathVariable Long idPet, @PathVariable Long idExame, Authentication authentication) {
        registroService.removerProntuarioAnterior(idPet, idExame, authentication.getName());
    }
}
