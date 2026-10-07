package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.service.PrescricaoService;
import br.com.fiap.VetSync.service.VeterinarioService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PrescricaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PrescricaoService prescricaoService;

    @MockBean
    private VeterinarioService veterinarioService;

    @MockBean
    private AdminRepository adminRepository;

    @Test
    @DisplayName("POST /prescricoes - VETERINARIO solicita medicamento com sucesso")
    @WithMockUser(username = "vet@teste.com", roles = "VETERINARIO")
    void solicitar_Sucesso() throws Exception {
        Veterinario vet = Veterinario.builder().idVeterinario(3L).build();
        when(veterinarioService.buscarAutenticado(any())).thenReturn(vet);

        Medicamento med = Medicamento.builder().nmMedicamento("Anti-inflamatório").build();
        Prescricao p = Prescricao.builder()
                .idPrescricao(1L)
                .dsStatus(StatusPrescricao.SOLICITADO)
                .medicamento(med)
                .dsPosologia("1 comp/dia")
                .dtInicio(LocalDate.now())
                .qtDosesDia(1)
                .build();

        when(prescricaoService.solicitar(eq(10L), eq(20L), eq("1 comp/dia"), any(), isNull(), eq(1), eq(3L), isNull()))
                .thenReturn(p);

        mockMvc.perform(multipart("/prescricoes")
                        .param("idEvento", "10")
                        .param("idMedicamento", "20")
                        .param("dsPosologia", "1 comp/dia")
                        .param("dtInicio", LocalDate.now().toString())
                        .param("qtDosesDia", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPrescricao").value(1))
                .andExpect(jsonPath("$.status").value("SOLICITADO"))
                .andExpect(jsonPath("$.nmMedicamento").value("Anti-inflamatório"));
    }

    @Test
    @DisplayName("POST /prescricoes - Falha 403 para TUTOR")
    @WithMockUser(roles = "TUTOR")
    void solicitar_TutorNegado() throws Exception {
        mockMvc.perform(multipart("/prescricoes")
                        .param("idEvento", "10")
                        .param("idMedicamento", "20")
                        .param("dsPosologia", "1 comp/dia")
                        .param("dtInicio", LocalDate.now().toString())
                        .param("qtDosesDia", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /prescricoes - Listar prescrições para ADMIN (pendentes)")
    @WithMockUser(roles = "ADMIN")
    void listar_Admin() throws Exception {
        Prescricao p = Prescricao.builder().idPrescricao(5L).dsStatus(StatusPrescricao.SOLICITADO).build();
        when(prescricaoService.listarPendentes()).thenReturn(List.of(p));

        mockMvc.perform(get("/prescricoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idPrescricao").value(5))
                .andExpect(jsonPath("$[0].status").value("SOLICITADO"));
    }

    @Test
    @DisplayName("PATCH /prescricoes/{id}/liberar - ADMIN libera prescrição")
    @WithMockUser(username = "admin@teste.com", roles = "ADMIN")
    void liberar_AdminSucesso() throws Exception {
        Admin admin = Admin.builder().idAdmin(1L).build();
        when(adminRepository.findByDsEmail("admin@teste.com")).thenReturn(Optional.of(admin));

        Prescricao liberada = Prescricao.builder()
                .idPrescricao(10L)
                .dsStatus(StatusPrescricao.LIBERADO)
                .build();

        when(prescricaoService.liberar(10L, 1L, true)).thenReturn(liberada);

        var req = new PrescricaoController.PrescricaoLiberarRequest(true);

        mockMvc.perform(patch("/prescricoes/10/liberar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPrescricao").value(10))
                .andExpect(jsonPath("$.status").value("LIBERADO"));
    }
}
