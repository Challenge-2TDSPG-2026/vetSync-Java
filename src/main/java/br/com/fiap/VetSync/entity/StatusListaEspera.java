package br.com.fiap.VetSync.entity;

public enum StatusListaEspera {
    /** Na fila, esperando surgir uma vaga compatível. */
    AGUARDANDO,
    /** Uma vaga compatível surgiu e o tutor foi avisado. */
    NOTIFICADO,
    /** O tutor já tem um agendamento do serviço dentro do período desejado. */
    ATENDIDO,
    /** O tutor saiu da fila. */
    CANCELADO,
    /** O período desejado terminou sem agendamento. */
    EXPIRADO
}