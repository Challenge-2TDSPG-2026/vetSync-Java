package br.com.fiap.VetSync.security;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda em memória os códigos de verificação usados no fluxo "Esqueci minha senha".
 * Cada e-mail tem no máximo um código válido por vez: uma nova solicitação sempre
 * substitui o código anterior.
 *
 * Observação: por ser em memória, os códigos se perdem se a aplicação reiniciar e
 * não são compartilhados entre múltiplas instâncias. Para produção com mais de uma
 * instância rodando, mova este armazenamento para uma tabela no banco (ou Redis).
 */
@Component
public class CodigoRedefinicaoStore {

    private static final int MAX_TENTATIVAS = 5;

    public record Registro(String codigo, LocalDateTime expiraEm, int tentativas) {}

    private final Map<String, Registro> codigos = new ConcurrentHashMap<>();

    public void salvar(String email, String codigo, LocalDateTime expiraEm) {
        codigos.put(chave(email), new Registro(codigo, expiraEm, 0));
    }

    public Registro buscar(String email) {
        return codigos.get(chave(email));
    }

    public void registrarTentativaFalha(String email) {
        codigos.computeIfPresent(chave(email), (k, r) ->
                new Registro(r.codigo(), r.expiraEm(), r.tentativas() + 1));
    }

    public boolean excedeuTentativas(String email) {
        Registro r = codigos.get(chave(email));
        return r != null && r.tentativas() >= MAX_TENTATIVAS;
    }

    public void remover(String email) {
        codigos.remove(chave(email));
    }

    private String chave(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}