package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.StatusEvento;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RelatorioClinicaService {
    private final EventoSaudeRepository eventos;
    private final VeterinarioRepository veterinarios;

    public Resumo gerar(String email, LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Intervalo de datas inválido");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) > 366) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O intervalo máximo é de 366 dias");
        }
        var vet = veterinarios.findByDsEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Veterinário não encontrado"));
        var lista = eventos.findByVeterinario_DsEmailOrderByDtEventoDesc(email).stream()
                .filter(e -> !e.getDtEvento().isBefore(inicio) && !e.getDtEvento().isAfter(fim))
                .toList();
        long agendadas = lista.stream().filter(e -> e.getDsStatus() == StatusEvento.AGENDADO).count();
        long concluidas = lista.stream().filter(e -> e.getDsStatus() == StatusEvento.CONCLUIDO).count();
        long canceladas = lista.stream().filter(e -> e.getDsStatus() == StatusEvento.CANCELADO).count();
        BigDecimal faturamento = lista.stream()
                .filter(e -> e.getDsStatus() == StatusEvento.CONCLUIDO)
                .map(e -> e.getVlCusto() == null ? BigDecimal.ZERO : e.getVlCusto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pacientes = lista.stream().filter(e -> e.getPet() != null)
                .map(e -> e.getPet().getIdPet()).filter(Objects::nonNull).distinct().count();
        long vacinas = lista.stream().filter(e -> e.getTipoEvento() != null
                && e.getTipoEvento().getNmTipoEvento() != null
                && e.getTipoEvento().getNmTipoEvento().toLowerCase(Locale.ROOT).contains("vacina")).count();
        return new Resumo(inicio, fim, agendadas, concluidas, canceladas, faturamento, pacientes, vacinas,
                vet.getClinica() == null ? null : vet.getClinica().getNmClinica());
    }

    public record Resumo(LocalDate inicio, LocalDate fim, long consultasAgendadas, long consultasConcluidas,
                          long cancelamentos, BigDecimal faturamento, long pacientesAtendidos,
                          long vacinasAplicadas, String clinica) {}
}
