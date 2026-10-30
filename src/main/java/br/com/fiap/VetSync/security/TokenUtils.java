package br.com.fiap.VetSync.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Ponto único para gerar valores aleatórios seguros (tokens, senhas temporárias, códigos) e
 * calcular o SHA-256 usado para guardar tokens no banco apenas como hash.
 */
public final class TokenUtils {

    /** Sem caracteres ambíguos (I, l, O, 0, 1) para a senha poder ser digitada a partir do e-mail. */
    private static final String ALFABETO_SENHA = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final String ALFABETO_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenUtils() {
    }

    /** Token aleatório em Base64 URL-safe, sem padding, a partir de {@code bytes} bytes de entropia. */
    public static String gerarTokenUrlSeguro(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    /** Senha temporária (letras maiúsculas/minúsculas e dígitos, sem caracteres ambíguos). */
    public static String gerarSenhaTemporaria(int tamanho) {
        return sortear(ALFABETO_SENHA, tamanho);
    }

    /** Código alfanumérico em maiúsculas (sem caracteres ambíguos), como os códigos de vínculo com a clínica. */
    public static String gerarCodigo(int tamanho) {
        return sortear(ALFABETO_CODIGO, tamanho);
    }

    /** Inteiro aleatório no intervalo [0, limiteExclusivo). */
    public static int inteiroAleatorio(int limiteExclusivo) {
        return RANDOM.nextInt(limiteExclusivo);
    }

    /** SHA-256 do valor (UTF-8) em hexadecimal minúsculo. */
    public static String sha256Hex(String valor) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível", e);
        }
    }

    private static String sortear(String alfabeto, int tamanho) {
        StringBuilder valor = new StringBuilder(tamanho);
        for (int i = 0; i < tamanho; i++) {
            valor.append(alfabeto.charAt(RANDOM.nextInt(alfabeto.length())));
        }
        return valor.toString();
    }
}
