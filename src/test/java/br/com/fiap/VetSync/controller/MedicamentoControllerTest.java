package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.entity.Medicamento;
import br.com.fiap.VetSync.entity.Veterinario;
import br.com.fiap.VetSync.service.MedicamentoService;
import br.com.fiap.VetSync.service.VeterinarioService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MedicamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MedicamentoService medicamentoService;

    @MockBean
    private VeterinarioService veterinarioService;

    private Veterinario vetDaClinica(Long idClinica) {
        Clinica clinica = Clinica.builder().idClinica(idClinica).nmClinica("Clínica Centro").build();
        return Veterinario.builder().idVeterinario(3L).clinica(clinica).build();
    }

    @Test
    @DisplayName("POST /medicamentos - Sucesso para VETERINARIO")
    @WithMockUser(roles = "VETERINARIO")
    void criar_Sucesso() throws Exception {
        var req = new MedicamentoController.MedicamentoRequest("Amoxicilina", "Amoxicilina", new BigDecimal("50.00"), 7L);
        Medicamento med = Medicamento.builder().idMedicamento(1L).nmMedicamento("Amoxicilina").dsPrincipio("Amoxicilina").vlPrecoRef(new BigDecimal("50.00"))
                .clinica(Clinica.builder().idClinica(7L).nmClinica("Clínica Centro").build()).build();

        when(veterinarioService.buscarAutenticado(any())).thenReturn(vetDaClinica(7L));
        when(medicamentoService.criar(eq("Amoxicilina"), eq("Amoxicilina"), eq(new BigDecimal("50.00")), eq(7L))).thenReturn(med);

        mockMvc.perform(post("/medicamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idMedicamento").value(1))
                .andExpect(jsonPath("$.nmMedicamento").value("Amoxicilina"))
                .andExpect(jsonPath("$.idClinica").value(7))
                .andExpect(jsonPath("$.nmClinica").value("Clínica Centro"));
    }

    @Test
    @DisplayName("POST /medicamentos - ADMIN escolhe a clínica pelo idClinica")
    @WithMockUser(roles = "ADMIN")
    void criar_AdminEscolheClinica() throws Exception {
        var req = new MedicamentoController.MedicamentoRequest("Dipirona", null, null, 9L);
        Medicamento med = Medicamento.builder().idMedicamento(2L).nmMedicamento("Dipirona")
                .clinica(Clinica.builder().idClinica(9L).nmClinica("Clínica Sul").build()).build();
        when(medicamentoService.criar(eq("Dipirona"), isNull(), isNull(), eq(9L))).thenReturn(med);

        mockMvc.perform(post("/medicamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idClinica").value(9));
    }

    @Test
    @DisplayName("POST /medicamentos - VETERINARIO não cadastra em outra clínica (403)")
    @WithMockUser(roles = "VETERINARIO")
    void criar_VetOutraClinicaNegado() throws Exception {
        var req = new MedicamentoController.MedicamentoRequest("Amoxicilina", null, null, 9L);
        when(veterinarioService.buscarAutenticado(any())).thenReturn(vetDaClinica(7L));

        mockMvc.perform(post("/medicamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        verify(medicamentoService, never()).criar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST /medicamentos - Falha 403 para TUTOR")
    @WithMockUser(roles = "TUTOR")
    void criar_AcessoNegadoParaTutor() throws Exception {
        var req = new MedicamentoController.MedicamentoRequest("Amoxicilina", "Amoxicilina", new BigDecimal("50.00"), 7L);

        mockMvc.perform(post("/medicamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /medicamentos - Listar medicamentos")
    @WithMockUser
    void listar_Sucesso() throws Exception {
        Medicamento m = Medicamento.builder().idMedicamento(1L).nmMedicamento("Dipirona").build();
        when(medicamentoService.listar()).thenReturn(List.of(m));

        mockMvc.perform(get("/medicamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nmMedicamento").value("Dipirona"));
    }

    @Test
    @DisplayName("GET /medicamentos/{id} - Buscar por ID sucesso e 404")
    @WithMockUser
    void buscarPorId() throws Exception {
        Medicamento m = Medicamento.builder().idMedicamento(1L).nmMedicamento("Dipirona").build();
        when(medicamentoService.buscarPorId(1L)).thenReturn(m);
        when(medicamentoService.buscarPorId(99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Não encontrado"));

        mockMvc.perform(get("/medicamentos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idMedicamento").value(1));

        mockMvc.perform(get("/medicamentos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /medicamentos/{id} - ADMIN pode deletar (204)")
    @WithMockUser(roles = "ADMIN")
    void deletar_AdminSucesso() throws Exception {
        mockMvc.perform(delete("/medicamentos/1"))
                .andExpect(status().isNoContent());

        verify(medicamentoService).deletar(1L);
    }

    @Test
    @DisplayName("DELETE /medicamentos/{id} - VETERINARIO não pode deletar (403)")
    @WithMockUser(roles = "VETERINARIO")
    void deletar_VetNegado() throws Exception {
        mockMvc.perform(delete("/medicamentos/1"))
                .andExpect(status().isForbidden());
    }
}