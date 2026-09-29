package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.repository.TutorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TutorServiceTest {

    @Mock
    private TutorRepository tutorRepository;

    @InjectMocks
    private TutorService tutorService;

    @Test
    @DisplayName("Deve cadastrar tutor com sucesso")
    void cadastrar_Sucesso() {
        Tutor tutor = Tutor.builder().dsEmail("tutor@teste.com").nmTutor("Maria").build();
        when(tutorRepository.findByDsEmail("tutor@teste.com")).thenReturn(Optional.empty());
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(inv -> inv.getArgument(0));

        Tutor salvo = tutorService.cadastrar(tutor);
        assertThat(salvo.getNmTutor()).isEqualTo("Maria");
    }

    @Test
    @DisplayName("Deve falhar ao cadastrar tutor com e-mail duplicado")
    void cadastrar_EmailDuplicado() {
        Tutor tutor = Tutor.builder().dsEmail("duplicado@teste.com").build();
        when(tutorRepository.findByDsEmail("duplicado@teste.com")).thenReturn(Optional.of(new Tutor()));

        assertThatThrownBy(() -> tutorService.cadastrar(tutor))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Já existe um tutor com o e-mail");

        verify(tutorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar tutor por ID com sucesso")
    void buscarPorId_Sucesso() {
        Tutor t = Tutor.builder().idTutor(1L).nmTutor("João").build();
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(t));

        Tutor res = tutorService.buscarPorId(1L);
        assertThat(res.getNmTutor()).isEqualTo("João");
    }

    @Test
    @DisplayName("Deve lançar 404 ao buscar tutor inexistente")
    void buscarPorId_NaoEncontrado() {
        when(tutorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tutorService.buscarPorId(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tutor não encontrado");
    }

    @Test
    @DisplayName("Deve atualizar dados do tutor com sucesso")
    void atualizar_Sucesso() {
        Tutor t = Tutor.builder().idTutor(1L).nmTutor("Antigo").nrTelefone("11999991111").build();
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(t));
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(inv -> inv.getArgument(0));

        Tutor novo = Tutor.builder().nmTutor("Novo Nome").nrTelefone("11999992222").build();
        Tutor res = tutorService.atualizar(1L, novo);

        assertThat(res.getNmTutor()).isEqualTo("Novo Nome");
        assertThat(res.getNrTelefone()).isEqualTo("11999992222");
    }

    @Test
    @DisplayName("Deve deletar tutor com sucesso")
    void deletar_Sucesso() {
        Tutor t = Tutor.builder().idTutor(1L).build();
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(t));

        tutorService.deletar(1L);
        verify(tutorRepository).delete(t);
    }
}
