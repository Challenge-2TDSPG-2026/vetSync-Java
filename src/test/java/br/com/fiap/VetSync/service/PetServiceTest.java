package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private TutorService tutorService;

    @Mock
    private EspecieRepository especieRepository;

    @Mock
    private RacaRepository racaRepository;

    @Mock
    private PetAcessoRepository petAcessoRepository;

    @InjectMocks
    private PetService petService;

    @Test
    @DisplayName("Deve cadastrar pet com espécie padrão e raça existente")
    void cadastrar_EspeciePadrao() {
        Tutor tutor = Tutor.builder().idTutor(1L).build();
        Especie especie = Especie.builder().idEspecie(10L).nmEspecie("Cão").build();
        Raca raca = Raca.builder().idRaca(20L).nmRaca("Labrador").especie(especie).build();

        when(tutorService.buscarPorId(1L)).thenReturn(tutor);
        when(especieRepository.findByNmEspecieIgnoreCase("Cão")).thenReturn(Optional.of(especie));
        when(racaRepository.findByNmRacaIgnoreCaseAndEspecie_IdEspecie("Labrador", 10L)).thenReturn(Optional.of(raca));
        when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));

        Pet pet = Pet.builder().nmPet("Max").dtNascimento(LocalDate.now().minusYears(3)).build();
        Pet cadastrado = petService.cadastrar(pet, 1L, EspecieCategoria.CAO, null, "Labrador");

        assertThat(cadastrado.getTutor()).isEqualTo(tutor);
        assertThat(cadastrado.getRaca()).isEqualTo(raca);
    }

    @Test
    @DisplayName("Deve cadastrar pet com espécie OUTRO")
    void cadastrar_EspecieOutro() {
        Tutor tutor = Tutor.builder().idTutor(1L).build();
        Especie especie = Especie.builder().idEspecie(11L).nmEspecie("Coelho").build();
        Raca raca = Raca.builder().idRaca(21L).nmRaca("Mini Lion").especie(especie).build();

        when(tutorService.buscarPorId(1L)).thenReturn(tutor);
        when(especieRepository.findByNmEspecieIgnoreCase("Coelho")).thenReturn(Optional.of(especie));
        when(racaRepository.findByNmRacaIgnoreCaseAndEspecie_IdEspecie("Mini Lion", 11L)).thenReturn(Optional.of(raca));
        when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));

        Pet pet = Pet.builder().nmPet("Pompom").build();
        Pet cadastrado = petService.cadastrar(pet, 1L, EspecieCategoria.OUTRO, "coelho", "Mini Lion");

        assertThat(cadastrado.getRaca()).isEqualTo(raca);
    }

    @Test
    @DisplayName("Deve falhar ao cadastrar espécie OUTRO sem especificar nome")
    void cadastrar_EspecieOutroSemNome() {
        Tutor tutor = Tutor.builder().idTutor(1L).build();
        when(tutorService.buscarPorId(1L)).thenReturn(tutor);

        Pet pet = Pet.builder().nmPet("Bicho").build();
        assertThatThrownBy(() -> petService.cadastrar(pet, 1L, EspecieCategoria.OUTRO, "   ", "Raça"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Informe o nome da espécie");
    }

    @Test
    @DisplayName("Deve buscar pet por ID com sucesso")
    void buscarPorId_Sucesso() {
        Pet p = Pet.builder().idPet(5L).nmPet("Rex").build();
        when(petRepository.findById(5L)).thenReturn(Optional.of(p));

        Pet res = petService.buscarPorId(5L);
        assertThat(res.getNmPet()).isEqualTo("Rex");
    }

    @Test
    @DisplayName("Deve listar pets por tutor")
    void listarPorTutor_Sucesso() {
        when(petRepository.findByTutor_IdTutor(1L)).thenReturn(List.of(
                Pet.builder().idPet(1L).build(),
                Pet.builder().idPet(2L).build()
        ));

        List<Pet> pets = petService.listarPorTutor(1L);
        assertThat(pets).hasSize(2);
    }

    @Test
    @DisplayName("Deve listar pets próprios + pets com acesso ativo (cuidador/cônjuge) sem duplicar")
    void listarAcessiveis_IncluiProprioseCompartilhados() {
        Pet proprio = Pet.builder().idPet(1L).build();
        Pet compartilhado = Pet.builder().idPet(2L).build();
        Pet compartilhadoJaProprio = Pet.builder().idPet(1L).build(); // mesmo id do próprio: não deve duplicar

        when(petRepository.findByTutor_IdTutor(9L)).thenReturn(List.of(proprio));
        when(petAcessoRepository.findByTutor_IdTutorAndDsStatus(9L, StatusAcessoPet.ATIVO)).thenReturn(List.of(
                PetAcesso.builder().pet(compartilhado).build(),
                PetAcesso.builder().pet(compartilhadoJaProprio).build()
        ));

        List<Pet> pets = petService.listarAcessiveis(9L);

        assertThat(pets).hasSize(2);
        assertThat(pets).extracting(Pet::getIdPet).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("Deve deletar pet com sucesso quando não há eventos vinculados")
    void deletar_Sucesso() {
        Pet p = Pet.builder().idPet(1L).build();
        when(petRepository.findById(1L)).thenReturn(Optional.of(p));

        petService.deletar(1L);
        verify(petRepository).delete(p);
        verify(petRepository).flush();
    }

    @Test
    @DisplayName("Deve lançar 409 ao deletar pet com eventos de saúde vinculados")
    void deletar_ConflitoEventosVinculados() {
        Pet p = Pet.builder().idPet(1L).build();
        when(petRepository.findById(1L)).thenReturn(Optional.of(p));
        doThrow(new DataIntegrityViolationException("FK constraint")).when(petRepository).flush();

        assertThatThrownBy(() -> petService.deletar(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("existem eventos de saúde vinculados");
    }

    @Test
    @DisplayName("Deve calcular idade do pet em anos corretamente")
    void calcularIdade() {
        Pet p = Pet.builder().dtNascimento(LocalDate.now().minusYears(4).minusMonths(2)).build();
        int idade = petService.calcularIdade(p);
        assertThat(idade).isEqualTo(4);
    }
}