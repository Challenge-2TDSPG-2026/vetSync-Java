package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Veterinario;
import br.com.fiap.VetSync.entity.BloqueioAgenda;
import br.com.fiap.VetSync.entity.Disponibilidade;
import br.com.fiap.VetSync.repository.BloqueioAgendaRepository;
import br.com.fiap.VetSync.repository.DisponibilidadeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {

    @Mock
    private DisponibilidadeRepository disponibilidadeRepository;

    @Mock
    private BloqueioAgendaRepository bloqueioAgendaRepository;

    @Mock
    private VeterinarioService veterinarioService;

    @InjectMocks
    private AgendaService agendaService;

    @Test
    @DisplayName("Deve adicionar disponibilidade com sucesso")
    void adicionarDisponibilidade_Sucesso() {
        Veterinario vet = Veterinario.builder().idVeterinario(1L).nmVeterinario("Dr. Silva").build();
        when(veterinarioService.buscarPorId(1L)).thenReturn(vet);
        when(disponibilidadeRepository.save(any(Disponibilidade.class))).thenAnswer(inv -> inv.getArgument(0));

        Disponibilidade disp = agendaService.adicionarDisponibilidade(1L, 2, "08:00", "12:00");

        assertThat(disp).isNotNull();
        assertThat(disp.getNrDiaSemana()).isEqualTo(2);
        assertThat(disp.getHrInicio()).isEqualTo("08:00");
        assertThat(disp.getHrFim()).isEqualTo("12:00");
    }

    @Test
    @DisplayName("Deve lançar 400 se dia da semana for inválido")
    void adicionarDisponibilidade_DiaInvalido() {
        assertThatThrownBy(() -> agendaService.adicionarDisponibilidade(1L, 8, "08:00", "12:00"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Dia da semana deve ser entre 1 (segunda) e 7 (domingo)");

        assertThatThrownBy(() -> agendaService.adicionarDisponibilidade(1L, 0, "08:00", "12:00"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("Deve lançar 400 se horário início for igual ou posterior ao fim")
    void adicionarDisponibilidade_HorarioInvalido() {
        assertThatThrownBy(() -> agendaService.adicionarDisponibilidade(1L, 1, "12:00", "08:00"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Horário de início deve ser antes do horário de fim");

        assertThatThrownBy(() -> agendaService.adicionarDisponibilidade(1L, 1, "08:00", "08:00"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("Deve listar disponibilidades do veterinário")
    void listarDisponibilidade_Sucesso() {
        when(disponibilidadeRepository.findByVeterinario_IdVeterinario(1L)).thenReturn(List.of(
                Disponibilidade.builder().idDisponibilidade(10L).nrDiaSemana(1).build()
        ));

        List<Disponibilidade> lista = agendaService.listarDisponibilidade(1L);
        assertThat(lista).hasSize(1);
    }

    @Test
    @DisplayName("Deve remover disponibilidade com sucesso")
    void removerDisponibilidade_Sucesso() {
        Veterinario vet = Veterinario.builder().idVeterinario(1L).build();
        Disponibilidade disp = Disponibilidade.builder().idDisponibilidade(10L).veterinario(vet).build();
        when(disponibilidadeRepository.findById(10L)).thenReturn(Optional.of(disp));

        agendaService.removerDisponibilidade(1L, 10L);
        verify(disponibilidadeRepository).delete(disp);
    }

    @Test
    @DisplayName("Deve lançar 404 ao tentar remover disponibilidade de outro veterinário")
    void removerDisponibilidade_OutroVeterinario() {
        Veterinario outroVet = Veterinario.builder().idVeterinario(2L).build();
        Disponibilidade disp = Disponibilidade.builder().idDisponibilidade(10L).veterinario(outroVet).build();
        when(disponibilidadeRepository.findById(10L)).thenReturn(Optional.of(disp));

        assertThatThrownBy(() -> agendaService.removerDisponibilidade(1L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Registro não pertence a este veterinário");

        verify(disponibilidadeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve adicionar bloqueio com sucesso")
    void adicionarBloqueio_Sucesso() {
        Veterinario vet = Veterinario.builder().idVeterinario(1L).build();
        when(veterinarioService.buscarPorId(1L)).thenReturn(vet);
        when(bloqueioAgendaRepository.save(any(BloqueioAgenda.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate inicio = LocalDate.now().plusDays(1);
        LocalDate fim = LocalDate.now().plusDays(5);

        BloqueioAgenda b = agendaService.adicionarBloqueio(1L, inicio, fim, "Férias");
        assertThat(b.getDsMotivo()).isEqualTo("Férias");
        assertThat(b.getDtInicio()).isEqualTo(inicio);
        assertThat(b.getDtFim()).isEqualTo(fim);
    }

    @Test
    @DisplayName("Deve lançar 400 se data fim for anterior à data início no bloqueio")
    void adicionarBloqueio_DataInvalida() {
        LocalDate inicio = LocalDate.now().plusDays(5);
        LocalDate fim = LocalDate.now().plusDays(1);

        assertThatThrownBy(() -> agendaService.adicionarBloqueio(1L, inicio, fim, "Motivo"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Data de fim deve ser igual ou depois da data de início");
    }

    @Test
    @DisplayName("Deve remover bloqueio com sucesso")
    void removerBloqueio_Sucesso() {
        Veterinario vet = Veterinario.builder().idVeterinario(1L).build();
        BloqueioAgenda b = BloqueioAgenda.builder().idBloqueio(20L).veterinario(vet).build();
        when(bloqueioAgendaRepository.findById(20L)).thenReturn(Optional.of(b));

        agendaService.removerBloqueio(1L, 20L);
        verify(bloqueioAgendaRepository).delete(b);
    }
}
