package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProximaAcaoService {
    private final EventoSaudeRepository eventoRepository;
    private final VacinaService vacinaService;
    private final PerfilSaudeService perfilService;

    public record Acao(String id, String tipo, String prioridade, String titulo, String descricao,
                       Long eventoReferenciaId, LocalDate dataLimite, boolean podeAgendar) {}

    public List<Acao> listar(Long idPet) {
        LocalDate hoje = LocalDate.now();
        List<Acao> acoes = new ArrayList<>();
        eventoRepository.findByPet_IdPet(idPet).stream()
                .filter(e -> e.getDsStatus() == StatusEvento.AGENDADO && e.getDtEvento().isBefore(hoje))
                .forEach(e -> {
                    long dias = ChronoUnit.DAYS.between(e.getDtEvento(), hoje);
                    acoes.add(new Acao("evento-" + e.getIdEvento(), "EVENTO_ATRASADO",
                            dias > 7 ? "ALTA" : "MEDIA", e.getTipoEvento().getNmTipoEvento() + " atrasado",
                            "O evento deveria ter sido realizado em " + e.getDtEvento() + ".",
                            e.getIdEvento(), e.getDtEvento(), true));
                });
        List<VacinaPet> vacinas = vacinaService.listar(idPet);
        Set<VacinaPet> substituidas = vacinaService.substituidas(vacinas, hoje);
        vacinas.stream().filter(v -> !substituidas.contains(v)).forEach(v -> {
            StatusVacina status = vacinaService.status(v, hoje);
            if (status == StatusVacina.ATRASADA || status == StatusVacina.VENCENDO) {
                boolean atrasada = status == StatusVacina.ATRASADA;
                acoes.add(new Acao("vacina-" + v.getIdVacina(), atrasada ? "VACINA_ATRASADA" : "VACINA_VENCENDO",
                        atrasada ? "ALTA" : "MEDIA", v.getTipoVacina().getNmTipoVacina() + (atrasada ? " atrasada" : " vencendo"),
                        "A próxima dose está prevista para " + v.getDtProximaDose() + ".",
                        v.getEvento() == null ? null : v.getEvento().getIdEvento(), v.getDtProximaDose(), true));
            }
        });
        perfilService.historicoPeso(idPet).stream().findFirst().ifPresentOrElse(p -> {
            if (p.getDataMedicao().isBefore(hoje.minusDays(180))) {
                acoes.add(pesoDesatualizado(p.getDataMedicao()));
            }
        }, () -> acoes.add(pesoDesatualizado(null)));
        return acoes.stream().sorted(Comparator.comparingInt((Acao a) -> prioridade(a.prioridade()))
                .thenComparing(Acao::dataLimite, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
    }

    private int prioridade(String prioridade) {
        return switch (prioridade) {
            case "ALTA" -> 0;
            case "MEDIA" -> 1;
            default -> 2;
        };
    }

    private Acao pesoDesatualizado(LocalDate ultimaMedicao) {
        return new Acao("peso", "PESO_DESATUALIZADO", "MEDIA", "Peso desatualizado",
                ultimaMedicao == null ? "Registre o peso atual do pet." : "A última medição foi em " + ultimaMedicao + ".",
                null, ultimaMedicao, false);
    }
}