package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.PermissaoPet;
import br.com.fiap.VetSync.entity.PetAcesso;
import br.com.fiap.VetSync.entity.PetConvite;
import br.com.fiap.VetSync.entity.RelacaoPet;
import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.service.JwtService;
import br.com.fiap.VetSync.service.PetAcessoService;
import br.com.fiap.VetSync.service.PetConviteService;
import br.com.fiap.VetSync.service.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Convites e Acessos de Pet", description = "Convite de cuidador/cônjuge para acesso adicional a um pet já cadastrado")
public class PetConviteController {

    private final PetConviteService petConviteService;
    private final PetAcessoService petAcessoService;
    private final TutorService tutorService;
    private final JwtService jwtService;

    // ---------------------------------------------------------------
    // DTOs
    // ---------------------------------------------------------------

    public record ConviteRequest(
            @NotBlank(message = "E-mail é obrigatório")
            @Email(message = "E-mail deve ter formato válido")
            String email,

            @NotNull(message = "Relação é obrigatória")
            RelacaoPet relacao,

            @NotNull(message = "Permissão é obrigatória")
            PermissaoPet permissao
    ) {}

    public record ConviteResponse(
            Long idConvite, String email, String nomePet, String relacao, String permissao,
            String status, LocalDateTime expiraEm
    ) {}

    public record ConvitePublicoResponse(
            String email, String nomePet, String relacao, String permissao, LocalDateTime expiraEm
    ) {}

    public record RegistrarConviteRequest(
            @NotBlank(message = "Token é obrigatório")
            String token,

            @NotBlank(message = "Nome é obrigatório")
            @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
            String nome,

            @NotBlank(message = "CPF é obrigatório")
            @Pattern(regexp = "^\\d{11}$", message = "CPF deve conter 11 dígitos numéricos")
            String cpf,

            @Pattern(regexp = "^\\d{10,11}$", message = "Telefone deve conter 10 ou 11 dígitos")
            String telefone,

            @NotBlank(message = "Senha é obrigatória")
            @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
            String senha
    ) {}

    public record RegistroResponse(String token, Long idUsuario, String email, String nome, String perfil) {}

    public record AcessoResponse(
            Long idAcesso, Long idTutor, String nomeTutor, String emailTutor,
            String relacao, String permissao, String status, LocalDateTime dtConcedido
    ) {}

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Long idTutorAutenticado(Authentication authentication) {
        Tutor tutor = tutorService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor autenticado não encontrado"));
        return tutor.getIdTutor();
    }

    private ConviteResponse toResponse(PetConvite convite) {
        return new ConviteResponse(
                convite.getIdConvite(),
                convite.getDsEmailDestino(),
                convite.getPet() != null ? convite.getPet().getNmPet() : null,
                convite.getDsRelacao().name(),
                convite.getDsPermissao().name(),
                convite.getDsStatus().name(),
                convite.getDtExpiracao()
        );
    }

    private AcessoResponse toResponse(PetAcesso acesso) {
        Tutor tutor = acesso.getTutor();
        return new AcessoResponse(
                acesso.getIdAcesso(),
                tutor != null ? tutor.getIdTutor() : null,
                tutor != null ? tutor.getNmTutor() : null,
                tutor != null ? tutor.getDsEmail() : null,
                acesso.getDsRelacao().name(),
                acesso.getDsPermissao().name(),
                acesso.getDsStatus().name(),
                acesso.getDtConcedido()
        );
    }

    // ---------------------------------------------------------------
    // Tutor proprietário cria convite
    // ---------------------------------------------------------------

    @PostMapping("/pets/{idPet}/convites")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TUTOR') and @petAccessSecurity.isOwner(#idPet, authentication)")
    @Operation(summary = "Convidar cuidador/cônjuge para um pet",
            description = "Somente o proprietário principal do pet pode convidar. Cria um convite pendente e envia " +
                    "um e-mail com o link de aceite. Retorna 409 se o e-mail já tiver uma conta cadastrada.")
    public ConviteResponse criar(@PathVariable Long idPet, Authentication authentication,
                                 @Valid @RequestBody ConviteRequest request) {
        Long idTutor = idTutorAutenticado(authentication);
        PetConvite convite = petConviteService.criar(idPet, idTutor, request.email(), request.relacao(), request.permissao());
        return toResponse(convite);
    }

    // ---------------------------------------------------------------
    // Site consulta o convite (público)
    // ---------------------------------------------------------------

    @GetMapping("/auth/convites/{token}")
    @Operation(summary = "Consultar convite pelo token (público)",
            description = "Usado pelo site para exibir o e-mail (bloqueado) antes do cadastro do cuidador. " +
                    "Não retorna CPF, id do tutor, token ou hash.")
    public ConvitePublicoResponse consultar(@PathVariable String token) {
        var publico = petConviteService.consultarPorToken(token);
        return new ConvitePublicoResponse(
                publico.email(), publico.nomePet(), publico.relacao().name(), publico.permissao().name(), publico.expiraEm()
        );
    }

    // ---------------------------------------------------------------
    // Site registra o cuidador (público)
    // ---------------------------------------------------------------

    @PostMapping("/auth/registrar-convite")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar cuidador a partir de um convite (público)",
            description = "O e-mail vem exclusivamente do convite — não é aceito no body. Cria o Tutor, aceita o " +
                    "convite e libera o acesso ao pet, tudo em uma única transação.")
    public RegistroResponse registrar(@Valid @RequestBody RegistrarConviteRequest request) {
        Tutor tutor = petConviteService.aceitar(
                request.token(), request.nome(), request.cpf(), request.telefone(), request.senha());
        String jwt = jwtService.gerarToken(tutor.getDsEmail());
        return new RegistroResponse(jwt, tutor.getIdTutor(), tutor.getDsEmail(), tutor.getNmTutor(), "TUTOR");
    }

    // ---------------------------------------------------------------
    // Tutor proprietário gerencia acessos e convites
    // ---------------------------------------------------------------

    @GetMapping("/pets/{idPet}/acessos")
    @PreAuthorize("hasRole('TUTOR') and @petAccessSecurity.isOwner(#idPet, authentication)")
    @Operation(summary = "Listar acessos ativos (cuidadores/cônjuge) de um pet")
    public List<AcessoResponse> listarAcessos(@PathVariable Long idPet) {
        return petAcessoService.listarAtivosDoPet(idPet).stream().map(this::toResponse).toList();
    }

    @DeleteMapping("/pets/{idPet}/acessos/{idAcesso}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TUTOR') and @petAccessSecurity.isOwner(#idPet, authentication)")
    @Operation(summary = "Revogar o acesso de um cuidador/cônjuge")
    public void revogarAcesso(@PathVariable Long idPet, @PathVariable Long idAcesso) {
        petAcessoService.revogar(idPet, idAcesso);
    }

    @DeleteMapping("/pets/{idPet}/convites/{idConvite}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TUTOR') and @petAccessSecurity.isOwner(#idPet, authentication)")
    @Operation(summary = "Cancelar um convite pendente")
    public void cancelarConvite(@PathVariable Long idPet, @PathVariable Long idConvite) {
        petConviteService.cancelar(idPet, idConvite);
    }

    // ---------------------------------------------------------------
    // Erros do fluxo público de convite, no formato {"message": "..."}
    // ---------------------------------------------------------------

    @ExceptionHandler(PetConviteService.ConviteException.class)
    public ResponseEntity<Map<String, String>> handleConviteException(PetConviteService.ConviteException ex) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }
}