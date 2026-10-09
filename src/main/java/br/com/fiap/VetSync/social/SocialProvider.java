package br.com.fiap.VetSync.social;

import java.util.Locale;
import java.util.Optional;

public enum SocialProvider {
    GOOGLE,
    APPLE;

    /** Converte o texto recebido do app. Retorna vazio para valores desconhecidos. */
    public static Optional<SocialProvider> from(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(valor.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}