package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.EventoAnexo;
import br.com.fiap.VetSync.entity.EventoHistorico;
import br.com.fiap.VetSync.service.EventoAnexoService;
import br.com.fiap.VetSync.service.EventoConsultaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/eventos")
@RequiredArgsConstructor
@Tag(name = "Histórico e anexos de eventos", description = "Linha do tempo de alterações e arquivos anexados a um evento de saúde.")
public class EventoDetalheController {

    private final EventoConsultaService eventoConsultaService;
    private final EventoAnexoService eventoAnexoService;

    public record HistoricoResponse(Long id, String acao, String statusAnterior, String statusNovo,
                                    LocalDate dataAnterior, LocalDate dataNova, String horaAnterior, String horaNova,
                                    String observacaoAnterior, String observacaoNova, BigDecimal custoAnterior,
                                    BigDecimal custoNovo, String ator, java.time.LocalDateTime ocorridoEm) {}
    public record AnexoResponse(Long id, String nome, String mimeType, Long tamanho, String ator,
                                java.time.LocalDateTime criadoEm) {}


    @GetMapping("/{id}/historico")
    @PreAuthorize("@eventoSecurity.isRelacionado(#id, authentication)")
    public List<HistoricoResponse> historico(@PathVariable Long id) {
        return eventoConsultaService.historico(id).stream().map(this::toHistoricoResponse).toList();
    }

    @PostMapping(value = "/{id}/anexos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@eventoSecurity.isTutorComEdicao(#id, authentication) or @eventoSecurity.isVeterinarioResponsavel(#id, authentication)")
    @ResponseStatus(HttpStatus.CREATED)
    public AnexoResponse adicionarAnexo(@PathVariable Long id, @RequestPart("arquivo") MultipartFile arquivo,
                                        Authentication authentication) {
        return toAnexoResponse(eventoAnexoService.adicionar(id, arquivo, authentication.getName()));
    }

    @GetMapping("/{id}/anexos")
    @PreAuthorize("@eventoSecurity.isRelacionado(#id, authentication)")
    public List<AnexoResponse> anexos(@PathVariable Long id) {
        return eventoAnexoService.listar(id).stream().map(this::toAnexoResponse).toList();
    }

    @GetMapping("/{id}/anexos/{idAnexo}")
    @PreAuthorize("@eventoSecurity.isRelacionado(#id, authentication)")
    public ResponseEntity<byte[]> baixarAnexo(@PathVariable Long id, @PathVariable Long idAnexo) {
        EventoAnexo anexo = eventoAnexoService.buscar(id, idAnexo);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(anexo.getDsMimeType()))
                .header("Content-Disposition", "attachment; filename=\"" + anexo.getNmArquivo() + "\"")
                .body(anexo.getDsConteudo());
    }

    @DeleteMapping("/{id}/anexos/{idAnexo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@eventoSecurity.isTutorComEdicao(#id, authentication) or @eventoSecurity.isVeterinarioResponsavel(#id, authentication)")
    public void removerAnexo(@PathVariable Long id, @PathVariable Long idAnexo) {
        eventoAnexoService.remover(id, idAnexo);
    }

    private HistoricoResponse toHistoricoResponse(EventoHistorico h) {
        return new HistoricoResponse(h.getIdHistorico(), h.getDsAcao(), h.getDsStatusAnterior(), h.getDsStatusNovo(),
                h.getDtEventoAnterior(), h.getDtEventoNovo(), h.getHrEventoAnterior(), h.getHrEventoNovo(),
                h.getDsObservacaoAnterior(), h.getDsObservacaoNova(), h.getVlCustoAnterior(), h.getVlCustoNovo(),
                h.getDsAtor(), h.getDtOcorrencia());
    }

    private AnexoResponse toAnexoResponse(EventoAnexo a) {
        return new AnexoResponse(a.getIdAnexo(), a.getNmArquivo(), a.getDsMimeType(), a.getNrTamanho(),
                a.getDsAtor(), a.getDtCriacao());
    }
}
