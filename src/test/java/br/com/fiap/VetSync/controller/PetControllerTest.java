package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.service.PetService;
import br.com.fiap.VetSync.service.TutorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PetService petService;

    @MockBean
    private TutorService tutorService;

    @MockBean
    private br.com.fiap.VetSync.security.PetSecurity petSecurity;

    @Test
    @DisplayName("POST /pets - Cadastrar pet com sucesso pelo TUTOR")
    @WithMockUser(username = "tutor@teste.com", roles = "TUTOR")
    void cadastrar_Sucesso() throws Exception {
        Tutor tutor = Tutor.builder().idTutor(1L).dsEmail("tutor@teste.com").build();
        when(tutorService.buscarPorEmail("tutor@teste.com")).thenReturn(Optional.of(tutor));

        Especie esp = Especie.builder().nmEspecie("Cão").build();
        Raca raca = Raca.builder().nmRaca("Pug").especie(esp).build();
        Pet petSalvo = Pet.builder().idPet(10L).nmPet("Frank").raca(raca).tutor(tutor).dtNascimento(LocalDate.now().minusYears(2)).nrPesoKg(new BigDecimal("7.5")).build();

        when(petService.cadastrar(any(), eq(1L), eq(EspecieCategoria.CAO), any(), eq("Pug"))).thenReturn(petSalvo);
        when(petService.calcularIdade(petSalvo)).thenReturn(2);

        var req = new PetController.PetRequest("Frank", EspecieCategoria.CAO, null, "Pug", LocalDate.now().minusYears(2), new BigDecimal("7.5"), "M");

        mockMvc.perform(post("/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPet").value(10))
                .andExpect(jsonPath("$.nmPet").value("Frank"))
                .andExpect(jsonPath("$.idadeAnos").value(2))
                .andExpect(jsonPath("$.especie").value("Cão"));
    }

    @Test
    @DisplayName("GET /pets - Listar meus pets (TUTOR)")
    @WithMockUser(username = "tutor@teste.com", roles = "TUTOR")
    void listarMeusPets_Sucesso() throws Exception {
        Tutor tutor = Tutor.builder().idTutor(1L).dsEmail("tutor@teste.com").build();
        when(tutorService.buscarPorEmail("tutor@teste.com")).thenReturn(Optional.of(tutor));

        Pet pet = Pet.builder().idPet(1L).nmPet("Totó").dtNascimento(LocalDate.now().minusYears(1)).build();
        when(petService.listarAcessiveis(1L)).thenReturn(List.of(pet));

        mockMvc.perform(get("/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nmPet").value("Totó"));
    }

    @Test
    @DisplayName("GET /pets/tutor/{idTutor} - VETERINARIO pode listar pets de um tutor específico")
    @WithMockUser(roles = "VETERINARIO")
    void listarPorTutor_VetSucesso() throws Exception {
        Pet pet = Pet.builder().idPet(2L).nmPet("Mel").dtNascimento(LocalDate.now().minusYears(3)).build();
        when(petService.listarPorTutor(5L)).thenReturn(List.of(pet));

        mockMvc.perform(get("/pets/tutor/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nmPet").value("Mel"));
    }

    @Test
    @DisplayName("GET /pets/tutor/{idTutor} - TUTOR não pode acessar lista de outro tutor -> 403 Forbidden")
    @WithMockUser(roles = "TUTOR")
    void listarPorTutor_TutorNegado() throws Exception {
        mockMvc.perform(get("/pets/tutor/5"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /pets/{id} - Conflito 409 quando pet possui eventos vinculados")
    @WithMockUser(username = "tutor@teste.com", roles = "TUTOR")
    void deletar_ConflitoEventos() throws Exception {
        when(petSecurity.isOwner(eq(1L), any())).thenReturn(true);
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "existem eventos de saúde vinculados"))
                .when(petService).deletar(1L);

        mockMvc.perform(delete("/pets/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem").value("existem eventos de saúde vinculados"));
    }
}