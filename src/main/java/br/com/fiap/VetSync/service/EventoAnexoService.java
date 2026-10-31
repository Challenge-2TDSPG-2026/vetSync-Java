package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.EventoAnexo;
import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.exception.Erros;
import br.com.fiap.VetSync.repository.EventoAnexoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/** Anexos (imagens e PDFs) de um evento de saúde. */
@Service
@RequiredArgsConstructor
public class EventoAnexoService {

    private static final long LIMITE_TAMANHO_BYTES = 10L * 1024 * 1024;

    private final EventoService eventoService;
    private final EventoAnexoRepository eventoAnexoRepository;

    public List<EventoAnexo> listar(Long idEvento) {
        eventoService.buscarPorId(idEvento);
        return eventoAnexoRepository.findByEvento_IdEventoOrderByDtCriacaoDesc(idEvento);
    }

    public EventoAnexo adicionar(Long idEvento, MultipartFile file, String ator) {
        EventoSaude evento = eventoService.buscarPorId(idEvento);
        if (file == null || file.isEmpty()) {
            throw Erros.arquivoObrigatorio();
        }
        if (file.getSize() > LIMITE_TAMANHO_BYTES) {
            throw Erros.arquivoExcedeLimite();
        }
        String tipo = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        if (!tipo.startsWith("image/") && !tipo.equals("application/pdf")) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de arquivo não permitido");
        }
        try {
            EventoAnexo anexo = EventoAnexo.builder().evento(evento).nmArquivo(file.getOriginalFilename() == null ? "anexo" : file.getOriginalFilename())
                    .dsMimeType(tipo).nrTamanho(file.getSize()).dsConteudo(file.getBytes()).dsAtor(ator)
                    .dtCriacao(LocalDateTime.now()).build();
            return eventoAnexoRepository.save(anexo);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo", ex);
        }
    }

    public EventoAnexo buscar(Long idEvento, Long idAnexo) {
        return eventoAnexoRepository.findByIdAnexoAndEvento_IdEvento(idAnexo, idEvento)
                .orElseThrow(Erros::anexoNaoEncontrado);
    }

    public void remover(Long idEvento, Long idAnexo) {
        eventoAnexoRepository.delete(buscar(idEvento, idAnexo));
    }
}
