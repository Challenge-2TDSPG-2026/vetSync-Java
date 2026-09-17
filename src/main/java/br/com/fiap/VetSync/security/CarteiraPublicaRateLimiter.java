package br.com.fiap.VetSync.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


@Component
public class CarteiraPublicaRateLimiter {

    private static final int LIMITE_REQUISICOES = 20;
    private static final long JANELA_MS = 60_000L;

    private record Janela(AtomicLong contador, AtomicLong inicioMs) {}

    private final Map<String, Janela> janelasPorChave = new ConcurrentHashMap<>();


    public boolean permitir(String chave) {
        if (chave == null || chave.isBlank()) {
            chave = "desconhecido";
        }
        long agora = Instant.now().toEpochMilli();
        Janela janela = janelasPorChave.computeIfAbsent(chave, k -> new Janela(new AtomicLong(0), new AtomicLong(agora)));

        synchronized (janela) {
            if (agora - janela.inicioMs().get() > JANELA_MS) {
                janela.inicioMs().set(agora);
                janela.contador().set(0);
            }
            long total = janela.contador().incrementAndGet();
            limparEntradasAntigas(agora);
            return total <= LIMITE_REQUISICOES;
        }
    }

    private void limparEntradasAntigas(long agora) {
        if (janelasPorChave.size() < 10_000) {
            return;
        }
        janelasPorChave.entrySet().removeIf(e -> agora - e.getValue().inicioMs().get() > JANELA_MS * 5);
    }
}