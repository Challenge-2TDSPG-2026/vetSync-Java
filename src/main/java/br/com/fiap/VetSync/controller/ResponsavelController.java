package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.PermissaoPet;
import br.com.fiap.VetSync.entity.PetConvite;
import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.entity.TutorResponsavel;
import br.com.fiap.VetSync.service.PetConviteService;
import br.com.fiap.VetSync.service.ResponsavelService;
import br.com.fiap.VetSync.service.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Responsáveis do tutor", description = "Responsáveis com acesso a todos os pets do tutor")
public class ResponsavelController {

    private final PetConviteService petConviteService;
    private final ResponsavelService responsavelService;
    private final TutorService tutorService;

    public record ConviteResponsavelRequest(
            @NotBlank(message = "E-mail é obrigatório")
            @Email(message = "E-mail deve ter formato válido")
            String email,

            @NotNull(message = "Permissão é obrigatória")
            PermissaoPet permissao
    ) {}

    public record ConviteResponsavelResponse(
            Long idConvite, String email, String permissao, String status, LocalDateTime expiraEm
    ) {}

    public record ResponsavelResponse(
            Long idResponsavel, Long idTutor, String nome, String email,
            String permissao, String status, LocalDateTime concedidoEm
    ) {}

    private ResponsavelResponse toResponse(TutorResponsavel vinculo) {
        Tutor responsavel = vinculo.getTutorResponsavel();
        return new ResponsavelResponse(
                vinculo.getIdResponsavel(),
                responsavel.getIdTutor(),
                responsavel.getNmTutor(),
                responsavel.getDsEmail(),
                vinculo.getDsPermissao().name(),
                vinculo.getDsStatus().name(),
                vinculo.getDtConcedido()
        );
    }

    @PostMapping("/responsaveis/convites")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TUTOR')")
    @Operation(summary = "Convidar responsável para todos os pets do tutor")
    public ConviteResponsavelResponse criarConviteResponsavel(
            Authentication authentication,
            @Valid @RequestBody ConviteResponsavelRequest request) {
        PetConvite convite = petConviteService.criarResponsavel(
                tutorService.buscarAutenticado(authentication).getIdTutor(), request.email(), request.permissao());
        return new ConviteResponsavelResponse(
                convite.getIdConvite(),
                convite.getDsEmailDestino(),
                convite.getDsPermissao().name(),
                convite.getDsStatus().name(),
                convite.getDtExpiracao()
        );
    }

    @GetMapping("/responsaveis")
    @PreAuthorize("hasRole('TUTOR')")
    @Operation(summary = "Listar responsáveis ativos do tutor autenticado")
    public List<ResponsavelResponse> listarResponsaveis(Authentication authentication) {
        return responsavelService.listarAtivos(tutorService.buscarAutenticado(authentication).getIdTutor())
                .stream().map(this::toResponse).toList();
    }

    @DeleteMapping("/responsaveis/{idResponsavel}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TUTOR')")
    @Operation(summary = "Remover responsável de todos os pets do tutor autenticado")
    public void revogarResponsavel(@PathVariable Long idResponsavel,
                                   Authentication authentication) {
        responsavelService.revogar(tutorService.buscarAutenticado(authentication).getIdTutor(), idResponsavel);
    }
}
