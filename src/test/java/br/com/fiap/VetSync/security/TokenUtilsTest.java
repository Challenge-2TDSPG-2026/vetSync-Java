package br.com.fiap.VetSync.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenUtilsTest {

    @Test
    @DisplayName("gerarTokenUrlSeguro - 32 bytes viram 43 caracteres Base64 URL-safe sem padding")
    void gerarTokenUrlSeguro_32Bytes_Retorna43CaracteresUrlSafe() {
        String token = TokenUtils.gerarTokenUrlSeguro(32);

        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    @DisplayName("gerarTokenUrlSeguro - duas chamadas não repetem o valor")
    void gerarTokenUrlSeguro_DuasChamadas_RetornaValoresDiferentes() {
        assertThat(TokenUtils.gerarTokenUrlSeguro(32)).isNotEqualTo(TokenUtils.gerarTokenUrlSeguro(32));
    }

    @Test
    @DisplayName("gerarSenhaTemporaria - respeita o tamanho e evita caracteres ambíguos")
    void gerarSenhaTemporaria_Tamanho10_SemCaracteresAmbiguos() {
        String senha = TokenUtils.gerarSenhaTemporaria(10);

        assertThat(senha).hasSize(10).matches("[A-HJ-NP-Za-km-z2-9]{10}");
    }

    @Test
    @DisplayName("gerarCodigo - respeita o tamanho e usa só maiúsculas e dígitos sem ambiguidade")
    void gerarCodigo_Tamanho18_SomenteMaiusculasEDigitos() {
        String codigo = TokenUtils.gerarCodigo(18);

        assertThat(codigo).hasSize(18).matches("[A-HJ-NP-Z2-9]{18}");
    }

    @Test
    @DisplayName("inteiroAleatorio - sempre dentro de [0, limite)")
    void inteiroAleatorio_VariasChamadas_FicaDentroDoLimite() {
        for (int i = 0; i < 1000; i++) {
            assertThat(TokenUtils.inteiroAleatorio(900_000)).isBetween(0, 899_999);
        }
    }

    @Test
    @DisplayName("sha256Hex - vetor conhecido de SHA-256 em hexadecimal minúsculo")
    void sha256Hex_Abc_RetornaVetorConhecido() {
        assertThat(TokenUtils.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
