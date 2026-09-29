package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Disponibilidade;
import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.entity.StatusEvento;
import br.com.fiap.VetSync.entity.TipoEvento;
import br.com.fiap.VetSync.entity.Veterinario;
import br.com.fiap.VetSync.repository.DisponibilidadeRepository;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import br.com.fiap.VetSync.repository.TipoEventoRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendaSlotsServiceTest {

    @Mock private TipoEventoRepository tipoEventoRepository;
    @Mock private VeterinarioRepository veterinarioRepository;
    @Mock private DisponibilidadeRepository disponibilidadeRepository;
    @Mock private EventoSaudeRepository eventoSaudeRepository;
    @Mock private AgendaService agendaService;
    @InjectMocks private AgendaSlotsService agendaSlotsService;

    @Test
    void listaApenasHorariosLivresDaDisponibilidadeDoVeterinario() {
        LocalDate data = LocalDate.now().plusDays(1);
        Veterinario veterinario = Veterinario.builder().idVeterinario(4L).nmVeterinario("Dra. Ana").build();
        TipoEvento tipo = TipoEvento.builder()
                .idTipoEvento(8L)
                .nmTipoEvento("Consulta de rotina")
                .dsModalidadeAgendamento("CLINICO_GERAL")
                .nrDuracaoMinutos(30)
                .build();
        Disponibilidade disponibilidade = Disponibilidade.builder()
                .nrDiaSemana(data.getDayOfWeek().getValue())
                .hrInicio("09:00")
                .hrFim("10:30")
                .build();
        EventoSaude ocupado = EventoSaude.builder()
                .dsStatus(StatusEvento.AGENDADO)
                .hrEvento("09:30")
                .build();

        when(tipoEventoRepository.findByDsModalidadeAgendamentoIgnoreCase("CLINICO_GERAL"))
                .thenReturn(List.of(tipo));
        when(veterinarioRepository.findAll()).thenReturn(List.of(veterinario));
        when(agendaService.estaBloqueado(4L, data)).thenReturn(false);
        when(eventoSaudeRepository.findByVeterinario_IdVeterinarioAndDtEvento(4L, data))
                .thenReturn(List.of(ocupado));
        when(disponibilidadeRepository.findByVeterinario_IdVeterinario(4L))
                .thenReturn(List.of(disponibilidade));

        List<AgendaSlotsService.SlotDisponivel> slots = agendaSlotsService.listarSlots(data, "CLINICO_GERAL");

        assertThat(slots).extracting(AgendaSlotsService.SlotDisponivel::hrEvento)
                .containsExactly("09:00", "10:00");
        assertThat(slots).allSatisfy(slot -> {
            assertThat(slot.idTipoEvento()).isEqualTo(8L);
            assertThat(slot.idVeterinario()).isEqualTo(4L);
        });
    }

    @Test
    void naoListaSlotsDeVeterinarioComAgendaBloqueada() {
        LocalDate data = LocalDate.now().plusDays(1);
        Veterinario veterinario = Veterinario.builder().idVeterinario(4L).nmVeterinario("Dra. Ana").build();
        TipoEvento tipo = TipoEvento.builder().idTipoEvento(8L).nmTipoEvento("Consulta").build();

        when(tipoEventoRepository.findByDsModalidadeAgendamentoIgnoreCase("CLINICO_GERAL"))
                .thenReturn(List.of(tipo));
        when(veterinarioRepository.findAll()).thenReturn(List.of(veterinario));
        when(agendaService.estaBloqueado(4L, data)).thenReturn(true);

        assertThat(agendaSlotsService.listarSlots(data, "CLINICO_GERAL")).isEmpty();
    }
}
