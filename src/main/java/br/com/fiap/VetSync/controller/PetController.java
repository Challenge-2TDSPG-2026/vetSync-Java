package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.EspecieCategoria;
import br.com.fiap.VetSync.entity.Pet;
import br.com.fiap.VetSync.entity.RacaCatalogo;
import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.service.PetService;
import br.com.fiap.VetSync.service.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
@Tag(name = "Pets", description = "Cadastro e gerenciamento de pets")
public class PetController {

    private final PetService petService;
    private final TutorService tutorService;

    public record PetRequest(
            @NotBlank(message = "Nome do pet é obrigatório")
            String nmPet,

            @NotNull(message = "Espécie é obrigatória")
            EspecieCategoria especie,
            String especieOutro,

            @NotBlank(message = "Raça é obrigatória")
            String raca,

            @NotNull(message = "Data de nascimento é obrigatória")
            LocalDate dtNascimento,
            BigDecimal peso,

            @NotBlank(message = "Sexo é obrigatório")
            @Pattern(regexp = "^[MF]$", message = "Sexo deve ser M ou F")
            String sexo
    ) {}

    public record PetResponse(
            Long idPet,
            String nmPet,
            String especie,
            String raca,
            LocalDate dtNascimento,
            int idadeAnos,
            BigDecimal peso,
            String sexo,
            Long idTutor,
            String fotoUrl
    ) {}

    private PetResponse toResponse(Pet pet) {
        int idade = petService.calcularIdade(pet);
        return new PetResponse(
                pet.getIdPet(),
                pet.getNmPet(),
                pet.getRaca() != null && pet.getRaca().getEspecie() != null
                        ? pet.getRaca().getEspecie().getNmEspecie() : null,
                pet.getRaca() != null ? pet.getRaca().getNmRaca() : null,
                pet.getDtNascimento(),
                idade,
                pet.getNrPesoKg(),
                pet.getDsSexo(),
                pet.getTutor() != null ? pet.getTutor().getIdTutor() : null,
                pet.getDsFoto() != null ? "/pets/" + pet.getIdPet() + "/foto" : null
        );
    }

    private Long idTutorAutenticado(Authentication authentication) {
        Tutor tutor = tutorService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor autenticado não encontrado"));
        return tutor.getIdTutor();
    }

    @GetMapping("/racas")
    @PreAuthorize("hasRole('TUTOR') or hasRole('VETERINARIO')")
    @Operation(summary = "Sugerir raças", description = "Retorna raças do catálogo para a espécie informada, filtradas pelo texto digitado (q). Usado para autocomplete no cadastro/atualização de pet.")
    public List<String> sugerirRacas(@RequestParam EspecieCategoria especie, @RequestParam(required = false) String q) {
        return RacaCatalogo.sugerir(especie, q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TUTOR')")
    @Operation(summary = "Cadastrar pet", description = "O tutor vem do token. especie: CAO, GATO, AVE ou OUTRO. sexo: M ou F.")
    public PetResponse cadastrar(Authentication authentication, @RequestBody @Valid PetRequest request) {
        Pet pet = Pet.builder()
                .nmPet(request.nmPet())
                .dtNascimento(request.dtNascimento())
                .nrPesoKg(request.peso())
                .dsSexo(request.sexo())
                .build();
        Long idTutor = idTutorAutenticado(authentication);
        return toResponse(petService.cadastrar(pet, idTutor, request.especie(), request.especieOutro(), request.raca()));
    }

    @GetMapping
    @PreAuthorize("hasRole('TUTOR')")
    @Operation(summary = "Listar pets do tutor autenticado",
            description = "Inclui os pets próprios e os pets de terceiros nos quais o tutor tem acesso ativo como cuidador/cônjuge.")
    public List<PetResponse> listarMeusPets(Authentication authentication) {
        Long idTutor = idTutorAutenticado(authentication);
        return petService.listarAcessiveis(idTutor).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasRole('VETERINARIO') or @petAccessSecurity.canView(#id, authentication)")
    @Operation(summary = "Buscar pet por ID",
            description = "Veterinário vê qualquer pet; tutor vê se for o proprietário ou tiver acesso ativo (LEITURA ou EDICAO).")
    public PetResponse buscarPorId(@PathVariable Long id) {
        return toResponse(petService.buscarPorId(id));
    }

    @GetMapping("/tutor/{idTutor}")
    @PreAuthorize("hasRole('VETERINARIO')")
    @Operation(summary = "Listar pets de um tutor específico. Somente VETERINARIO.")
    public List<PetResponse> listarPorTutor(@PathVariable Long idTutor) {
        return petService.listarPorTutor(idTutor).stream()
                .map(this::toResponse)
                .toList();
    }

    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasRole('TUTOR') and @petSecurity.isOwner(#id, authentication)")
    @Operation(summary = "Atualizar pet (nome, peso, data de nascimento, sexo, raça/espécie). Só o tutor dono do pet.")
    public PetResponse atualizar(@PathVariable Long id, @RequestBody @Valid PetRequest request) {
        Pet petAtualizado = Pet.builder()
                .nmPet(request.nmPet())
                .nrPesoKg(request.peso())
                .dtNascimento(request.dtNascimento())
                .dsSexo(request.sexo())
                .build();
        return toResponse(petService.atualizar(id, petAtualizado, request.especie(), request.especieOutro(), request.raca()));
    }

    // NOVO
    @PutMapping(value = "/{id:\\d+}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@petAccessSecurity.canEdit(#id, authentication)")
    @Operation(summary = "Enviar ou substituir a foto do pet (perfil e carteirinha)",
            description = "Requisição multipart/form-data com o arquivo no campo 'foto' (JPEG, PNG ou WEBP, até 5MB). "
                    + "Permitido ao tutor dono do pet e a cuidadores com permissão de EDICAO.")
    public PetResponse atualizarFoto(@PathVariable Long id, @RequestParam("foto") MultipartFile foto) {
        return toResponse(petService.atualizarFoto(id, foto));
    }

    // NOVO
    @DeleteMapping("/{id:\\d+}/foto")
    @PreAuthorize("@petAccessSecurity.canEdit(#id, authentication)")
    @Operation(summary = "Remover a foto do pet")
    public PetResponse removerFoto(@PathVariable Long id) {
        return toResponse(petService.removerFoto(id));
    }

    // NOVO
    @GetMapping("/{id:\\d+}/foto")
    @PreAuthorize("hasRole('VETERINARIO') or @petAccessSecurity.canView(#id, authentication)")
    @Operation(summary = "Obter a foto do pet (imagem)",
            description = "Exige token JWT: quem tem acesso ao pet (tutor, cuidador com acesso ativo) ou veterinário.")
    public ResponseEntity<byte[]> obterFoto(@PathVariable Long id) {
        Pet pet = petService.buscarPorId(id);
        if (pet.getDsFoto() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esse pet não possui foto cadastrada");
        }
        MediaType mediaType = pet.getDsFotoTipo() != null
                ? MediaType.parseMediaType(pet.getDsFotoTipo())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header("Cache-Control", "private, no-cache")
                .body(pet.getDsFoto());
    }

    @DeleteMapping("/{id:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TUTOR') and @petSecurity.isOwner(#id, authentication)")
    @Operation(summary = "Deletar pet. Só o tutor dono do pet.", description = "Retorna 409 se o pet tiver eventos de saúde vinculados.")
    public void deletar(@PathVariable Long id) {
        petService.deletar(id);
    }
}