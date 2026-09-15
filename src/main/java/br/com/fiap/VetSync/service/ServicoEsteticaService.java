package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.ServicoEstetica;
import br.com.fiap.VetSync.entity.TipoServicoEstetica;
import br.com.fiap.VetSync.repository.ServicoEsteticaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServicoEsteticaService {

    private final ServicoEsteticaRepository servicoEsteticaRepository;

    public List<ServicoEstetica> listarTodos() {
        return servicoEsteticaRepository.findAll();
    }

    public List<ServicoEstetica> listarBase() {
        return servicoEsteticaRepository.findByTpServico(TipoServicoEstetica.BASE);
    }

    public List<ServicoEstetica> listarExtras() {
        return servicoEsteticaRepository.findByTpServico(TipoServicoEstetica.EXTRA);
    }

    public ServicoEstetica buscarPorId(Long id) {
        return servicoEsteticaRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Serviço não encontrado com id: " + id));
    }


    public Set<ServicoEstetica> buscarVarios(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        List<Long> unicos = ids.stream().distinct().toList();
        List<ServicoEstetica> encontrados = servicoEsteticaRepository.findAllById(unicos);
        if (encontrados.size() != unicos.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Um ou mais serviços informados não existem");
        }
        return new HashSet<>(encontrados);
    }
}