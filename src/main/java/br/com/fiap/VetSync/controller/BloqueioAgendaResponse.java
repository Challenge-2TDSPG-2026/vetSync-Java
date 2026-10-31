package br.com.fiap.VetSync.controller;

import java.time.LocalDate;

public record BloqueioAgendaResponse(Long idBloqueio, LocalDate dtInicio, LocalDate dtFim, String motivo) {}
