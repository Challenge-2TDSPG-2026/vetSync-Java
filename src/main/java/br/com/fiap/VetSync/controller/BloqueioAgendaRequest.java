package br.com.fiap.VetSync.controller;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Corpo para bloquear a agenda de um profissional (veterinario ou estetica). */
public record BloqueioAgendaRequest(
        @NotNull(message = "Data de início é obrigatória")
        LocalDate dtInicio,

        @NotNull(message = "Data de fim é obrigatória")
        LocalDate dtFim,

        String motivo
) {}
