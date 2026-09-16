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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanoTratamentoServiceTest {

    @Mock
    private PlanoTratamentoRepository planoTratamentoRepository;

    @Mock
    private PlanoItemRepository planoItemRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private VeterinarioRepository veterinarioRepository;

    @Mock
    private TipoEventoRepository tipoEventoRepository;

    @Mock
    private EventoService eventoService;

    @InjectMocks
    private PlanoTratamentoService planoTratamentoService;

    @Test
    @DisplayName("Deve criar plano de tratamento com múltiplos itens em sequência")
    void criar_Sucesso() {
        Pet pet = Pet.builder().idPet(1L).build();
        Veterinario vet = Veterinario.builder().idVeterinario(2L).build();
        TipoEvento t1 = TipoEvento.builder().idTipoEvento(10L).build();
        TipoEvento t2 = TipoEvento.builder().idTipoEvento(20L).build();

        when(petRepository.findById(1L)).thenReturn(Optional.of(pet));
        when(veterinarioRepository.findById(2L)).thenReturn(Optional.of(vet));
        when(tipoEventoRepository.findById(10L)).thenReturn(Optional.of(t1));
        when(tipoEventoRepository.findById(20L)).thenReturn(Optional.of(t2));
        when(planoTratamentoRepository.save(any(PlanoTratamento.class))).thenAnswer(inv -> {
            PlanoTratamento p = inv.getArgument(0);
            p.setIdPlano(100L);
            return p;
        });

        PlanoTratamento plano = planoTratamentoService.criar(1L, 2L, 50, List.of(10L, 20L));

        assertThat(plano.getIdPlano()).isEqualTo(100L);
        assertThat(plano.getDsStatus()).isEqualTo(StatusPlanoTratamento.EM_ANDAMENTO);
        assertThat(plano.getNrPontosBonus()).isEqualTo(50);
        verify(planoItemRepository, times(2)).save(any(PlanoItem.class));
    }

    @Test
    @DisplayName("Deve falhar com 400 se lista de tipos tiver menos de 2 itens")
    void criar_MenosDeDoisItens() {
        assertThatThrownBy(() -> planoTratamentoService.criar(1L, 2L, 10, List.of(1L)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("precisa de pelo menos 2 itens");

        assertThatThrownBy(() -> planoTratamentoService.criar(1L, 2L, 10, null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("Deve agendar próximo item pendente do plano")
    void agendarItem_Sucesso() {
        Pet pet = Pet.builder().idPet(1L).build();
        PlanoTratamento plano = PlanoTratamento.builder()
                .idPlano(10L)
                .pet(pet)
                .dsStatus(StatusPlanoTratamento.EM_ANDAMENTO)
                .build();

        TipoEvento tipo = TipoEvento.builder().idTipoEvento(5L).build();
        PlanoItem item = PlanoItem.builder()
                .idItem(100L)
                .plano(plano)
                .tipoEvento(tipo)
                .dsStatus(StatusPlanoItem.PENDENTE)
                .build();

        when(planoItemRepository.findById(100L)).thenReturn(Optional.of(item));

        EventoSaude eventoCriado = EventoSaude.builder().idEvento(500L).build();
        when(eventoService.agendar(any(), eq(1L), eq(5L), eq(2L))).thenReturn(eventoCriado);

        EventoSaude agendado = planoTratamentoService.agendarItem(100L, 2L, LocalDate.now().plusDays(3), "09:00", "Primeira dose");

        assertThat(agendado).isNotNull();
        assertThat(item.getDsStatus()).isEqualTo(StatusPlanoItem.AGENDADO);
        assertThat(item.getEvento()).isEqualTo(eventoCriado);
        verify(planoItemRepository).save(item);
    }

    @Test
    @DisplayName("Deve lançar 409 se item do plano já estiver agendado ou concluído")
    void agendarItem_ItemNaoPendente() {
        PlanoItem item = PlanoItem.builder()
                .idItem(100L)
                .dsStatus(StatusPlanoItem.CONCLUIDO)
                .build();

        when(planoItemRepository.findById(100L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> planoTratamentoService.agendarItem(100L, 2L, LocalDate.now(), "09:00", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("já está CONCLUIDO");
    }

    @Test
    @DisplayName("Deve lançar 409 se plano não estiver mais em andamento")
    void agendarItem_PlanoQuebrado() {
        PlanoTratamento plano = PlanoTratamento.builder()
                .idPlano(10L)
                .dsStatus(StatusPlanoTratamento.QUEBRADO)
                .build();

        PlanoItem item = PlanoItem.builder()
                .idItem(100L)
                .plano(plano)
                .dsStatus(StatusPlanoItem.PENDENTE)
                .build();

        when(planoItemRepository.findById(100L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> planoTratamentoService.agendarItem(100L, 2L, LocalDate.now(), "09:00", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("não está mais em andamento");
    }
}
