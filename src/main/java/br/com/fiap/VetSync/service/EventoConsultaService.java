package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.EventoHistorico;
import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.entity.StatusEvento;
import br.com.fiap.VetSync.entity.TipoEvento;
import br.com.fiap.VetSync.repository.EventoHistoricoRepository;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Consultas e indicadores dos eventos de saúde: listagens, histórico, gasto total e alertas de atraso. */
@Service
@RequiredArgsConstructor
public class EventoConsultaService {

    private static final long MESES_LIMITE_ATRASO = 12;

    public record AlertaEvento(
            String nmTipoEvento, LocalDate ultimaData, long mesesDesdeUltimo, boolean atrasado, String mensagem
    ) {}

    private final EventoService eventoService;
    private final EventoSaudeRepository eventoSaudeRepository;
    private final EventoHistoricoRepository eventoHistoricoRepository;

    public List<EventoSaude> listarPorPet(Long idPet) {
        return eventoSaudeRepository.findByPet_IdPet(idPet);
    }

    public List<EventoSaude> listarParaTutor(String email) {
        return eventoSaudeRepository.findVisiveisParaTutor(email);
    }

    public List<EventoSaude> listarParaVeterinario(String email) {
        return eventoSaudeRepository.findByVeterinario_DsEmailOrderByDtEventoDesc(email);
    }

    public List<EventoSaude> listarParaProfissionalEstetica(String email) {
        return eventoSaudeRepository.findByProfissionalEstetica_DsEmailOrderByDtEventoDesc(email);
    }

    public List<EventoHistorico> historico(Long idEvento) {
        eventoService.buscarPorId(idEvento);
        return eventoHistoricoRepository.findByEvento_IdEventoOrderByDtOcorrenciaDesc(idEvento);
    }

    public BigDecimal calcularGastoTotal(Long idPet) {
        return listarPorPet(idPet).stream()
                .filter(e -> e.getDsStatus() == StatusEvento.CONCLUIDO)
                .map(EventoSaude::getVlCusto)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<AlertaEvento> gerarAlertas(Long idPet) {
        List<EventoSaude> concluidos = listarPorPet(idPet).stream()
                .filter(e -> e.getDsStatus() == StatusEvento.CONCLUIDO)
                .toList();

        Map<TipoEvento, EventoSaude> maisRecentePorTipo = concluidos.stream()
                .collect(Collectors.toMap(
                        EventoSaude::getTipoEvento,
                        e -> e,
                        (e1, e2) -> e1.getDtEvento().isAfter(e2.getDtEvento()) ? e1 : e2
                ));

        List<AlertaEvento> alertas = new ArrayList<>();
        for (Map.Entry<TipoEvento, EventoSaude> entry : maisRecentePorTipo.entrySet()) {
            TipoEvento tipo = entry.getKey();
            EventoSaude ultimo = entry.getValue();
            long meses = ChronoUnit.MONTHS.between(ultimo.getDtEvento(), LocalDate.now());
            boolean atrasado = meses >= MESES_LIMITE_ATRASO;
            String mensagem = atrasado
                    ? "Último \"" + tipo.getNmTipoEvento() + "\" foi há " + meses + " meses — pode estar atrasado"
                    : "Último \"" + tipo.getNmTipoEvento() + "\" foi há " + meses + " meses — em dia";
            alertas.add(new AlertaEvento(tipo.getNmTipoEvento(), ultimo.getDtEvento(), meses, atrasado, mensagem));
        }
        alertas.sort(Comparator.comparing(AlertaEvento::atrasado).reversed());
        return alertas;
    }
}
