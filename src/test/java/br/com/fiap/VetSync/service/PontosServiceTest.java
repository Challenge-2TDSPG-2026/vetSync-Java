package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.repository.LancamentoPontosRepository;
import br.com.fiap.VetSync.repository.ResgateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PontosServiceTest {
    @Mock
    private LancamentoPontosRepository lancamentoPontosRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private ResgateRepository resgateRepository;

    @InjectMocks
    private PontosService pontosService;

    @Test
    @DisplayName("Deve lançar pontos pendentes a partir de evento concluído")
    void lancarPendente_Evento() {
        TipoEvento tipo = TipoEvento.builder().nrPontos(25).build();
        Clinica clinica = Clinica.builder().idClinica(7L).nmClinica("Clínica Centro").build();
        EventoSaude evento = EventoSaude.builder().idEvento(1L).tipoEvento(tipo).clinica(clinica).build();

        when(lancamentoPontosRepository.save(any(LancamentoPontos.class))).thenAnswer(inv -> inv.getArgument(0));

        LancamentoPontos lanc = pontosService.lancarPendente(evento);
        assertThat(lanc.getNrPontos()).isEqualTo(25);
        assertThat(lanc.getDsStatus()).isEqualTo(StatusLancamentoPontos.PENDENTE);
        assertThat(lanc.getEvento()).isEqualTo(evento);
        assertThat(lanc.getClinica()).isEqualTo(clinica); // pontos nascem na clínica do atendimento
    }

    @Test
    @DisplayName("Deve lançar bônus pendente a partir de plano de tratamento concluído")
    void lancarBonusPendente_Plano() {
        Clinica clinica = Clinica.builder().idClinica(7L).nmClinica("Clínica Centro").build();
        PlanoTratamento plano = PlanoTratamento.builder().idPlano(1L).nrPontosBonus(100).clinica(clinica).build();
        when(lancamentoPontosRepository.save(any(LancamentoPontos.class))).thenAnswer(inv -> inv.getArgument(0));

        LancamentoPontos lanc = pontosService.lancarBonusPendente(plano);
        assertThat(lanc.getNrPontos()).isEqualTo(100);
        assertThat(lanc.getDsStatus()).isEqualTo(StatusLancamentoPontos.PENDENTE);
        assertThat(lanc.getPlanoTratamento()).isEqualTo(plano);
        assertThat(lanc.getClinica()).isEqualTo(clinica); // bônus fica na clínica do plano
    }

    private final Clinica clinica7 = Clinica.builder().idClinica(7L).nmClinica("Clínica Centro").build();
    private final LocalDate hoje = LocalDate.now();

    private LancamentoPontos lancamento(StatusLancamentoPontos status, int pontos, LocalDate liberacao, LocalDate validade) {
        return LancamentoPontos.builder().idLancamento(10L).clinica(clinica7).nrPontos(pontos)
                .dsStatus(status).dtLiberacao(liberacao).dtValidade(validade).build();
    }

    private Resgate resgate(StatusResgate status, int custo, LocalDate data) {
        return Resgate.builder().dsStatus(status).dtResgate(data.atStartOfDay())
                .recompensa(Recompensa.builder().nrCustoPontos(custo).build()).build();
    }

    @Test
    @DisplayName("Liberar define liberação, validade (365 dias) e o admin validador")
    void liberar_Sucesso() {
        LancamentoPontos lanc = lancamento(StatusLancamentoPontos.PENDENTE, 20, null, null);
        Admin admin = Admin.builder().idAdmin(2L).build();

        when(lancamentoPontosRepository.findById(10L)).thenReturn(Optional.of(lanc));
        when(adminRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(lancamentoPontosRepository.save(any(LancamentoPontos.class))).thenAnswer(inv -> inv.getArgument(0));

        LancamentoPontos liberado = pontosService.liberar(10L, 7L, 2L);
        assertThat(liberado.getDsStatus()).isEqualTo(StatusLancamentoPontos.LIBERADO);
        assertThat(liberado.getAdminValidador()).isEqualTo(admin);
        assertThat(liberado.getDtLiberacao()).isEqualTo(hoje);
        assertThat(liberado.getDtValidade()).isEqualTo(hoje.plusDays(365));
    }

    @Test
    @DisplayName("Liberar com clínica diferente da do lançamento retorna 404 e não grava")
    void liberar_OutraClinica() {
        when(lancamentoPontosRepository.findById(10L))
                .thenReturn(Optional.of(lancamento(StatusLancamentoPontos.PENDENTE, 20, null, null)));

        assertThatThrownBy(() -> pontosService.liberar(10L, 8L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("não pertence à clínica 8");
        verify(lancamentoPontosRepository, never()).save(any());
    }

    @Test
    @DisplayName("Liberar lançamento sem clínica retorna 409")
    void liberar_SemClinica() {
        LancamentoPontos lanc = lancamento(StatusLancamentoPontos.PENDENTE, 20, null, null);
        lanc.setClinica(null);
        when(lancamentoPontosRepository.findById(10L)).thenReturn(Optional.of(lanc));

        assertThatThrownBy(() -> pontosService.liberar(10L, 7L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("nenhuma clínica");
    }

    @Test
    @DisplayName("Deve lançar 409 se tentar liberar lançamento já liberado")
    void liberar_JaLiberado() {
        when(lancamentoPontosRepository.findById(10L))
                .thenReturn(Optional.of(lancamento(StatusLancamentoPontos.LIBERADO, 20, hoje, hoje.plusDays(10))));

        assertThatThrownBy(() -> pontosService.liberar(10L, 7L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("já está LIBERADO");
        verify(lancamentoPontosRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não libera lançamento BLOQUEADO")
    void liberar_Bloqueado() {
        when(lancamentoPontosRepository.findById(10L))
                .thenReturn(Optional.of(lancamento(StatusLancamentoPontos.BLOQUEADO, 20, null, null)));

        assertThatThrownBy(() -> pontosService.liberar(10L, 7L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("BLOQUEADO");
    }

    @Test
    @DisplayName("Bloquear lançamento pendente guarda motivo, data e admin")
    void bloquear_Pendente() {
        Admin admin = Admin.builder().idAdmin(2L).build();
        when(lancamentoPontosRepository.findById(10L))
                .thenReturn(Optional.of(lancamento(StatusLancamentoPontos.PENDENTE, 20, null, null)));
        when(adminRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(lancamentoPontosRepository.save(any(LancamentoPontos.class))).thenAnswer(inv -> inv.getArgument(0));

        LancamentoPontos l = pontosService.bloquear(10L, 7L, "  Suspeita de fraude ", 2L);
        assertThat(l.getDsStatus()).isEqualTo(StatusLancamentoPontos.BLOQUEADO);
        assertThat(l.getDsMotivoBloqueio()).isEqualTo("Suspeita de fraude");
        assertThat(l.getDtBloqueio()).isEqualTo(hoje);
        assertThat(l.getAdminBloqueio()).isEqualTo(admin);
    }

    @Test
    @DisplayName("Bloquear exige motivo")
    void bloquear_SemMotivo() {
        assertThatThrownBy(() -> pontosService.bloquear(10L, 7L, " ", 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Motivo");
    }

    @Test
    @DisplayName("Não bloqueia lançamento já expirado")
    void bloquear_Expirado() {
        when(lancamentoPontosRepository.findById(10L)).thenReturn(Optional.of(
                lancamento(StatusLancamentoPontos.LIBERADO, 20, hoje.minusDays(400), hoje.minusDays(35))));

        assertThatThrownBy(() -> pontosService.bloquear(10L, 7L, "x", 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("EXPIRADO");
    }

    @Test
    @DisplayName("Desbloquear volta a PENDENTE se nunca foi liberado e a LIBERADO se já foi")
    void desbloquear() {
        LancamentoPontos nuncaLiberado = lancamento(StatusLancamentoPontos.BLOQUEADO, 20, null, null);
        nuncaLiberado.setDsMotivoBloqueio("x");
        LancamentoPontos jaLiberado = lancamento(StatusLancamentoPontos.BLOQUEADO, 20, hoje, hoje.plusDays(30));

        when(adminRepository.findById(2L)).thenReturn(Optional.of(Admin.builder().idAdmin(2L).build()));
        when(lancamentoPontosRepository.save(any(LancamentoPontos.class))).thenAnswer(inv -> inv.getArgument(0));

        when(lancamentoPontosRepository.findById(10L)).thenReturn(Optional.of(nuncaLiberado));
        LancamentoPontos a = pontosService.desbloquear(10L, 7L, 2L);
        assertThat(a.getDsStatus()).isEqualTo(StatusLancamentoPontos.PENDENTE);
        assertThat(a.getDsMotivoBloqueio()).isNull();

        when(lancamentoPontosRepository.findById(10L)).thenReturn(Optional.of(jaLiberado));
        assertThat(pontosService.desbloquear(10L, 7L, 2L).getDsStatus()).isEqualTo(StatusLancamentoPontos.LIBERADO);
    }

    @Test
    @DisplayName("LIBERADO com validade vencida é EXPIRADO e não é resgatável")
    void statusEfetivo_Expirado() {
        LancamentoPontos vencido = lancamento(StatusLancamentoPontos.LIBERADO, 20, hoje.minusDays(40), hoje.minusDays(1));
        LancamentoPontos noPrazo = lancamento(StatusLancamentoPontos.LIBERADO, 20, hoje, hoje); // validade inclusiva
        LancamentoPontos bloqueado = lancamento(StatusLancamentoPontos.BLOQUEADO, 20, hoje, hoje.plusDays(5));

        assertThat(vencido.statusEfetivo(hoje)).isEqualTo(StatusLancamentoPontos.EXPIRADO);
        assertThat(vencido.resgatavel(hoje)).isFalse();
        assertThat(noPrazo.statusEfetivo(hoje)).isEqualTo(StatusLancamentoPontos.LIBERADO);
        assertThat(noPrazo.resgatavel(hoje)).isTrue();
        assertThat(bloqueado.resgatavel(hoje)).isFalse();
    }

    @Test
    @DisplayName("Saldo só considera liberados no prazo; pendentes, bloqueados e expirados ficam de fora")
    void calcular_SeparaEstados() {
        List<LancamentoPontos> lancamentos = List.of(
                lancamento(StatusLancamentoPontos.LIBERADO, 100, hoje.minusDays(10), hoje.plusDays(355)),
                lancamento(StatusLancamentoPontos.PENDENTE, 30, null, null),
                lancamento(StatusLancamentoPontos.BLOQUEADO, 40, null, null),
                lancamento(StatusLancamentoPontos.LIBERADO, 50, hoje.minusDays(400), hoje.minusDays(35)));

        PontosService.SaldoPontos saldo = PontosService.calcular(lancamentos, List.of(), hoje);
        assertThat(saldo.saldoDisponivel()).isEqualTo(100);
        assertThat(saldo.pontosLiberados()).isEqualTo(100);
        assertThat(saldo.pontosPendentes()).isEqualTo(30);
        assertThat(saldo.pontosBloqueados()).isEqualTo(40);
        assertThat(saldo.pontosExpirados()).isEqualTo(50);
    }

    @Test
    @DisplayName("Resgate pendente reserva pontos; validado desconta; negado não afeta")
    void calcular_ReservaEResgates() {
        List<LancamentoPontos> lancamentos = List.of(
                lancamento(StatusLancamentoPontos.LIBERADO, 200, hoje.minusDays(10), hoje.plusDays(355)));
        List<Resgate> resgates = List.of(
                resgate(StatusResgate.VALIDADO, 50, hoje.minusDays(5)),
                resgate(StatusResgate.PENDENTE, 80, hoje),
                resgate(StatusResgate.NEGADO, 70, hoje));

        PontosService.SaldoPontos saldo = PontosService.calcular(lancamentos, resgates, hoje);
        assertThat(saldo.pontosResgatados()).isEqualTo(50);
        assertThat(saldo.pontosReservados()).isEqualTo(80);
        assertThat(saldo.saldoDisponivel()).isEqualTo(70);
    }

    @Test
    @DisplayName("Pontos antigos já gastos antes de vencer não deixam o saldo negativo")
    void calcular_GastoAntesDeVencer() {
        // Lote A (100) venceu ontem e foi gasto há 30 dias; lote B (100) segue válido.
        List<LancamentoPontos> lancamentos = List.of(
                lancamento(StatusLancamentoPontos.LIBERADO, 100, hoje.minusDays(300), hoje.minusDays(1)),
                lancamento(StatusLancamentoPontos.LIBERADO, 100, hoje.minusDays(20), hoje.plusDays(345)));
        List<Resgate> resgates = List.of(resgate(StatusResgate.VALIDADO, 100, hoje.minusDays(30)));

        PontosService.SaldoPontos saldo = PontosService.calcular(lancamentos, resgates, hoje);
        assertThat(saldo.saldoDisponivel()).isEqualTo(100);
        assertThat(saldo.pontosExpirados()).isZero();
    }

    @Test
    @DisplayName("Resgate usa primeiro os pontos que vencem antes")
    void calcular_ConsomeOQueVenceAntes() {
        List<LancamentoPontos> lancamentos = List.of(
                lancamento(StatusLancamentoPontos.LIBERADO, 100, hoje.minusDays(100), hoje.plusDays(265)),
                lancamento(StatusLancamentoPontos.LIBERADO, 100, hoje.minusDays(300), hoje.plusDays(65)));
        List<Resgate> resgates = List.of(resgate(StatusResgate.VALIDADO, 100, hoje.minusDays(1)));

        PontosService.SaldoPontos saldo = PontosService.calcular(lancamentos, resgates, hoje);
        assertThat(saldo.saldoDisponivel()).isEqualTo(100);
    }
}