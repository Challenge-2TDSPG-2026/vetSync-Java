package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.SiaSessaoAgendamento;
import br.com.fiap.VetSync.repository.SiaSessaoAgendamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Guarda temporariamente o rascunho de agendamento da conversa com a SIA (validade maxima de 16 minutos). */
@Service
@RequiredArgsConstructor
public class SiaSessaoAgendamentoService {

    private static final long VALIDADE_MAXIMA_MINUTOS = 16;

    private final SiaSessaoAgendamentoRepository repository;

    @Transactional
    public SiaSessaoAgendamento salvar(String idCaminho, String idCorpo, String dados,
                                       LocalDateTime expiraEm, String emailTutor) {
        LocalDateTime agora = LocalDateTime.now(ZoneOffset.UTC);
        if (!idCaminho.equals(idCorpo) || expiraEm == null || !expiraEm.isAfter(agora)
                || expiraEm.isAfter(agora.plusMinutes(VALIDADE_MAXIMA_MINUTOS))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sessão inválida");
        }
        SiaSessaoAgendamento item = repository.findById(idCaminho).orElseGet(SiaSessaoAgendamento::new);
        exigirDono(item, emailTutor);
        item.setId(idCaminho);
        item.setTutorEmail(emailTutor);
        item.setDados(dados);
        item.setExpiraEm(expiraEm);
        return repository.save(item);
    }

    @Transactional(readOnly = true)
    public SiaSessaoAgendamento buscar(String id, String emailTutor) {
        SiaSessaoAgendamento item = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE));
        exigirDono(item, emailTutor);
        if (!item.getExpiraEm().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new ResponseStatusException(HttpStatus.GONE);
        }
        return item;
    }

    private void exigirDono(SiaSessaoAgendamento item, String emailTutor) {
        if (item.getTutorEmail() != null && !item.getTutorEmail().equalsIgnoreCase(emailTutor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
