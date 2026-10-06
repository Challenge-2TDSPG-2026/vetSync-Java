package br.com.fiap.VetSync.entity;

public enum StatusLancamentoPontos {
    /** Aguardando liberação do admin. Não conta no saldo. */
    PENDENTE,
    /** Liberado e dentro da validade: entra no saldo resgatável. */
    LIBERADO,
    /** Retido pelo admin (ex.: suspeita de fraude). Não conta no saldo nem é resgatável. */
    BLOQUEADO,
    /** Estava liberado, mas a validade venceu. Derivado na leitura; não é resgatável. */
    EXPIRADO
}