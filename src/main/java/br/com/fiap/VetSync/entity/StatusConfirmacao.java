package br.com.fiap.VetSync.entity;

/**
 * Etapa de confirmação da clínica sobre um agendamento. É independente de {@link StatusEvento}:
 * um evento PENDENTE continua AGENDADO e, por isso, continua ocupando o horário na agenda.
 */
public enum StatusConfirmacao {
    PENDENTE,
    CONFIRMADO,
    RECUSADO
}