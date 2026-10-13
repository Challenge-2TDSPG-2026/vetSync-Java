package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.service.PetAtividadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Convites e Acessos de Pet")
public class PetAtividadeController {

    private final PetAtividadeService petAtividadeService;

    public record AtividadeResponse(Long idAtividade, String tipo, String descricao, String autorNome,
                                    LocalDateTime dtAtividade) {}

    @GetMapping("/pets/{idPet}/atividades")
    @PreAuthorize("hasRole('TUTOR') and @petAccessSecurity.isOwner(#idPet, authentication)")
    @Operation(summary = "Histórico de atividade do pet (quem fez o quê)",
            description = "Somente o proprietário. Reúne convites, acessos concedidos/removidos, eventos de saúde e "
                    + "acessos ao prontuário, do mais recente ao mais antigo. 'limite' padrão 30, máximo 100.")
    public List<AtividadeResponse> listar(@PathVariable Long idPet,
                                          @RequestParam(required = false) Integer limite) {
        return petAtividadeService.listar(idPet, limite).stream()
                .map(a -> new AtividadeResponse(a.idAtividade(), a.tipo(), a.descricao(), a.autorNome(), a.dtAtividade()))
                .toList();
    }
}