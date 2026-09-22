package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.CarteiraCompartilhada;
import br.com.fiap.VetSync.entity.Especie;
import br.com.fiap.VetSync.entity.Pet;
import br.com.fiap.VetSync.entity.Raca;
import br.com.fiap.VetSync.entity.StatusEvento;
import br.com.fiap.VetSync.repository.CarteiraCompartilhadaRepository;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarteiraCompartilhadaServiceTest {

    @Mock
    private CarteiraCompartilhadaRepository carteiraCompartilhadaRepository;

    @Mock
    private EventoSaudeRepository eventoSaudeRepository;

    @Mock
    private PetService petService;

    @InjectMocks
    private CarteiraCompartilhadaService carteiraCompartilhadaService;

    @Test
    void deveMontarCarteiraPublicaComProjecaoSemCarregarEventoCompleto() {
        Pet pet = Pet.builder()
                .idPet(7L)
                .nmPet("Morgana")
                .raca(Raca.builder().nmRaca("Labrador").especie(Especie.builder().nmEspecie("Cão").build()).build())
                .build();
        CarteiraCompartilhada carteira = CarteiraCompartilhada.builder()
                .pet(pet)
                .expiraEm(LocalDateTime.now().plusDays(1))
                .build();
        EventoSaudeRepository.VacinaPublicaProjection vacina = mock(EventoSaudeRepository.VacinaPublicaProjection.class);
        when(vacina.getNome()).thenReturn("Vacina");
        when(vacina.getData()).thenReturn(LocalDate.now().minusDays(1));
        when(vacina.getStatus()).thenReturn(StatusEvento.AGENDADO);

        when(carteiraCompartilhadaRepository.findByTokenHash(any())).thenReturn(Optional.of(carteira));
        when(carteiraCompartilhadaRepository.save(any(CarteiraCompartilhada.class))).thenReturn(carteira);
        when(eventoSaudeRepository.buscarVacinasPublicasPorPet(7L, "Vacina", StatusEvento.CANCELADO))
                .thenReturn(List.of(vacina));

        CarteiraCompartilhadaService.CarteiraPublica resultado =
                carteiraCompartilhadaService.resolverParaExibicaoPublica("token-publico");

        assertThat(resultado.nomePet()).isEqualTo("Morgana");
        assertThat(resultado.especie()).isEqualTo("Cão");
        assertThat(resultado.vacinas()).singleElement().satisfies(item -> {
            assertThat(item.nome()).isEqualTo("Vacina");
            assertThat(item.status()).isEqualTo("Atrasada");
        });
        verify(eventoSaudeRepository).buscarVacinasPublicasPorPet(7L, "Vacina", StatusEvento.CANCELADO);
        verify(eventoSaudeRepository, never()).findByPet_IdPet(eq(7L));
    }
}
