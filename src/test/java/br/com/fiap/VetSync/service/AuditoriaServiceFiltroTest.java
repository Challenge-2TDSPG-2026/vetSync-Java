package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Auditoria;
import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.repository.AuditoriaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuditoriaService.class)
class AuditoriaServiceFiltroTest {

    @Autowired private AuditoriaService service;
    @Autowired private AuditoriaRepository repository;
    @Autowired private EntityManager em;

    private final Clinica c7 = Clinica.builder().idClinica(7L).nmClinica("Centro").build();
    private final Clinica c8 = Clinica.builder().idClinica(8L).nmClinica("Norte").build();

    private void grava(String entidade, Long id, String acao, Clinica clinica, LocalDateTime quando) {
        repository.save(Auditoria.builder().dsEntidade(entidade).idEntidade(id).dsAcao(acao).dsAtor("a@x.com")
                .dsPerfil("ADMIN").idClinica(clinica != null ? clinica.getIdClinica() : null)
                .nmClinica(clinica != null ? clinica.getNmClinica() : null).dtOcorrencia(quando).build());
    }

    @BeforeEach
    void dados() {
        grava(AuditoriaTipos.CONTRATO, 7L, "CONTRATO_ATIVADO", c7, LocalDateTime.of(2026, 9, 1, 10, 0));
        grava(AuditoriaTipos.PONTOS, 1L, "PONTOS_LIBERADOS", c7, LocalDateTime.of(2026, 9, 10, 23, 30));
        grava(AuditoriaTipos.RESGATE, 2L, "RESGATE_VALIDADO", c8, LocalDateTime.of(2026, 9, 10, 8, 0));
        grava(AuditoriaTipos.EVENTO, 3L, "CRIADO", null, LocalDateTime.of(2026, 9, 20, 9, 0));
    }

    @Test
    @DisplayName("Filtra por clínica")
    void porClinica() {
        assertThat(service.buscar(7L, null, null, null, null, null, null)).hasSize(2)
                .allSatisfy(a -> assertThat(a.getNmClinica()).isEqualTo("Centro"));
    }

    @Test
    @DisplayName("Período é inclusivo e 'ate' cobre o dia inteiro")
    void porPeriodo() {
        List<Auditoria> r = service.buscar(null, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10), null, null, null, null);
        assertThat(r).extracting(Auditoria::getDsAcao).containsExactly("PONTOS_LIBERADOS", "RESGATE_VALIDADO");
    }

    @Test
    @DisplayName("Filtra por ação e por entidade, sem diferenciar maiúsculas")
    void porAcaoEEntidade() {
        assertThat(service.buscar(null, null, null, "resgate_validado", null, null, null)).hasSize(1);
        assertThat(service.buscar(null, null, null, null, "pontos", 1L, null)).hasSize(1);
        assertThat(service.buscar(7L, null, null, null, "RESGATE", null, null)).isEmpty();
    }

    @Test
    @DisplayName("Sem filtros devolve tudo, mais recente primeiro, respeitando o limite")
    void semFiltrosELimite() {
        assertThat(service.buscar(null, null, null, null, null, null, null)).extracting(Auditoria::getDsAcao)
                .containsExactly("CRIADO", "PONTOS_LIBERADOS", "RESGATE_VALIDADO", "CONTRATO_ATIVADO");
        assertThat(service.buscar(null, null, null, null, null, null, 2)).hasSize(2);
    }

    @Test
    @DisplayName("Registro é append-only: alterar a entidade não altera a linha no banco")
    void imutavel() {
        Auditoria a = repository.findAll().get(0);
        String acaoOriginal = a.getDsAcao();
        a.setDsAcao("ADULTERADA");
        em.flush();
        em.clear();
        assertThat(repository.findById(a.getIdAuditoria()).orElseThrow().getDsAcao()).isEqualTo(acaoOriginal);
    }

    @Test
    @DisplayName("registrar grava clínica (id e nome da época) junto do ator")
    void registraClinica() {
        Auditoria a = service.registrar("CONTRATO", 7L, "CONTRATO_ATIVADO", "adm@x.com", "ADMIN", c7, "INATIVO", "ATIVO", null);
        assertThat(a.getIdClinica()).isEqualTo(7L);
        assertThat(a.getNmClinica()).isEqualTo("Centro");
    }
}