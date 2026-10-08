package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class VacinaServiceTest {
    @Mock VacinaPetRepository vacinaRepository;
    @Mock TipoVacinaRepository tipoRepository;
    @Mock PetService petService;
    @Mock EventoSaudeRepository eventoRepository;

    @InjectMocks VacinaService service;

    @Test
    void deveClassificarStatusDaDose() {
        LocalDate hoje = LocalDate.of(2026, 9, 28);
        TipoVacina tipo = TipoVacina.builder().nmTipoVacina("Antirrábica").nrPeriodicidadeDias(365).build();

        assertThat(service.status(VacinaPet.builder().tipoVacina(tipo).dtAplicacao(hoje.minusYears(1))
                .dtProximaDose(hoje.plusDays(10)).build(), hoje)).isEqualTo(StatusVacina.VENCENDO);
        assertThat(service.status(VacinaPet.builder().tipoVacina(tipo).dtAplicacao(hoje.minusYears(2))
                .dtProximaDose(hoje.minusDays(1)).build(), hoje)).isEqualTo(StatusVacina.ATRASADA);
        assertThat(service.status(VacinaPet.builder().tipoVacina(tipo).dtAplicacao(hoje.plusDays(1))
                .dtProximaDose(hoje.plusDays(366)).build(), hoje)).isEqualTo(StatusVacina.FUTURA);
    }

    private VacinaPet dose(Long id, TipoVacina tipo, LocalDate aplicacao, LocalDate proxima) {
        return VacinaPet.builder().idVacina(id).tipoVacina(tipo).dtAplicacao(aplicacao).dtProximaDose(proxima).build();
    }

    @Test
    void doseSubstituidaPorReaplicacaoNaoDeveFicarAtrasada() {
        LocalDate hoje = LocalDate.of(2026, 9, 28);
        TipoVacina v10 = TipoVacina.builder().idTipoVacina(1L).nmTipoVacina("V10").nrPeriodicidadeDias(365).build();
        VacinaPet antiga = dose(1L, v10, hoje.minusYears(2), hoje.minusYears(1));
        VacinaPet nova = dose(2L, v10, hoje.minusDays(20), hoje.plusDays(345));

        Set<VacinaPet> substituidas = service.substituidas(List.of(nova, antiga), hoje);

        assertThat(substituidas).containsExactly(antiga);
        assertThat(service.status(antiga, hoje)).isEqualTo(StatusVacina.ATRASADA);
        assertThat(service.status(antiga, hoje, substituidas)).isEqualTo(StatusVacina.EM_DIA);
        assertThat(service.status(nova, hoje, substituidas)).isEqualTo(StatusVacina.EM_DIA);
    }

    @Test
    void vacinaSemReaplicacaoContinuaAtrasada() {
        LocalDate hoje = LocalDate.of(2026, 9, 28);
        TipoVacina v10 = TipoVacina.builder().idTipoVacina(1L).nmTipoVacina("V10").nrPeriodicidadeDias(365).build();
        VacinaPet unica = dose(1L, v10, hoje.minusYears(2), hoje.minusDays(5));

        Set<VacinaPet> substituidas = service.substituidas(List.of(unica), hoje);

        assertThat(substituidas).isEmpty();
        assertThat(service.status(unica, hoje, substituidas)).isEqualTo(StatusVacina.ATRASADA);
    }

    @Test
    void reaplicacaoDeOutroTipoOuFuturaNaoSubstituiDose() {
        LocalDate hoje = LocalDate.of(2026, 9, 28);
        TipoVacina v10 = TipoVacina.builder().idTipoVacina(1L).nmTipoVacina("V10").nrPeriodicidadeDias(365).build();
        TipoVacina raiva = TipoVacina.builder().idTipoVacina(2L).nmTipoVacina("Antirrábica").nrPeriodicidadeDias(365).build();
        VacinaPet v10Antiga = dose(1L, v10, hoje.minusYears(2), hoje.minusDays(5));
        VacinaPet raivaNova = dose(2L, raiva, hoje.minusDays(3), hoje.plusDays(362));
        VacinaPet v10Futura = dose(3L, v10, hoje.plusDays(10), hoje.plusDays(375));

        assertThat(service.substituidas(List.of(v10Antiga, raivaNova, v10Futura), hoje)).isEmpty();
    }

    @Test
    void mesmaDataDesempataPeloMaiorId() {
        LocalDate hoje = LocalDate.of(2026, 9, 28);
        TipoVacina v10 = TipoVacina.builder().idTipoVacina(1L).nmTipoVacina("V10").nrPeriodicidadeDias(365).build();
        VacinaPet a = dose(1L, v10, hoje.minusDays(10), hoje.plusDays(355));
        VacinaPet b = dose(2L, v10, hoje.minusDays(10), hoje.plusDays(355));

        assertThat(service.substituidas(List.of(a, b), hoje)).containsExactly(a);
    }
}