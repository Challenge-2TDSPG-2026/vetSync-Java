package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.LancamentoPontos;
import br.com.fiap.VetSync.entity.Resgate;
import br.com.fiap.VetSync.entity.StatusResgate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Regra pura (sem acesso a banco) que consome os lotes de pontos liberados com os resgates do tutor. */
final class CalculoSaldoPontos {

    private CalculoSaldoPontos() {
    }

    /** Lote de pontos liberados que vai sendo consumido pelos resgates. */
    private static final class Lote {
        final LocalDate liberacao;
        final LocalDate validade; // nulo = não vence
        int restante;

        Lote(LancamentoPontos l) {
            this.liberacao = l.getDtLiberacao() != null ? l.getDtLiberacao() : l.getDtLancamento();
            this.validade = l.getDtValidade();
            this.restante = l.getNrPontos() != null ? l.getNrPontos() : 0;
        }

        boolean validoEm(LocalDate data) {
            return !liberacao.isAfter(data) && (validade == null || !validade.isBefore(data));
        }
    }

    /**
     * Regra do saldo (lançamentos e resgates devem ser do MESMO tutor e da MESMA clínica):
     * resgates VALIDADOS e PENDENTES (reservados) consomem os lotes liberados na data do resgate,
     * do que vence primeiro para o que vence depois. Assim, pontos antigos já gastos não fazem o saldo
     * ficar negativo quando vencem. O que sobra em lotes dentro da validade é o saldo disponível;
     * o que sobra em lotes vencidos é "expirado".
     * O custo de cada resgate é o congelado no momento em que ele foi feito, então editar o preço
     * da recompensa depois não altera o saldo.
     */
    static PontosService.SaldoPontos calcular(List<LancamentoPontos> lancamentos, List<Resgate> resgates, LocalDate hoje) {
        int pendentes = 0, bloqueados = 0, liberados = 0;
        List<Lote> lotes = new ArrayList<>();
        for (LancamentoPontos l : lancamentos) {
            int pontos = l.getNrPontos() != null ? l.getNrPontos() : 0;
            switch (l.statusEfetivo(hoje)) {
                case PENDENTE -> pendentes += pontos;
                case BLOQUEADO -> bloqueados += pontos;
                case LIBERADO -> { liberados += pontos; lotes.add(new Lote(l)); }
                case EXPIRADO -> lotes.add(new Lote(l));
            }
        }
        Comparator<Lote> porVencimento = Comparator
                .comparing((Lote x) -> x.validade, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(x -> x.liberacao);

        int resgatados = 0, reservados = 0;
        List<Resgate> consumidores = resgates.stream()
                .filter(r -> r.getDsStatus() == StatusResgate.VALIDADO || r.getDsStatus() == StatusResgate.PENDENTE)
                .sorted(Comparator.comparing(Resgate::getDtResgate))
                .toList();
        for (Resgate r : consumidores) {
            int custo = r.custoAplicado(); // custo congelado no momento do resgate
            if (r.getDsStatus() == StatusResgate.VALIDADO) resgatados += custo; else reservados += custo;

            LocalDate dataResgate = r.getDtResgate().toLocalDate();
            int falta = custo;
            for (Lote lote : lotes.stream().filter(x -> x.validoEm(dataResgate)).sorted(porVencimento).toList()) {
                int usa = Math.min(falta, lote.restante);
                lote.restante -= usa;
                falta -= usa;
            }
            // Dado legado (resgate sem lote válido na data): desconta do que sobrar, para não superestimar o saldo.
            for (Lote lote : lotes.stream().sorted(porVencimento).toList()) {
                if (falta == 0) break;
                int usa = Math.min(falta, lote.restante);
                lote.restante -= usa;
                falta -= usa;
            }
        }

        int disponivel = 0, expirados = 0;
        for (Lote lote : lotes) {
            if (lote.validade != null && lote.validade.isBefore(hoje)) expirados += lote.restante;
            else disponivel += lote.restante;
        }
        return new PontosService.SaldoPontos(pendentes, liberados, bloqueados, expirados, resgatados, reservados, disponivel);
    }
}
