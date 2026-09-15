package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.ServicoEstetica;
import br.com.fiap.VetSync.service.ServicoEsteticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/servicos-estetica")
@RequiredArgsConstructor
@Tag(name = "Serviços de Estética", description = "Catálogo de serviços base (banho/tosa) e extras (higiene bucal, hidratação, etc.)")
public class ServicoEsteticaController {

    private final ServicoEsteticaService servicoEsteticaService;

    public record ServicoResponse(Long idServico, String nmServico, String tpServico) {}

    private ServicoResponse toResponse(ServicoEstetica s) {
        return new ServicoResponse(s.getIdServico(), s.getNmServico(), s.getTpServico().name());
    }

    @GetMapping
    @Operation(summary = "Listar todos os serviços (base + extras)")
    public List<ServicoResponse> listar() {
        return servicoEsteticaService.listarTodos().stream().map(this::toResponse).toList();
    }

    @GetMapping("/base")
    @Operation(summary = "Listar só os serviços base (banho, banho e tosa na tesoura, banho e tosa na máquina)")
    public List<ServicoResponse> listarBase() {
        return servicoEsteticaService.listarBase().stream().map(this::toResponse).toList();
    }

    @GetMapping("/extras")
    @Operation(summary = "Listar só os serviços extras opcionais")
    public List<ServicoResponse> listarExtras() {
        return servicoEsteticaService.listarExtras().stream().map(this::toResponse).toList();
    }
}