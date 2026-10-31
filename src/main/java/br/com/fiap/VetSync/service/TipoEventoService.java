package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.TipoEvento;
import br.com.fiap.VetSync.repository.TipoEventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoEventoService {

    private final TipoEventoRepository tipoEventoRepository;

    /** Catalogo global (tipos que nao pertencem a uma clinica especifica). */
    public List<TipoEvento> listarCatalogoGlobal() {
        return tipoEventoRepository.findByClinicaIsNull();
    }
}
