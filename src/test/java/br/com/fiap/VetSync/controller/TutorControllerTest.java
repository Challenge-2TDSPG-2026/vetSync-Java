package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.security.TutorSecurity;
import br.com.fiap.VetSync.service.TutorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TutorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TutorService tutorService;

    @MockBean
    private TutorSecurity tutorSecurity;

    @Test
    @DisplayName("GET /tutores - Sucesso para VETERINARIO")
    @WithMockUser(roles = "VETERINARIO")
    void listarTodos_VetSucesso() throws Exception {
        Tutor t = Tutor.builder().idTutor(1L).nmTutor("Maria").dsEmail("maria@email.com").build();
        when(tutorService.listarTodos()).thenReturn(List.of(t));

        mockMvc.perform(get("/tutores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idTutor").value(1))
                .andExpect(jsonPath("$[0].nmTutor").value("Maria"));
    }

    @Test
    @DisplayName("GET /tutores - 403 Forbidden para TUTOR")
    @WithMockUser(roles = "TUTOR")
    void listarTodos_TutorNegado() throws Exception {
        mockMvc.perform(get("/tutores"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /tutores/{id} - Sucesso para o próprio tutor")
    @WithMockUser(username = "maria@email.com", roles = "TUTOR")
    void buscarPorId_ProprioTutor() throws Exception {
        when(tutorSecurity.isSelf(eq(1L), any())).thenReturn(true);
        Tutor t = Tutor.builder().idTutor(1L).nmTutor("Maria").dsEmail("maria@email.com").build();
        when(tutorService.buscarPorId(1L)).thenReturn(t);

        mockMvc.perform(get("/tutores/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTutor").value(1))
                .andExpect(jsonPath("$.nmTutor").value("Maria"));
    }

    @Test
    @DisplayName("GET /tutores/{id} - 403 Forbidden para outro tutor")
    @WithMockUser(username = "joao@email.com", roles = "TUTOR")
    void buscarPorId_OutroTutorNegado() throws Exception {
        when(tutorSecurity.isSelf(eq(1L), any())).thenReturn(false);

        mockMvc.perform(get("/tutores/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /tutores/{id} - Atualizar dados do tutor")
    @WithMockUser(username = "maria@email.com", roles = "TUTOR")
    void atualizar_Sucesso() throws Exception {
        when(tutorSecurity.isSelf(eq(1L), any())).thenReturn(true);
        Tutor t = Tutor.builder().idTutor(1L).nmTutor("Maria Silva").nrTelefone("11988887777").build();
        when(tutorService.atualizar(eq(1L), any(Tutor.class))).thenReturn(t);

        var req = new TutorController.TutorAtualizarRequest("Maria Silva", "11988887777", "01310100", "Avenida Paulista", "1000", "Apto 10", "Bela Vista", "São Paulo", "SP");

        mockMvc.perform(put("/tutores/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nmTutor").value("Maria Silva"))
                .andExpect(jsonPath("$.nrTelefone").value("11988887777"));
    }

    @Test
    @DisplayName("DELETE /tutores/{id} - Deletar conta do próprio tutor (204)")
    @WithMockUser(username = "maria@email.com", roles = "TUTOR")
    void deletar_Sucesso() throws Exception {
        when(tutorSecurity.isSelf(eq(1L), any())).thenReturn(true);

        mockMvc.perform(delete("/tutores/1"))
                .andExpect(status().isNoContent());

        verify(tutorService).deletar(1L);
    }
}
