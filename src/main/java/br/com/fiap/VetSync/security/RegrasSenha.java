package br.com.fiap.VetSync.security;

/** Regra unica de tamanho minimo de senha, compartilhada por cadastro, troca e redefinicao. */
public final class RegrasSenha {

    public static final int TAMANHO_MINIMO = 6;

    private RegrasSenha() {
    }

    public static boolean invalida(String senha) {
        return senha == null || senha.length() < TAMANHO_MINIMO;
    }
}