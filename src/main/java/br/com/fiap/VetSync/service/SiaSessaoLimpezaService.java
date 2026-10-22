package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.repository.SiaSessaoAgendamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class SiaSessaoLimpezaService {
    private final SiaSessaoAgendamentoRepository repository;

    @Scheduled(fixedDelayString = "${app.sia.limpeza-sessoes-ms:3600000}")
    @Transactional
    public void limparExpiradas() {
        repository.deleteByExpiraEmBefore(LocalDateTime.now(ZoneOffset.UTC));
    }
}
