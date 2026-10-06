package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.repository.LancamentoPontosRepository;
import br.com.fiap.VetSync.repository.ResgateRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PontosService {

    private final LancamentoPontosRepository lancamentoPontosRepository;
    private final AdminRepository adminRepository;
    private final ResgateRepository resgateRepository;

    /** Quantos dias os pontos valem a partir da liberação. */
    @Value("${app.pontos.validade-dias:365}")
    private int validadeDias = 365;

    /**
     * Saldo de UM tutor em UMA clínica. Pontos de clínicas diferentes nunca são somados.
     *
     * @param pontosPendentes  aguardando liberação (não contam no saldo)
     * @param pontosLiberados  total de lançamentos liberados e dentro da validade (antes de descontar resgates)
     * @param pontosBloqueados retidos pelo admin (não contam no saldo)
     * @param pontosExpirados  pontos liberados que venceram sem terem sido usados
     * @param pontosResgatados custo dos resgates já VALIDADOS
     * @param pontosReservados custo dos resgates PENDENTES: ficam reservados até o veterinário validar ou negar
     * @param saldoDisponivel  o que o tutor pode resgatar agora (nunca inclui pendente, bloqueado, expirado ou reservado)
     */
    public record SaldoPontos(int pontosPendentes, int pontosLiberados, int pontosBloqueados, int pontosExpirados,
                              int pontosResgatados, int pontosReservados, int saldoDisponivel) {
        public static SaldoPontos vazio() {
            return new SaldoPontos(0, 0, 0, 0, 0, 0, 0);
        }
    }

    public record SaldoTutorClinica(Tutor tutor, Clinica clinica, SaldoPontos saldo) {}

    /**
     * Chamado quando um evento vira CONCLUIDO. Cria o lançamento PENDENTE
     * com os pontos do tipo de evento — não credita nada ainda, só entra na
     * fila de liberação do admin.
     */
    public LancamentoPontos lancarPendente(EventoSaude evento) {
        int pontos = evento.getTipoEvento() != null && evento.getTipoEvento().getNrPontos() != null
                ? evento.getTipoEvento().getNrPontos() : 0;
        LancamentoPontos lancamento = LancamentoPontos.builder()
                .evento(evento)
                .clinica(evento.getClinica())
                .nrPontos(pontos)
                .dsStatus(StatusLancamentoPontos.PENDENTE)
                .build();
        return lancamentoPontosRepository.save(lancamento);
    }


    public LancamentoPontos lancarBonusPendente(PlanoTratamento plano) {
        int bonus = plano.getNrPontosBonus() != null ? plano.getNrPontosBonus() : 0;
        LancamentoPontos lancamento = LancamentoPontos.builder()
                .planoTratamento(plano)
                .clinica(plano.getClinica())
                .nrPontos(bonus)
                .dsStatus(StatusLancamentoPontos.PENDENTE)
                .build();
        return lancamentoPontosRepository.save(lancamento);
    }

    public LancamentoPontos buscarPorId(Long id) {
        return lancamentoPontosRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lançamento de pontos não encontrado com id: " + id));
    }

    // ------------------------------------------------------------------ listagem

    /**
     * Visão do admin: todos os lançamentos, com filtros opcionais por clínica, status e tutor.
     * O status EXPIRADO é derivado (LIBERADO com validade vencida).
     */
    @Transactional(readOnly = true)
    public List<LancamentoPontos> listar(Long idClinica, StatusLancamentoPontos status, Long idTutor) {
        return filtrarPorStatus(buscar(idClinica, idTutor, null), status);
    }

    /** Visão do tutor: só os próprios lançamentos, com filtros opcionais por clínica e status. */
    @Transactional(readOnly = true)
    public List<LancamentoPontos> listarParaTutor(String email, Long idClinica, StatusLancamentoPontos status) {
        return filtrarPorStatus(buscar(idClinica, null, email), status);
    }

    private List<LancamentoPontos> filtrarPorStatus(List<LancamentoPontos> lancamentos, StatusLancamentoPontos status) {
        if (status == null) return lancamentos;
        LocalDate hoje = LocalDate.now();
        return lancamentos.stream().filter(l -> l.statusEfetivo(hoje) == status).toList();
    }

    private List<LancamentoPontos> buscar(Long idClinica, Long idTutor, String emailTutor) {
        Specification<LancamentoPontos> spec = (root, query, cb) -> {
            Join<Object, Object> evento = root.join("evento", JoinType.LEFT);
            Join<Object, Object> plano = root.join("planoTratamento", JoinType.LEFT);
            Join<Object, Object> tutorEvento = evento.join("pet", JoinType.LEFT).join("tutor", JoinType.LEFT);
            Join<Object, Object> tutorPlano = plano.join("pet", JoinType.LEFT).join("tutor", JoinType.LEFT);

            List<Predicate> filtros = new ArrayList<>();
            if (idClinica != null) {
                filtros.add(cb.equal(root.get("clinica").get("idClinica"), idClinica));
            }
            if (idTutor != null) {
                filtros.add(cb.or(cb.equal(tutorEvento.get("idTutor"), idTutor),
                        cb.equal(tutorPlano.get("idTutor"), idTutor)));
            }
            if (emailTutor != null) {
                filtros.add(cb.or(cb.equal(tutorEvento.get("dsEmail"), emailTutor),
                        cb.equal(tutorPlano.get("dsEmail"), emailTutor)));
            }
            return cb.and(filtros.toArray(new Predicate[0]));
        };
        return lancamentoPontosRepository.findAll(spec,
                Sort.by(Sort.Direction.DESC, "dtLancamento").and(Sort.by(Sort.Direction.DESC, "idLancamento")));
    }

    // ------------------------------------------------------------------ ações do admin

    /** Libera um lançamento PENDENTE da clínica informada e define a validade a partir de hoje. */
    @Transactional
    public LancamentoPontos liberar(Long idLancamento, Long idClinica, Long idAdmin) {
        LancamentoPontos lancamento = buscarDaClinica(idLancamento, idClinica);
        if (lancamento.getDsStatus() == StatusLancamentoPontos.BLOQUEADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse lançamento está BLOQUEADO. Desbloqueie antes de liberar");
        }
        if (lancamento.getDsStatus() != StatusLancamentoPontos.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse lançamento já está " + lancamento.statusEfetivo(LocalDate.now()));
        }
        Admin admin = buscarAdmin(idAdmin);

        LocalDate hoje = LocalDate.now();
        lancamento.setAdminValidador(admin);
        lancamento.setDsStatus(StatusLancamentoPontos.LIBERADO);
        lancamento.setDtLiberacao(hoje);
        lancamento.setDtValidade(hoje.plusDays(validadeDias));
        return lancamentoPontosRepository.save(lancamento);
    }

    /**
     * Bloqueia um lançamento PENDENTE ou LIBERADO (ainda válido) da clínica informada.
     * Se os pontos já foram gastos em resgates, o bloqueio é recusado para não deixar o saldo negativo.
     */
    @Transactional
    public LancamentoPontos bloquear(Long idLancamento, Long idClinica, String motivo, Long idAdmin) {
        if (motivo == null || motivo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motivo do bloqueio é obrigatório");
        }
        LancamentoPontos lancamento = buscarDaClinica(idLancamento, idClinica);
        LocalDate hoje = LocalDate.now();
        StatusLancamentoPontos atual = lancamento.statusEfetivo(hoje);
        if (atual != StatusLancamentoPontos.PENDENTE && atual != StatusLancamentoPontos.LIBERADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse lançamento já está " + atual);
        }
        Admin admin = buscarAdmin(idAdmin);

        if (atual == StatusLancamentoPontos.LIBERADO) {
            Tutor tutor = lancamento.tutorOrigem();
            if (tutor != null) {
                int disponivel = calcularSaldo(tutor.getIdTutor(), idClinica).saldoDisponivel();
                if (disponivel < lancamento.getNrPontos()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Esses pontos já foram usados em resgates e não podem ser bloqueados");
                }
            }
        }

        lancamento.setDsStatus(StatusLancamentoPontos.BLOQUEADO);
        lancamento.setDtBloqueio(hoje);
        lancamento.setDsMotivoBloqueio(motivo.trim());
        lancamento.setAdminBloqueio(admin);
        return lancamentoPontosRepository.save(lancamento);
    }

    /** Desbloqueia: volta a PENDENTE se nunca foi liberado, ou a LIBERADO (respeitando a validade original). */
    @Transactional
    public LancamentoPontos desbloquear(Long idLancamento, Long idClinica, Long idAdmin) {
        LancamentoPontos lancamento = buscarDaClinica(idLancamento, idClinica);
        if (lancamento.getDsStatus() != StatusLancamentoPontos.BLOQUEADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse lançamento não está bloqueado (status " + lancamento.statusEfetivo(LocalDate.now()) + ")");
        }
        buscarAdmin(idAdmin);

        lancamento.setDsStatus(lancamento.getDtLiberacao() != null
                ? StatusLancamentoPontos.LIBERADO : StatusLancamentoPontos.PENDENTE);
        lancamento.setDtBloqueio(null);
        lancamento.setDsMotivoBloqueio(null);
        lancamento.setAdminBloqueio(null);
        return lancamentoPontosRepository.save(lancamento);
    }

    private LancamentoPontos buscarDaClinica(Long idLancamento, Long idClinica) {
        LancamentoPontos lancamento = buscarPorId(idLancamento);
        if (lancamento.getClinica() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse lançamento não está associado a nenhuma clínica");
        }
        if (!lancamento.getClinica().getIdClinica().equals(idClinica)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Lançamento " + idLancamento + " não pertence à clínica " + idClinica);
        }
        return lancamento;
    }

    private Admin buscarAdmin(Long idAdmin) {
        return adminRepository.findById(idAdmin).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin não encontrado"));
    }

    // ------------------------------------------------------------------ saldo

    /** Pontos liberados e ainda dentro da validade do tutor SOMENTE na clínica informada (antes de descontar resgates). */
    public int calcularPontosLiberados(Long idTutor, Long idClinica) {
        LocalDate hoje = LocalDate.now();
        return buscar(idClinica, idTutor, null).stream()
                .filter(l -> l.statusEfetivo(hoje) == StatusLancamentoPontos.LIBERADO)
                .mapToInt(LancamentoPontos::getNrPontos).sum();
    }

    /** Saldo do tutor na clínica informada. Pontos e resgates de outras clínicas não entram na conta. */
    @Transactional(readOnly = true)
    public SaldoPontos calcularSaldo(Long idTutor, Long idClinica) {
        return calcular(buscar(idClinica, idTutor, null), resgatesDe(idTutor, idClinica), LocalDate.now());
    }

    /**
     * Um saldo por par (tutor, clínica), nunca somando clínicas. Admin pode filtrar por clínica e tutor;
     * o tutor passa o próprio id. Lançamentos sem clínica não aparecem (não são resgatáveis em lugar nenhum).
     */
    @Transactional(readOnly = true)
    public List<SaldoTutorClinica> listarSaldos(Long idClinica, Long idTutor) {
        Map<String, List<LancamentoPontos>> grupos = new LinkedHashMap<>();
        for (LancamentoPontos l : buscar(idClinica, idTutor, null)) {
            Tutor tutor = l.tutorOrigem();
            if (l.getClinica() == null || tutor == null) continue;
            grupos.computeIfAbsent(tutor.getIdTutor() + ":" + l.getClinica().getIdClinica(), k -> new ArrayList<>()).add(l);
        }
        LocalDate hoje = LocalDate.now();
        List<SaldoTutorClinica> saldos = new ArrayList<>();
        for (List<LancamentoPontos> grupo : grupos.values()) {
            Tutor tutor = grupo.get(0).tutorOrigem();
            Clinica clinica = grupo.get(0).getClinica();
            saldos.add(new SaldoTutorClinica(tutor, clinica,
                    calcular(grupo, resgatesDe(tutor.getIdTutor(), clinica.getIdClinica()), hoje)));
        }
        saldos.sort(Comparator.comparing((SaldoTutorClinica s) -> s.tutor().getNmTutor(), Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(s -> s.clinica().getNmClinica(), Comparator.nullsLast(String::compareToIgnoreCase)));
        return saldos;
    }

    /** Resgates do tutor na clínica em que foram feitos (clínica congelada no resgate, não a atual da recompensa). */
    private List<Resgate> resgatesDe(Long idTutor, Long idClinica) {
        List<Resgate> resgates = resgateRepository
                .findByTutor_IdTutorAndClinica_IdClinicaOrderByDtResgateDesc(idTutor, idClinica);
        return resgates != null ? resgates : List.of();
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
    static SaldoPontos calcular(List<LancamentoPontos> lancamentos, List<Resgate> resgates, LocalDate hoje) {
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
        return new SaldoPontos(pendentes, liberados, bloqueados, expirados, resgatados, reservados, disponivel);
    }
}