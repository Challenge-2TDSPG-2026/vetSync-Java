package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.entity.SiaResumo;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import br.com.fiap.VetSync.repository.PetRepository;
import br.com.fiap.VetSync.repository.SiaResumoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SiaResumoService {
    private final SiaResumoRepository repository;
    private final PetRepository petRepository;
    private final EventoSaudeRepository eventoRepository;

    @Transactional
    public SiaResumo criar(Long idPet, String email, String resumo, String perguntas, Long idEvento) {
        SiaResumo item = new SiaResumo();
        item.setPet(petRepository.findById(idPet).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        item.setTutorEmail(email);
        item.setResumo(resumo);
        item.setPerguntas(perguntas);
        item.setEvento(eventoDoPet(idPet, idEvento));
        return repository.save(item);
    }

    @Transactional(readOnly = true)
    public List<SiaResumo> listarTutor(Long idPet, String email) {
        return repository.findByPet_IdPetAndTutorEmailIgnoreCaseOrderByCriadoEmDesc(idPet, email);
    }

    @Transactional(readOnly = true)
    public List<SiaResumo> listarVet(Long idEvento) {
        return repository.findByEvento_IdEventoAndCompartilhadoEmIsNotNullAndRevogadoEmIsNullOrderByCriadoEmDesc(idEvento);
    }

    @Transactional
    public SiaResumo atualizar(Long idPet, Long id, String email, String resumo, String perguntas, Long idEvento) {
        SiaResumo item = exigirTutor(idPet, id, email);
        item.setResumo(resumo);
        item.setPerguntas(perguntas);
        item.setEvento(eventoDoPet(idPet, idEvento));
        item.setCompartilhadoEm(null);
        item.setRevogadoEm(null);
        return item;
    }

    @Transactional
    public SiaResumo compartilhar(Long idPet, Long id, String email) {
        SiaResumo item = exigirTutor(idPet, id, email);
        if (item.getEvento() == null || item.getEvento().getVeterinario() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vincule uma consulta com veterinário antes de compartilhar");
        }
        item.setCompartilhadoEm(LocalDateTime.now());
        item.setRevogadoEm(null);
        return item;
    }

    @Transactional
    public SiaResumo revogar(Long idPet, Long id, String email) {
        SiaResumo item = exigirTutor(idPet, id, email);
        item.setRevogadoEm(LocalDateTime.now());
        return item;
    }

    private SiaResumo exigirTutor(Long idPet, Long id, String email) {
        SiaResumo item = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!item.getPet().getIdPet().equals(idPet) || !item.getTutorEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return item;
    }

    private EventoSaude eventoDoPet(Long idPet, Long idEvento) {
        if (idEvento == null) return null;
        EventoSaude evento = eventoRepository.findById(idEvento).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!evento.getPet().getIdPet().equals(idPet)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consulta não pertence ao pet");
        }
        return evento;
    }
}
