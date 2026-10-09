package br.com.fiap.VetSync.social;

/**
 * Dados extraídos de um token de identidade JÁ VALIDADO (assinatura, emissor, audiência e expiração).
 *
 * @param subject       identificador único e estável do usuário no provedor (claim "sub")
 * @param email         e-mail do token, normalizado em minúsculas; pode ser nulo (Apple) ou um relay privado
 * @param emailVerified true somente se o provedor afirma que o e-mail foi verificado
 * @param nome          nome presente no token (Google); a Apple não o inclui no token
 */
public record SocialIdentity(SocialProvider provider, String subject, String email,
                             boolean emailVerified, String nome) {
}