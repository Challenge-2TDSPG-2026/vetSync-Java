package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.Admin;
import br.com.fiap.VetSync.entity.Prescricao;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.security.PerfilUtils;
import br.com.fiap.VetSync.security.PetAccessSecurity;
import br.com.fiap.VetSync.service.PrescricaoService;
import br.com.fiap.VetSync.service.VeterinarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/prescricoes")
@RequiredArgsConstructor
@Tag(name = "Prescrições", description = "Veterinário solicita medicamento para o paciente; fica pendente até o ADMIN liberar ou negar")
public class PrescricaoController {

    private final PrescricaoService prescricaoService;
    private final VeterinarioService veterinarioService;
    private final AdminRepository adminRepository;
    private final PetAccessSecurity petAccessSecurity;

    public record PrescricaoRequest(
            @NotNull(message = "idEvento é obrigatório") Long idEvento,
            @NotNull(message = "idMedicamento é obrigatório") Long idMedicamento,
            @NotBlank(message = "dsPosologia é obrigatória") String dsPosologia,
            @NotNull(message = "dtInicio é obrigatória") LocalDate dtInicio,
            LocalDate dtFim,
            @Positive(message = "qtDosesDia deve ser positivo") Integer qtDosesDia
    ) {}

    public record PrescricaoLiberarRequest(boolean aprovado) {}

    public record PrescricaoResponse(
            Long idPrescricao,
            String status,
            Long idEvento,
            String nmMedicamento,
            String dsPosologia,
            LocalDate dtInicio,
            LocalDate dtFim,
            Integer qtDosesDia,
            String nmPet,
            String nmTutor,
            String nmVeterinario
    ) {}

    private PrescricaoResponse toResponse(Prescricao p) {
        var evento = p.getEvento();
        var pet = evento != null ? evento.getPet() : null;
        return new PrescricaoResponse(
                p.getIdPrescricao(),
                p.getDsStatus().name(),
                evento != null ? evento.getIdEvento() : null,
                p.getMedicamento() != null ? p.getMedicamento().getNmMedicamento() : null,
                p.getDsPosologia(),
                p.getDtInicio(),
                p.getDtFim(),
                p.getQtDosesDia(),
                pet != null ? pet.getNmPet() : null,
                pet != null && pet.getTutor() != null ? pet.getTutor().getNmTutor() : null,
                evento != null && evento.getVeterinario() != null ? evento.getVeterinario().getNmVeterinario() : null
        );
    }

    private Long idAdminAutenticado(Authentication authentication) {
        return adminRepository.findByDsEmail(authentication.getName())
                .map(Admin::getIdAdmin)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin autenticado não encontrado"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VETERINARIO')")
    @Operation(summary = "Veterinário solicita um medicamento para o paciente de um evento sob sua responsabilidade",
            description = "Cria a prescrição com status SOLICITADO, aguardando liberação do ADMIN.")
    public PrescricaoResponse solicitar(Authentication authentication, @Valid @RequestBody PrescricaoRequest request) {
        Long idVeterinario = veterinarioService.buscarAutenticado(authentication).getIdVeterinario();
        Prescricao prescricao = prescricaoService.solicitar(
                request.idEvento(), request.idMedicamento(), request.dsPosologia(),
                request.dtInicio(), request.dtFim(), request.qtDosesDia(), idVeterinario
        );
        return toResponse(prescricao);
    }

    @GetMapping
    @Operation(summary = "Listar prescrições", description = "Tutor vê as dos próprios pets; veterinário vê as que ele solicitou; admin vê a fila de pendentes (SOLICITADO).")
    public List<PrescricaoResponse> listar(Authentication authentication) {
        List<Prescricao> prescricoes;
        if (PerfilUtils.isAdmin(authentication)) {
            prescricoes = prescricaoService.listarPendentes();
        } else if (PerfilUtils.isVeterinario(authentication)) {
            prescricoes = prescricaoService.listarPorVeterinario(authentication.getName());
        } else {
            prescricoes = prescricaoService.listarPorTutor(authentication.getName());
        }
        return prescricoes.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar prescrição por ID", description = "Tutor dono do pet, veterinário responsável pelo evento ou qualquer admin.")
    public PrescricaoResponse buscarPorId(@PathVariable Long id, Authentication authentication) {
        Prescricao prescricao = prescricaoService.buscarPorId(id);
        var evento = prescricao.getEvento();
        boolean ehVet = evento != null && evento.getVeterinario() != null
                && evento.getVeterinario().getDsEmail().equalsIgnoreCase(authentication.getName());
        boolean temAcessoAoPet = evento != null && evento.getPet() != null
                && petAccessSecurity.canView(evento.getPet().getIdPet(), authentication);
        if (!PerfilUtils.isAdmin(authentication) && !ehVet && !temAcessoAoPet) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem acesso a essa prescrição");
        }
        return toResponse(prescricao);
    }

    @PatchMapping("/{id}/liberar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin libera ou nega uma prescrição pendente",
            description = "Se aprovado=true, dispara e-mail real avisando o tutor que o medicamento foi liberado.")
    public PrescricaoResponse liberar(@PathVariable Long id, Authentication authentication,
                                      @RequestBody PrescricaoLiberarRequest request) {
        Prescricao prescricao = prescricaoService.liberar(id, idAdminAutenticado(authentication), request.aprovado());
        return toResponse(prescricao);
    }
}