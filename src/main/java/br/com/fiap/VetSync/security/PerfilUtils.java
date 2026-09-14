package br.com.fiap.VetSync.security;

import org.springframework.security.core.Authentication;


public final class PerfilUtils {

    private PerfilUtils() {
    }

    public static boolean isTutor(Authentication authentication) {
        return temRole(authentication, "ROLE_TUTOR");
    }

    public static boolean isVeterinario(Authentication authentication) {
        return temRole(authentication, "ROLE_VETERINARIO");
    }

    public static boolean isProfissionalEstetica(Authentication authentication) {
        return temRole(authentication, "ROLE_PROFISSIONAL_ESTETICA");
    }

    public static boolean isAdmin(Authentication authentication) {
        return temRole(authentication, "ROLE_ADMIN");
    }

    private static boolean temRole(Authentication authentication, String role) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }
}