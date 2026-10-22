package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.SiaConversa;
import br.com.fiap.VetSync.entity.SiaMensagem;
import br.com.fiap.VetSync.repository.PetRepository;
import br.com.fiap.VetSync.repository.SiaConversaRepository;
import br.com.fiap.VetSync.repository.SiaMensagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SiaConversaService {
    private final SiaConversaRepository conversas;
    private final SiaMensagemRepository mensagens;
    private final PetRepository pets;

    @Transactional
    public SiaConversa criar(Long idPet, String email) {
        SiaConversa conversa = new SiaConversa();
        conversa.setId(UUID.randomUUID().toString());
        conversa.setPet(pets.findById(idPet).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        conversa.setTutorEmail(email);
        conversa.setCriadoEm(LocalDateTime.now());
        conversa.setAtualizadoEm(conversa.getCriadoEm());
        return conversas.save(conversa);
    }

    @Transactional(readOnly = true)
    public List<SiaConversa> listar(Long idPet, String email) {
        return conversas.findByPet_IdPetAndTutorEmailIgnoreCaseOrderByAtualizadoEmDesc(idPet, email);
    }

    @Transactional(readOnly = true)
    public List<SiaMensagem> mensagens(Long idPet, String id, String email) {
        exigir(idPet, id, email);
        return mensagens.findByConversa_IdOrderByIdAsc(id);
    }

    @Transactional
    public SiaMensagem adicionar(Long idPet, String id, String email, String autoria, String texto) {
        SiaConversa conversa = exigir(idPet, id, email);
        SiaMensagem mensagem = new SiaMensagem();
        mensagem.setConversa(conversa);
        mensagem.setAutoria(autoria);
        mensagem.setTexto(texto);
        mensagem.setCriadoEm(LocalDateTime.now());
        conversa.setAtualizadoEm(mensagem.getCriadoEm());
        return mensagens.save(mensagem);
    }

    private SiaConversa exigir(Long idPet, String id, String email) {
        SiaConversa conversa = conversas.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!conversa.getPet().getIdPet().equals(idPet) || !conversa.getTutorEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return conversa;
    }
}
