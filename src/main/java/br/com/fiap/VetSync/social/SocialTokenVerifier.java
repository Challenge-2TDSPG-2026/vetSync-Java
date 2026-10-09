package br.com.fiap.VetSync.social;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Valida tokens de identidade (id_token) do Google e da Apple.
 *
 * Para cada provedor, o token precisa: estar assinado (RS256) por uma chave publicada no JWKS oficial do
 * provedor, ter emissor ("iss") esperado, audiência ("aud") igual a um dos client IDs configurados e não estar
 * expirado. Se nenhum client ID estiver configurado para o provedor, o provedor fica desabilitado: nunca se
 * aceita "qualquer audiência".
 */
@Component
public class SocialTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(SocialTokenVerifier.class);

    static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    static final String APPLE_JWKS_URI = "https://appleid.apple.com/auth/keys";
    static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
    static final Set<String> APPLE_ISSUERS = Set.of("https://appleid.apple.com");

    private static final Duration TOLERANCIA_RELOGIO = Duration.ofSeconds(60);
    private static final int TAMANHO_MAXIMO_TOKEN = 8192;
    private static final String MSG_TOKEN_INVALIDO = "Token de identidade inválido ou expirado";

    private final Map<SocialProvider, JwtDecoder> decoders;

    @Autowired
    public SocialTokenVerifier(@Value("${app.social.google.client-ids:}") String googleClientIds,
                               @Value("${app.social.apple.client-ids:}") String appleClientIds) {
        Map<SocialProvider, JwtDecoder> mapa = new EnumMap<>(SocialProvider.class);
        Set<String> google = parseClientIds(googleClientIds);
        if (!google.isEmpty()) {
            mapa.put(SocialProvider.GOOGLE, construirDecoder(decoderRemoto(GOOGLE_JWKS_URI), GOOGLE_ISSUERS, google));
        }
        Set<String> apple = parseClientIds(appleClientIds);
        if (!apple.isEmpty()) {
            mapa.put(SocialProvider.APPLE, construirDecoder(decoderRemoto(APPLE_JWKS_URI), APPLE_ISSUERS, apple));
        }
        this.decoders = Map.copyOf(mapa);
    }

    /** Construtor para testes: recebe decoders já montados (sem rede). */
    SocialTokenVerifier(Map<SocialProvider, JwtDecoder> decoders) {
        this.decoders = Map.copyOf(decoders);
    }

    public SocialIdentity verificar(SocialProvider provider, String idToken) {
        if (idToken == null || idToken.isBlank() || idToken.length() > TAMANHO_MAXIMO_TOKEN) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, MSG_TOKEN_INVALIDO);
        }
        JwtDecoder decoder = decoders.get(provider);
        if (decoder == null) {
            log.error("Login social {} solicitado, mas nenhum client ID foi configurado", provider);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Login com " + provider + " indisponível no momento");
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken.trim());
        } catch (JwtException | IllegalArgumentException e) {
            // Nunca registrar o token nem a mensagem detalhada (pode conter trechos do token).
            log.warn("Token social rejeitado: provider={} causa={}", provider, e.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, MSG_TOKEN_INVALIDO);
        }
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank() || jwt.getExpiresAt() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, MSG_TOKEN_INVALIDO);
        }
        return new SocialIdentity(provider, subject, textoOuNulo(jwt.getClaim("email"), true),
                booleano(jwt.getClaim("email_verified")), textoOuNulo(jwt.getClaim("name"), false));
    }

    /**
     * Aplica os validadores de prazo, emissor e audiência sobre um decoder que já verifica a assinatura.
     * Público e estático para que os testes usem exatamente a mesma regra com uma chave local.
     */
    public static NimbusJwtDecoder construirDecoder(NimbusJwtDecoder base, Set<String> emissores,
                                                    Set<String> audiencias) {
        OAuth2TokenValidator<Jwt> emissor = jwt -> {
            Object iss = jwt.getClaims().get("iss");
            if (iss != null && emissores.contains(iss.toString())) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Emissor inválido", null));
        };
        OAuth2TokenValidator<Jwt> audiencia = jwt -> {
            List<String> aud = jwt.getAudience();
            if (aud != null && aud.stream().anyMatch(audiencias::contains)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Audiência inválida", null));
        };
        base.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(TOLERANCIA_RELOGIO), emissor, audiencia));
        return base;
    }

    private static NimbusJwtDecoder decoderRemoto(String jwksUri) {
        RestTemplate rest = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
        return NimbusJwtDecoder.withJwkSetUri(jwksUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .restOperations(rest)
                .build();
    }

    static Set<String> parseClientIds(String valor) {
        Set<String> ids = new LinkedHashSet<>();
        if (valor != null) {
            Arrays.stream(valor.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(ids::add);
        }
        return ids;
    }

    private static String textoOuNulo(Object valor, boolean minusculas) {
        if (!(valor instanceof String s) || s.isBlank()) {
            return null;
        }
        String limpo = s.trim();
        return minusculas ? limpo.toLowerCase(Locale.ROOT) : limpo;
    }

    /** A Apple envia "email_verified" como string ("true"); o Google, como booleano. */
    private static boolean booleano(Object valor) {
        return Boolean.TRUE.equals(valor) || (valor instanceof String s && "true".equalsIgnoreCase(s));
    }
}