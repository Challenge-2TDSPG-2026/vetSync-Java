package br.com.fiap.VetSync.security;

import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlacklist {

    private final Set<String> tokensRevogados = ConcurrentHashMap.newKeySet();
    private final Map<String, Date> revogacaoPorUsuario = new ConcurrentHashMap<>();

    public void revogar(String token) {
        tokensRevogados.add(token);
    }

    /** Invalida todos os tokens já emitidos para um usuário, por exemplo após trocar a senha. */
    public void revogarSessoesDoUsuario(String email) {
        revogacaoPorUsuario.put(normalizarEmail(email), new Date());
    }

    public boolean isRevogado(String token) {
        return tokensRevogados.contains(token);
    }

    public boolean isRevogadoParaUsuario(String email, Date emitidoEm) {
        Date revogadoEm = revogacaoPorUsuario.get(normalizarEmail(email));
        return revogadoEm != null && emitidoEm != null && !emitidoEm.after(revogadoEm);
    }

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
