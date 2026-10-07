package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.entity.LancamentoPontos;
import br.com.fiap.VetSync.entity.StatusLancamentoPontos;
import br.com.fiap.VetSync.entity.StatusResgate;
import br.com.fiap.VetSync.repository.ClinicaRepository;
import br.com.fiap.VetSync.repository.RecompensaRepository;
import br.com.fiap.VetSync.repository.ResgateRepository;
import br.com.fiap.VetSync.service.PontosService.SaldoTutorClinica;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Indicadores do painel inicial do admin. Com {@code idClinica} os números são só daquela clínica;
 * sem ele são os totais de TODAS as clínicas (a resposta marca isso em {@code totaisGlobais} e no
 * {@code rotuloEscopo}). Os pontos disponíveis vêm do saldo resgatável de cada tutor em cada clínica,
 * então nunca incluem pontos pendentes, bloqueados, vencidos ou reservados por resgates pendentes.
 */
@Service
@RequiredArgsConstructor
public class PainelAdminService {

    public static final String ESCOPO_CLINICA = "CLINICA";
    public static final String ESCOPO_GLOBAL = "GLOBAL";
    public static final String ROTULO_GLOBAL = "Totais de todas as clínicas";

    private final PontosService pontosService;
    private final ClinicaService clinicaService;
    private final ClinicaRepository clinicaRepository;
    private final RecompensaRepository recompensaRepository;
    private final ResgateRepository resgateRepository;

    /**
     * @param pontosPendentes     aguardando liberação (não disponíveis)
     * @param pontosBloqueados    retidos pelo admin (não disponíveis)
     * @param pontosExpirados     vencidos e não usados (não disponíveis)
     * @param pontosLiberados     liberados e dentro da validade, antes de descontar resgates
     * @param pontosResgatados    custo dos resgates já validados
     * @param pontosReservados    custo dos resgates pendentes de validação (não disponíveis)
     * @param pontosDisponiveis   soma do que os tutores podem resgatar agora
     * @param lancamentosPendentes  quantidade de lançamentos aguardando liberação
     * @param lancamentosBloqueados quantidade de lançamentos bloqueados
     */
    public record PontosPainel(int pontosPendentes, int pontosBloqueados, int pontosExpirados, int pontosLiberados,
                               int pontosResgatados, int pontosReservados, int pontosDisponiveis,
                               int lancamentosPendentes, int lancamentosBloqueados) {}

    public record RecompensasPainel(long recompensasAtivas, long recompensasInativas, long recompensasTotal,
                                    long resgatesPendentes, long resgatesValidados, long resgatesNegados) {}

    public record PontosPorClinica(Long idClinica, String nmClinica, PontosPainel pontos) {}

    /**
     * @param escopo        CLINICA (filtrado) ou GLOBAL (todas as clínicas somadas)
     * @param totaisGlobais true quando os números somam todas as clínicas
     * @param rotuloEscopo  texto pronto para exibir junto aos indicadores
     * @param porClinica    detalhamento por clínica; só vem no escopo GLOBAL (vazio no escopo CLINICA)
     */
    public record ResumoPainel(String escopo, boolean totaisGlobais, Long idClinica, String nmClinica,
                               String rotuloEscopo, PontosPainel pontos, RecompensasPainel recompensas,
                               List<PontosPorClinica> porClinica) {}

    @Transactional(readOnly = true)
    public ResumoPainel resumo(Long idClinica) {
        Clinica clinica = idClinica != null ? clinicaService.buscarObrigatoria(idClinica) : null;
        LocalDate hoje = LocalDate.now();

        List<SaldoTutorClinica> saldos = pontosService.listarSaldos(idClinica, null);
        // Lançamentos sem clínica não são resgatáveis em lugar nenhum e ficam fora de qualquer total.
        List<LancamentoPontos> lancamentos = pontosService.listar(idClinica, null, null).stream()
                .filter(l -> l.getClinica() != null).toList();

        PontosPainel pontos = pontosPainel(saldos, lancamentos, hoje);
        RecompensasPainel recompensas = recompensasPainel(idClinica);

        if (clinica != null) {
            return new ResumoPainel(ESCOPO_CLINICA, false, clinica.getIdClinica(), clinica.getNmClinica(),
                    clinica.getNmClinica(), pontos, recompensas, List.of());
        }
        return new ResumoPainel(ESCOPO_GLOBAL, true, null, null, ROTULO_GLOBAL, pontos, recompensas,
                porClinica(saldos, lancamentos, hoje));
    }

    private List<PontosPorClinica> porClinica(List<SaldoTutorClinica> saldos, List<LancamentoPontos> lancamentos,
                                              LocalDate hoje) {
        Map<Long, List<SaldoTutorClinica>> saldosPorClinica = saldos.stream()
                .collect(Collectors.groupingBy(s -> s.clinica().getIdClinica()));
        Map<Long, List<LancamentoPontos>> lancamentosPorClinica = lancamentos.stream()
                .collect(Collectors.groupingBy(l -> l.getClinica().getIdClinica()));

        List<PontosPorClinica> resultado = new ArrayList<>();
        for (Clinica c : clinicaRepository.findAll()) {
            resultado.add(new PontosPorClinica(c.getIdClinica(), c.getNmClinica(),
                    pontosPainel(saldosPorClinica.getOrDefault(c.getIdClinica(), List.of()),
                            lancamentosPorClinica.getOrDefault(c.getIdClinica(), List.of()), hoje)));
        }
        resultado.sort(Comparator.comparing(PontosPorClinica::nmClinica, Comparator.nullsLast(String::compareToIgnoreCase)));
        return resultado;
    }

    private PontosPainel pontosPainel(List<SaldoTutorClinica> saldos, List<LancamentoPontos> lancamentos, LocalDate hoje) {
        int pendentes = 0, bloqueados = 0, expirados = 0, liberados = 0, resgatados = 0, reservados = 0, disponiveis = 0;
        for (SaldoTutorClinica s : saldos) {
            var p = s.saldo();
            pendentes += p.pontosPendentes();
            bloqueados += p.pontosBloqueados();
            expirados += p.pontosExpirados();
            liberados += p.pontosLiberados();
            resgatados += p.pontosResgatados();
            reservados += p.pontosReservados();
            disponiveis += p.saldoDisponivel();
        }
        int qtdPendentes = 0, qtdBloqueados = 0;
        for (LancamentoPontos l : lancamentos) {
            StatusLancamentoPontos status = l.statusEfetivo(hoje);
            if (status == StatusLancamentoPontos.PENDENTE) qtdPendentes++;
            else if (status == StatusLancamentoPontos.BLOQUEADO) qtdBloqueados++;
        }
        return new PontosPainel(pendentes, bloqueados, expirados, liberados, resgatados, reservados, disponiveis,
                qtdPendentes, qtdBloqueados);
    }

    private RecompensasPainel recompensasPainel(Long idClinica) {
        long ativas, inativas, pendentes, validados, negados;
        if (idClinica == null) {
            ativas = recompensaRepository.countByFlAtivo(true);
            inativas = recompensaRepository.countByFlAtivo(false);
            pendentes = resgateRepository.countByDsStatus(StatusResgate.PENDENTE);
            validados = resgateRepository.countByDsStatus(StatusResgate.VALIDADO);
            negados = resgateRepository.countByDsStatus(StatusResgate.NEGADO);
        } else {
            ativas = recompensaRepository.countByFlAtivoAndClinica_IdClinica(true, idClinica);
            inativas = recompensaRepository.countByFlAtivoAndClinica_IdClinica(false, idClinica);
            pendentes = resgateRepository.countByDsStatusAndClinica_IdClinica(StatusResgate.PENDENTE, idClinica);
            validados = resgateRepository.countByDsStatusAndClinica_IdClinica(StatusResgate.VALIDADO, idClinica);
            negados = resgateRepository.countByDsStatusAndClinica_IdClinica(StatusResgate.NEGADO, idClinica);
        }
        return new RecompensasPainel(ativas, inativas, ativas + inativas, pendentes, validados, negados);
    }
}