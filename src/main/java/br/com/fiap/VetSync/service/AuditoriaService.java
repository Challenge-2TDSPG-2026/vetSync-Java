package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Auditoria;
import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.repository.AuditoriaRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Auditoria append-only: só existe inserção e consulta. Complementa (não substitui) os registros do domínio.
 * O registro entra na MESMA transação da ação auditada: se a ação falhar e der rollback, o registro some junto.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {
    private static final int LIMITE_PADRAO = 200;
    private static final int LIMITE_MAXIMO = 1000;

    private final AuditoriaRepository repository;

    /** Assinatura legada (sem clínica). */
    @Transactional
    public Auditoria registrar(String entidade, Long idEntidade, String acao, String ator,
                               String perfil, String anterior, String novo, String ip) {
        return registrar(entidade, idEntidade, acao, ator, perfil, null, anterior, novo, ip);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Auditoria registrar(String entidade, Long idEntidade, String acao, String ator, String perfil,
                               Clinica clinica, String anterior, String novo, String ip) {
        return repository.save(Auditoria.builder().dsEntidade(entidade).idEntidade(idEntidade)
                .dsAcao(acao).dsAtor(ator).dsPerfil(perfil)
                .idClinica(clinica != null ? clinica.getIdClinica() : null)
                .nmClinica(clinica != null ? clinica.getNmClinica() : null)
                .dsValorAnterior(anterior).dsValorNovo(novo).dsIp(ip)
                .dtOcorrencia(LocalDateTime.now()).build());
    }

    /**
     * Registra uma ação no contexto de uma clínica, descobrindo quem agiu (e o IP) pela requisição em curso.
     * Fora de uma requisição autenticada (ex.: cadastro público), o ator cai em {@code atorPadrao}/SISTEMA.
     */
    @Transactional
    public Auditoria registrarAcao(String entidade, Long idEntidade, String acao, Clinica clinica,
                                   String anterior, String novo, String atorPadrao, String perfilPadrao) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean autenticado = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        String ator = autenticado ? auth.getName() : (atorPadrao != null ? atorPadrao : "SISTEMA");
        String perfil = autenticado ? perfilDe(auth) : (perfilPadrao != null ? perfilPadrao : "SISTEMA");
        return registrar(entidade, idEntidade, acao, ator, perfil, clinica, anterior, novo, ipDaRequisicao());
    }

    @Transactional(readOnly = true)
    public List<Auditoria> listar(String entidade, Long idEntidade) {
        return repository.findByDsEntidadeAndIdEntidadeOrderByDtOcorrenciaDesc(entidade, idEntidade);
    }

    /** Consulta com filtros opcionais, mais recentes primeiro. {@code ate} é inclusivo (o dia inteiro). */
    @Transactional(readOnly = true)
    public List<Auditoria> buscar(Long idClinica, LocalDate de, LocalDate ate, String acao,
                                  String entidade, Long idEntidade, Integer limite) {
        Specification<Auditoria> spec = (root, query, cb) -> {
            List<Predicate> f = new ArrayList<>();
            if (idClinica != null) f.add(cb.equal(root.get("idClinica"), idClinica));
            if (de != null) f.add(cb.greaterThanOrEqualTo(root.get("dtOcorrencia"), de.atStartOfDay()));
            if (ate != null) f.add(cb.lessThan(root.get("dtOcorrencia"), ate.plusDays(1).atStartOfDay()));
            if (acao != null && !acao.isBlank()) f.add(cb.equal(root.get("dsAcao"), acao.trim().toUpperCase()));
            if (entidade != null && !entidade.isBlank()) f.add(cb.equal(root.get("dsEntidade"), entidade.trim().toUpperCase()));
            if (idEntidade != null) f.add(cb.equal(root.get("idEntidade"), idEntidade));
            return cb.and(f.toArray(new Predicate[0]));
        };
        int tamanho = limite == null ? LIMITE_PADRAO : Math.max(1, Math.min(limite, LIMITE_MAXIMO));
        Sort ordem = Sort.by(Sort.Direction.DESC, "dtOcorrencia").and(Sort.by(Sort.Direction.DESC, "idAuditoria"));
        return repository.findAll(spec, PageRequest.of(0, tamanho, ordem)).getContent();
    }

    private static String perfilDe(Authentication auth) {
        return auth.getAuthorities().stream().map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5)).findFirst().orElse("SISTEMA");
    }

    private static String ipDaRequisicao() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            String forwarded = attrs.getRequest().getHeader("X-Forwarded-For");
            String ip = forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : attrs.getRequest().getRemoteAddr();
            return ip != null && ip.length() > 64 ? ip.substring(0, 64) : ip;
        }
        return null;
    }
}