package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecompensaServiceTest {

    @Mock
    private RecompensaRepository recompensaRepository;

    @Mock
    private ResgateRepository resgateRepository;

    @Mock
    private VeterinarioRepository veterinarioRepository;

    @Mock
    private TutorService tutorService;

    @Mock
    private PontosService pontosService;

    @Mock
    private ClinicaService clinicaService;

    @Mock
    private VinculoClinicaService vinculoClinicaService;

    @InjectMocks
    private RecompensaService recompensaService;

    private final Clinica clinica = Clinica.builder().idClinica(7L).nmClinica("Clínica Centro").build();

    private VinculoTutorClinica vinculoEm(Clinica c) {
        return VinculoTutorClinica.builder().clinica(c).build();
    }

    @Test
    @DisplayName("Deve criar recompensa ativa no catálogo")
    void criar_Sucesso() {
        when(clinicaService.buscarObrigatoria(7L)).thenReturn(clinica);
        when(recompensaRepository.save(any(Recompensa.class))).thenAnswer(inv -> inv.getArgument(0));

        Recompensa r = recompensaService.criar("Banho Grátis", "Vale 1 banho", 100, TipoRecompensa.PRODUTO, 7L);
        assertThat(r.getClinica()).isEqualTo(clinica);
        assertThat(r.getNmRecompensa()).isEqualTo("Banho Grátis");
        assertThat(r.getNrCustoPontos()).isEqualTo(100);
        assertThat(r.getFlAtivo()).isTrue();
    }

    private PontosService.SaldoPontos saldoDisponivel(int disponivel) {
        return new PontosService.SaldoPontos(0, disponivel, 0, 0, 0, 0, disponivel);
    }

    @Test
    @DisplayName("O saldo do resgate é o saldo disponível calculado pelo PontosService (já sem reservas, bloqueados e vencidos)")
    void calcularSaldo() {
        when(pontosService.calcularSaldo(1L, 7L)).thenReturn(new PontosService.SaldoPontos(10, 200, 20, 30, 50, 80, 70));

        assertThat(recompensaService.calcularSaldo(1L, 7L)).isEqualTo(70);
    }

    @Test
    @DisplayName("Deve solicitar resgate com sucesso quando saldo for suficiente")
    void solicitarResgate_Sucesso() {
        Recompensa rec = Recompensa.builder()
                .idRecompensa(10L)
                .nrCustoPontos(50)
                .flAtivo(true)
                .clinica(clinica)
                .build();
        Tutor tutor = Tutor.builder().idTutor(1L).build();

        when(recompensaRepository.findById(10L)).thenReturn(Optional.of(rec));
        when(vinculoClinicaService.buscarVinculoAtivo(1L)).thenReturn(vinculoEm(clinica));
        when(pontosService.calcularSaldo(1L, 7L)).thenReturn(saldoDisponivel(100));
        when(tutorService.buscarPorId(1L)).thenReturn(tutor);
        when(resgateRepository.save(any(Resgate.class))).thenAnswer(inv -> inv.getArgument(0));

        Resgate resgate = recompensaService.solicitarResgate(1L, 10L);
        assertThat(resgate.getDsStatus()).isEqualTo(StatusResgate.PENDENTE);
        assertThat(resgate.getTutor()).isEqualTo(tutor);
        assertThat(resgate.getRecompensa()).isEqualTo(rec);
    }

    @Test
    @DisplayName("Deve falhar ao solicitar resgate com saldo insuficiente")
    void solicitarResgate_SaldoInsuficiente() {
        Recompensa rec = Recompensa.builder()
                .idRecompensa(10L)
                .nrCustoPontos(150)
                .flAtivo(true)
                .clinica(clinica)
                .build();

        when(recompensaRepository.findById(10L)).thenReturn(Optional.of(rec));
        when(vinculoClinicaService.buscarVinculoAtivo(1L)).thenReturn(vinculoEm(clinica));
        when(pontosService.calcularSaldo(1L, 7L)).thenReturn(saldoDisponivel(100));

        assertThatThrownBy(() -> recompensaService.solicitarResgate(1L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Saldo insuficiente");

        verify(resgateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar ao solicitar resgate de recompensa inativa")
    void solicitarResgate_RecompensaInativa() {
        Recompensa rec = Recompensa.builder()
                .idRecompensa(10L)
                .flAtivo(false)
                .build();

        when(recompensaRepository.findById(10L)).thenReturn(Optional.of(rec));

        assertThatThrownBy(() -> recompensaService.solicitarResgate(1L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("não está mais disponível");

        verify(resgateRepository, never()).save(any());
    }

    private Resgate resgatePendente(Clinica clinicaDoResgate, Clinica clinicaDaRecompensa) {
        Recompensa rec = Recompensa.builder().idRecompensa(10L).clinica(clinicaDaRecompensa).build();
        return Resgate.builder().idResgate(5L).dsStatus(StatusResgate.PENDENTE)
                .recompensa(rec).clinica(clinicaDoResgate).build();
    }

    @Test
    @DisplayName("Deve validar resgate com sucesso quando veterinário, resgate e recompensa são da mesma clínica")
    void validar_Sucesso() {
        Resgate resgate = resgatePendente(clinica, clinica);
        Veterinario vet = Veterinario.builder().idVeterinario(3L).clinica(clinica).build();

        when(resgateRepository.findByIdParaAtualizar(5L)).thenReturn(Optional.of(resgate));
        when(veterinarioRepository.findById(3L)).thenReturn(Optional.of(vet));
        when(resgateRepository.save(any(Resgate.class))).thenAnswer(inv -> inv.getArgument(0));

        Resgate validado = recompensaService.validar(5L, 3L, true);
        assertThat(validado.getDsStatus()).isEqualTo(StatusResgate.VALIDADO);
        assertThat(validado.getVeterinarioValidador()).isEqualTo(vet);
        assertThat(validado.getClinica()).isEqualTo(clinica);
    }

    @Test
    @DisplayName("Deve negar resgate mantendo a clínica do resgate")
    void validar_Negar() {
        Resgate resgate = resgatePendente(clinica, clinica);
        Veterinario vet = Veterinario.builder().idVeterinario(3L).clinica(clinica).build();

        when(resgateRepository.findByIdParaAtualizar(5L)).thenReturn(Optional.of(resgate));
        when(veterinarioRepository.findById(3L)).thenReturn(Optional.of(vet));
        when(resgateRepository.save(any(Resgate.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(recompensaService.validar(5L, 3L, false).getDsStatus()).isEqualTo(StatusResgate.NEGADO);
    }

    @Test
    @DisplayName("Não valida resgate que já foi decidido")
    void validar_JaDecidido() {
        Resgate resgate = resgatePendente(clinica, clinica);
        resgate.setDsStatus(StatusResgate.VALIDADO);
        when(resgateRepository.findByIdParaAtualizar(5L)).thenReturn(Optional.of(resgate));

        assertThatThrownBy(() -> recompensaService.validar(5L, 3L, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("já foi");
        verify(resgateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não valida resgate sem clínica (não haveria saldo a debitar)")
    void validar_ResgateSemClinica() {
        Resgate resgate = resgatePendente(null, clinica);
        Veterinario vet = Veterinario.builder().idVeterinario(3L).clinica(clinica).build();
        when(resgateRepository.findByIdParaAtualizar(5L)).thenReturn(Optional.of(resgate));
        when(veterinarioRepository.findById(3L)).thenReturn(Optional.of(vet));

        assertThatThrownBy(() -> recompensaService.validar(5L, 3L, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("não está associado a nenhuma clínica");
        verify(resgateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não valida quando a clínica do resgate difere da clínica da recompensa")
    void validar_ClinicaDoResgateDiferenteDaRecompensa() {
        Clinica outra = Clinica.builder().idClinica(8L).nmClinica("Clínica Norte").build();
        Resgate resgate = resgatePendente(clinica, outra);
        Veterinario vet = Veterinario.builder().idVeterinario(3L).clinica(clinica).build();
        when(resgateRepository.findByIdParaAtualizar(5L)).thenReturn(Optional.of(resgate));
        when(veterinarioRepository.findById(3L)).thenReturn(Optional.of(vet));

        assertThatThrownBy(() -> recompensaService.validar(5L, 3L, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("não corresponde à clínica da recompensa");
        verify(resgateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não valida quando a recompensa do resgate não tem clínica")
    void validar_RecompensaSemClinica() {
        Resgate resgate = resgatePendente(clinica, null);
        Veterinario vet = Veterinario.builder().idVeterinario(3L).clinica(clinica).build();
        when(resgateRepository.findByIdParaAtualizar(5L)).thenReturn(Optional.of(resgate));
        when(veterinarioRepository.findById(3L)).thenReturn(Optional.of(vet));

        assertThatThrownBy(() -> recompensaService.validar(5L, 3L, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("não corresponde à clínica da recompensa");
        verify(resgateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Veterinário sem clínica não valida resgate")
    void validar_VeterinarioSemClinica() {
        Resgate resgate = resgatePendente(clinica, clinica);
        Veterinario vet = Veterinario.builder().idVeterinario(3L).build();
        when(resgateRepository.findByIdParaAtualizar(5L)).thenReturn(Optional.of(resgate));
        when(veterinarioRepository.findById(3L)).thenReturn(Optional.of(vet));

        assertThatThrownBy(() -> recompensaService.validar(5L, 3L, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("outra clínica");
        verify(resgateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve negar resgate de recompensa de outra clínica (pontos não acompanham o tutor)")
    void solicitarResgate_OutraClinica() {
        Clinica outra = Clinica.builder().idClinica(8L).nmClinica("Clínica Norte").build();
        Recompensa rec = Recompensa.builder()
                .idRecompensa(10L)
                .nrCustoPontos(10)
                .flAtivo(true)
                .clinica(outra)
                .build();

        when(recompensaRepository.findById(10L)).thenReturn(Optional.of(rec));
        when(vinculoClinicaService.buscarVinculoAtivo(1L)).thenReturn(vinculoEm(clinica));

        assertThatThrownBy(() -> recompensaService.solicitarResgate(1L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("outra clínica");

        verify(resgateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve negar resgate de recompensa sem clínica definida")
    void solicitarResgate_RecompensaSemClinica() {
        Recompensa rec = Recompensa.builder().idRecompensa(10L).nrCustoPontos(10).flAtivo(true).build();
        when(recompensaRepository.findById(10L)).thenReturn(Optional.of(rec));

        assertThatThrownBy(() -> recompensaService.solicitarResgate(1L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("nenhuma clínica");
    }

    @Test
    @DisplayName("Saldo na clínica vinculada é 0 quando o tutor não tem vínculo ativo")
    void calcularSaldoNaClinicaVinculada_SemVinculo() {
        when(vinculoClinicaService.buscarVinculoAtivo(1L)).thenReturn(null);

        assertThat(recompensaService.calcularSaldoNaClinicaVinculada(1L)).isZero();
        verifyNoInteractions(pontosService);
    }

    @Test
    @DisplayName("Saldo na clínica vinculada usa só a clínica do vínculo ativo")
    void calcularSaldoNaClinicaVinculada_ComVinculo() {
        when(vinculoClinicaService.buscarVinculoAtivo(1L)).thenReturn(vinculoEm(clinica));
        when(pontosService.calcularSaldo(1L, 7L)).thenReturn(saldoDisponivel(40));

        assertThat(recompensaService.calcularSaldoNaClinicaVinculada(1L)).isEqualTo(40);
    }

    @Test
    @DisplayName("Veterinário não valida resgate de outra clínica")
    void validar_OutraClinica() {
        Clinica outra = Clinica.builder().idClinica(8L).nmClinica("Clínica Norte").build();
        Resgate resgate = resgatePendente(outra, outra);
        Veterinario vet = Veterinario.builder().idVeterinario(3L).clinica(clinica).build();

        when(resgateRepository.findByIdParaAtualizar(5L)).thenReturn(Optional.of(resgate));
        when(veterinarioRepository.findById(3L)).thenReturn(Optional.of(vet));

        assertThatThrownBy(() -> recompensaService.validar(5L, 3L, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("outra clínica");

        verify(resgateRepository, never()).save(any());
    }
}