package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.TipoEvento;
import br.com.fiap.VetSync.repository.TipoEventoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tipos-evento")
@RequiredArgsConstructor
@Tag(name = "Tipos de Evento", description = "Catálogo de tipos de evento de saúde e seus pontos de fidelidade")
public class TipoEventoController {

    private final TipoEventoRepository tipoEventoRepository;

    public record TipoEventoResponse(
            Long idTipoEvento,
            String nmTipoEvento,
            String dsCategoria,
            String dsModalidadeAgendamento,
            Integer nrDuracaoMinutos,
            Integer nrPontos
    ) {}

    private TipoEventoResponse toResponse(TipoEvento tipo) {
        return new TipoEventoResponse(
                tipo.getIdTipoEvento(),
                tipo.getNmTipoEvento(),
                tipo.getDsCategoria(),
                tipo.getDsModalidadeAgendamento(),
                tipo.getNrDuracaoMinutos(),
                tipo.getNrPontos()
        );
    }

    @GetMapping
    @Operation(summary = "Listar catálogo de tipos de evento", description = "Usado pelo app para popular o seletor de tipo ao solicitar um evento de saúde.")
    public List<TipoEventoResponse> listar() {
        return tipoEventoRepository.findByClinicaIsNull().stream().map(this::toResponse).toList();
    }
}
