package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.AcaoIaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AcaoIaService {
    private final AcaoIaRepository repository;

    @Transactional
    public AcaoIa criar(String email, String acao, String resumo, String dados) {
        if (!"CRIAR_EVENTO".equals(acao)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ação de IA não permitida");
        }
        return repository.save(AcaoIa.builder()
                .acao(acao).resumo(resumo).dados(dados).emailUsuario(email)
                .status(StatusAcaoIa.PENDENTE)
                .criacao(LocalDateTime.now()).expiracao(LocalDateTime.now().plusMinutes(10))
                .build());
    }

    @Transactional
    public AcaoIa confirmar(Long id, String email) {
        AcaoIa acao = repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Prévia não encontrada"));
        if (!email.equals(acao.getEmailUsuario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Prévia pertence a outro usuário");
        }
        if (acao.getStatus() != StatusAcaoIa.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Prévia já processada");
        }
        if (acao.getExpiracao().isBefore(LocalDateTime.now())) {
            acao.setStatus(StatusAcaoIa.EXPIRADA);
            repository.save(acao);
            throw new ResponseStatusException(HttpStatus.GONE, "Prévia expirada");
        }
        acao.setStatus(StatusAcaoIa.CONFIRMADA);
        return repository.save(acao);
    }
}
