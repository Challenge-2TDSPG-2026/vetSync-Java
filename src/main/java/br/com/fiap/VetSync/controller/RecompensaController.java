package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.Recompensa;
import br.com.fiap.VetSync.entity.Resgate;
import br.com.fiap.VetSync.entity.TipoRecompensa;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import br.com.fiap.VetSync.service.RecompensaService;
import br.com.fiap.VetSync.service.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/recompensas")
@RequiredArgsConstructor
@Tag(name = "Recompensas", description = "Catálogo de prêmios/produtos, saldo de pontos e resgate")
public class RecompensaController {

    private final RecompensaService recompensaService;
    private final TutorService tutorService;
    private final VeterinarioRepository veterinarioRepository;

    // Usado com multipart/form-data: os campos chegam como @RequestParam, não como JSON no corpo.
    public record RecompensaRequest(
            @NotBlank(message = "Nome é obrigatório")
            String nome,

            String descricao,

            @NotNull(message = "Custo em pontos é obrigatório")
            @Positive(message = "Custo em pontos deve ser positivo")
            Integer custoPontos,

            @NotNull(message = "Tipo é obrigatório")
            TipoRecompensa tipo
    ) {}

    // NOVO: edição (multipart). 'ativo' é opcional; 'removerImagem' só vale se nenhuma imagem nova for enviada.
    public record RecompensaUpdateRequest(
            @NotBlank(message = "Nome é obrigatório")
            String nome,

            String descricao,

            @NotNull(message = "Custo em pontos é obrigatório")
            @Positive(message = "Custo em pontos deve ser positivo")
            Integer custoPontos,

            @NotNull(message = "Tipo é obrigatório")
            TipoRecompensa tipo,

            Boolean ativo,

            Boolean removerImagem
    ) {}

    public record RecompensaResponse(
            Long idRecompensa, String nome, String descricao, Integer custoPontos, String tipo, boolean ativa,
            String imagemUrl
    ) {}

    public record ExclusaoResponse(boolean excluidoDefinitivamente, String mensagem) {}

    public record ResgateResponse(
            Long idResgate, String status, LocalDateTime dtResgate,
            String nmRecompensa, Integer custoPontos, String nmVeterinarioValidador
    ) {}

    public record ValidarResgateRequest(
            @NotNull(message = "Campo 'aprovado' é obrigatório")
            Boolean aprovado
    ) {}

    private RecompensaResponse toResponse(Recompensa r) {
        String imagemUrl = r.getDsImagem() != null ? "/recompensas/" + r.getIdRecompensa() + "/imagem" : null;
        return new RecompensaResponse(r.getIdRecompensa(), r.getNmRecompensa(), r.getDsDescricao(),
                r.getNrCustoPontos(), r.getDsTipo().name(), Boolean.TRUE.equals(r.getFlAtivo()), imagemUrl);
    }

    private ResgateResponse toResponse(Resgate r) {
        return new ResgateResponse(
                r.getIdResgate(), r.getDsStatus().name(), r.getDtResgate(),
                r.getRecompensa().getNmRecompensa(), r.getRecompensa().getNrCustoPontos(),
                r.getVeterinarioValidador() != null ? r.getVeterinarioValidador().getNmVeterinario() : null
        );
    }

    private Long idTutorAutenticado(Authentication authentication) {
        return tutorService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor autenticado não encontrado"))
                .getIdTutor();
    }

    private Long idVeterinarioAutenticado(Authentication authentication) {
        return veterinarioRepository.findByDsEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veterinário autenticado não encontrado"))
                .getIdVeterinario();
    }

    @GetMapping
    @Operation(summary = "Listar recompensas/produtos ativos do catálogo")
    public List<RecompensaResponse> listar() {
        return recompensaService.listarAtivas().stream().map(this::toResponse).toList();
    }

    // NOVO
    @GetMapping("/todas")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todo o catálogo, inclusive inativos. Somente ADMIN.")
    public List<RecompensaResponse> listarTodas() {
        return recompensaService.listarTodas().stream().map(this::toResponse).toList();
    }

    // NOVO
    @GetMapping("/{id}")
    @Operation(summary = "Buscar recompensa/produto por ID")
    public RecompensaResponse buscarPorId(@PathVariable Long id) {
        return toResponse(recompensaService.buscarPorId(id));
    }

    // ALTERADO: cadastro restrito ao ADMIN
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Cadastrar recompensa/produto no catálogo (com upload de imagem opcional). Somente ADMIN.",
            description = "Requisição multipart/form-data. Campos: nome, descricao, custoPontos, tipo (PRODUTO ou CUPOM_DESCONTO) e, opcionalmente, o arquivo 'imagem' (JPEG, PNG ou WEBP, até 5MB)."
    )
    public RecompensaResponse criar(@Valid RecompensaRequest request,
                                    @RequestParam(value = "imagem", required = false) MultipartFile imagem) {
        Recompensa recompensa = recompensaService.criar(
                request.nome(), request.descricao(), request.custoPontos(), request.tipo(), imagem
        );
        return toResponse(recompensa);
    }

    // NOVO
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Editar recompensa/produto (dados e foto). Somente ADMIN.",
            description = "Requisição multipart/form-data. Campos: nome, descricao, custoPontos, tipo, ativo (opcional), "
                    + "removerImagem (opcional) e 'imagem' (opcional; se enviada, substitui a foto atual)."
    )
    public RecompensaResponse atualizar(@PathVariable Long id, @Valid RecompensaUpdateRequest request,
                                        @RequestParam(value = "imagem", required = false) MultipartFile imagem) {
        Recompensa recompensa = recompensaService.atualizar(
                id, request.nome(), request.descricao(), request.custoPontos(), request.tipo(),
                request.ativo(), imagem, Boolean.TRUE.equals(request.removerImagem())
        );
        return toResponse(recompensa);
    }

    // NOVO
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Excluir recompensa/produto. Somente ADMIN.",
            description = "Se o produto já tiver resgates vinculados, ele é apenas inativado para preservar o histórico dos tutores."
    )
    public ExclusaoResponse excluir(@PathVariable Long id) {
        var resultado = recompensaService.excluir(id);
        return resultado.excluidoDefinitivamente()
                ? new ExclusaoResponse(true, "Produto excluído")
                : new ExclusaoResponse(false, "Produto possui resgates vinculados e foi apenas inativado");
    }

    @GetMapping("/{id}/imagem")
    @Operation(summary = "Obter a imagem cadastrada de uma recompensa/produto do catálogo")
    public ResponseEntity<byte[]> obterImagem(@PathVariable Long id) {
        Recompensa recompensa = recompensaService.buscarPorId(id);
        if (recompensa.getDsImagem() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Essa recompensa não possui imagem cadastrada");
        }
        MediaType mediaType = recompensa.getDsImagemTipo() != null
                ? MediaType.parseMediaType(recompensa.getDsImagemTipo())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(mediaType).body(recompensa.getDsImagem());
    }

    @GetMapping("/saldo")
    @PreAuthorize("hasRole('TUTOR')")
    @Operation(summary = "Saldo de pontos do tutor autenticado")
    public int saldo(Authentication authentication) {
        return recompensaService.calcularSaldo(idTutorAutenticado(authentication));
    }

    @PatchMapping("/{id}/resgatar")
    @PreAuthorize("hasRole('TUTOR')")
    @Operation(summary = "Resgatar uma recompensa", description = "Debita do saldo do tutor autenticado e cria um resgate PENDENTE, aguardando validação de um veterinário.")
    public ResgateResponse resgatar(@PathVariable Long id, Authentication authentication) {
        return toResponse(recompensaService.solicitarResgate(idTutorAutenticado(authentication), id));
    }

    @GetMapping("/resgates")
    @Operation(summary = "Listar resgates", description = "Tutor vê os próprios; veterinário vê todos os pendentes de validação.")
    public List<ResgateResponse> listarResgates(Authentication authentication) {
        boolean ehVeterinario = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VETERINARIO"));
        List<Resgate> resgates = ehVeterinario
                ? recompensaService.listarPendentes()
                : recompensaService.listarResgatesDoTutor(idTutorAutenticado(authentication));
        return resgates.stream().map(this::toResponse).toList();
    }

    @PatchMapping("/resgates/{idResgate}/validar")
    @PreAuthorize("hasRole('VETERINARIO')")
    @Operation(summary = "Validar ou negar um resgate pendente. Somente VETERINARIO.")
    public ResgateResponse validar(@PathVariable Long idResgate, Authentication authentication,
                                   @Valid @RequestBody ValidarResgateRequest request) {
        return toResponse(recompensaService.validar(idResgate, idVeterinarioAutenticado(authentication), request.aprovado()));
    }
}