package br.com.fiap.VetSync.social;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.server.ResponseStatusException;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Valida a regra de verificação com chaves RSA geradas no teste: sem rede e sem contas reais. */
class SocialTokenVerifierTest {

    private static final String GOOGLE_WEB = "google-web.apps.googleusercontent.com";
    private static final String GOOGLE_ANDROID = "google-android.apps.googleusercontent.com";
    private static final String APPLE_BUNDLE = "br.com.fiap.vetsync";

    private static KeyPair chave;
    private static KeyPair outraChave;
    private static SocialTokenVerifier verifier;

    @BeforeAll
    static void preparar() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        chave = gen.generateKeyPair();
        outraChave = gen.generateKeyPair();
        verifier = new SocialTokenVerifier(Map.of(
                SocialProvider.GOOGLE, SocialTokenVerifier.construirDecoder(
                        decoder(chave), SocialTokenVerifier.GOOGLE_ISSUERS, Set.of(GOOGLE_WEB, GOOGLE_ANDROID)),
                SocialProvider.APPLE, SocialTokenVerifier.construirDecoder(
                        decoder(chave), SocialTokenVerifier.APPLE_ISSUERS, Set.of(APPLE_BUNDLE))));
    }

    private static NimbusJwtDecoder decoder(KeyPair kp) {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) kp.getPublic())
                .signatureAlgorithm(SignatureAlgorithm.RS256).build();
    }

    private static JWTClaimsSet.Builder claimsGoogle() {
        return new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .audience(GOOGLE_WEB)
                .subject("google-sub-123")
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .claim("email", "Maria@Gmail.com")
                .claim("email_verified", true)
                .claim("name", "Maria Silva");
    }

    private static JWTClaimsSet.Builder claimsApple() {
        return new JWTClaimsSet.Builder()
                .issuer("https://appleid.apple.com")
                .audience(APPLE_BUNDLE)
                .subject("apple-sub-456")
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .claim("email", "abc123@privaterelay.appleid.com")
                .claim("email_verified", "true")
                .claim("is_private_email", "true");
    }

    private static String assinar(KeyPair kp, JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k1").build(), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) kp.getPrivate()));
        return jwt.serialize();
    }

    private static void assertRejeitado(SocialProvider provider, String token, HttpStatus esperado) {
        var ex = assertThrows(ResponseStatusException.class, () -> verifier.verificar(provider, token));
        assertEquals(esperado.value(), ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Token Google válido: extrai sub, e-mail normalizado, verificação e nome")
    void googleValido() throws Exception {
        SocialIdentity id = verifier.verificar(SocialProvider.GOOGLE, assinar(chave, claimsGoogle().build()));
        assertEquals(SocialProvider.GOOGLE, id.provider());
        assertEquals("google-sub-123", id.subject());
        assertEquals("maria@gmail.com", id.email());
        assertTrue(id.emailVerified());
        assertEquals("Maria Silva", id.nome());
    }

    @Test
    @DisplayName("Google também emite iss sem esquema (accounts.google.com): aceito")
    void googleEmissorSemEsquema() throws Exception {
        var claims = claimsGoogle().issuer("accounts.google.com").build();
        assertEquals("google-sub-123", verifier.verificar(SocialProvider.GOOGLE, assinar(chave, claims)).subject());
    }

    @Test
    @DisplayName("Audiência de qualquer client ID configurado do Google é aceita")
    void googleAudienciaAndroid() throws Exception {
        var claims = claimsGoogle().audience(GOOGLE_ANDROID).build();
        assertEquals("google-sub-123", verifier.verificar(SocialProvider.GOOGLE, assinar(chave, claims)).subject());
    }

    @Test
    @DisplayName("Token Apple válido: e-mail relay, email_verified como string e sem nome")
    void appleValido() throws Exception {
        SocialIdentity id = verifier.verificar(SocialProvider.APPLE, assinar(chave, claimsApple().build()));
        assertEquals(SocialProvider.APPLE, id.provider());
        assertEquals("apple-sub-456", id.subject());
        assertEquals("abc123@privaterelay.appleid.com", id.email());
        assertTrue(id.emailVerified());
        assertNull(id.nome());
    }

    @Test
    @DisplayName("Token Apple sem e-mail (autorizações seguintes): válido, e-mail nulo")
    void appleSemEmail() throws Exception {
        var claims = new JWTClaimsSet.Builder(claimsApple().build()).claim("email", null)
                .claim("email_verified", null).build();
        SocialIdentity id = verifier.verificar(SocialProvider.APPLE, assinar(chave, claims));
        assertNull(id.email());
        assertFalse(id.emailVerified());
    }

    @Test
    @DisplayName("Assinatura inválida (chave diferente) -> 401")
    void assinaturaInvalida() throws Exception {
        assertRejeitado(SocialProvider.GOOGLE, assinar(outraChave, claimsGoogle().build()), HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Token expirado -> 401")
    void tokenExpirado() throws Exception {
        var claims = claimsGoogle().expirationTime(Date.from(Instant.now().minusSeconds(3600))).build();
        assertRejeitado(SocialProvider.GOOGLE, assinar(chave, claims), HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Audiência incorreta -> 401")
    void audienciaIncorreta() throws Exception {
        var claims = claimsGoogle().audience("client-de-outro-app").build();
        assertRejeitado(SocialProvider.GOOGLE, assinar(chave, claims), HttpStatus.UNAUTHORIZED);
        var apple = claimsApple().audience("br.com.outro.app").build();
        assertRejeitado(SocialProvider.APPLE, assinar(chave, apple), HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Emissor incorreto -> 401")
    void emissorIncorreto() throws Exception {
        var claims = claimsGoogle().issuer("https://evil.example.com").build();
        assertRejeitado(SocialProvider.GOOGLE, assinar(chave, claims), HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Token com emissor da Apple não vale como token do Google")
    void emissorDeOutroProvedor() throws Exception {
        var claims = claimsGoogle().issuer("https://appleid.apple.com").build();
        assertRejeitado(SocialProvider.GOOGLE, assinar(chave, claims), HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Token sem sub -> 401")
    void semSubject() throws Exception {
        var claims = new JWTClaimsSet.Builder(claimsGoogle().build()).subject(null).build();
        assertRejeitado(SocialProvider.GOOGLE, assinar(chave, claims), HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Token sem assinatura (alg none) -> 401")
    void algoritmoNone() {
        String token = new PlainJWT(claimsGoogle().build()).serialize();
        assertRejeitado(SocialProvider.GOOGLE, token, HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Token assinado com HS256 (confusão de algoritmo) -> 401")
    void algoritmoHmac() throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsGoogle().build());
        jwt.sign(new MACSigner("0123456789abcdef0123456789abcdef"));
        assertRejeitado(SocialProvider.GOOGLE, jwt.serialize(), HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Token vazio, nulo, malformado ou gigante -> 401")
    void tokenMalformado() {
        assertRejeitado(SocialProvider.GOOGLE, null, HttpStatus.UNAUTHORIZED);
        assertRejeitado(SocialProvider.GOOGLE, "  ", HttpStatus.UNAUTHORIZED);
        assertRejeitado(SocialProvider.GOOGLE, "isto-nao-e-um-jwt", HttpStatus.UNAUTHORIZED);
        assertRejeitado(SocialProvider.GOOGLE, "a".repeat(9000), HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Provedor sem client ID configurado fica desabilitado -> 503 (nunca aceita qualquer audiência)")
    void provedorNaoConfigurado() throws Exception {
        var semApple = new SocialTokenVerifier(Map.of(SocialProvider.GOOGLE, SocialTokenVerifier.construirDecoder(
                decoder(chave), SocialTokenVerifier.GOOGLE_ISSUERS, Set.of(GOOGLE_WEB))));
        var ex = assertThrows(ResponseStatusException.class,
                () -> semApple.verificar(SocialProvider.APPLE, assinar(chave, claimsApple().build())));
        assertEquals(503, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Construtor de produção: lista vazia desabilita o provedor; valores são separados e limpos")
    void clientIdsConfigurados() {
        assertTrue(SocialTokenVerifier.parseClientIds(",,").isEmpty());
        assertTrue(SocialTokenVerifier.parseClientIds(null).isEmpty());
        assertEquals(Set.of("a", "b"), SocialTokenVerifier.parseClientIds(" a, ,b,a "));

        var semConfig = new SocialTokenVerifier("", "");
        var ex = assertThrows(ResponseStatusException.class,
                () -> semConfig.verificar(SocialProvider.GOOGLE, "qualquer.token.aqui"));
        assertEquals(503, ex.getStatusCode().value());
    }
}