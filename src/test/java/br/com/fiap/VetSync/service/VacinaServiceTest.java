package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

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
}
