package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.service.PainelAdminService;
import br.com.fiap.VetSync.service.PainelAdminService.PontosPainel;
import br.com.fiap.VetSync.service.PainelAdminService.RecompensasPainel;
import br.com.fiap.VetSync.service.PainelAdminService.ResumoPainel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PainelAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PainelAdminService painelAdminService;

    private static final PontosPainel PONTOS = new PontosPainel(30, 20, 50, 100, 0, 40, 60, 1, 1);
    private static final RecompensasPainel RECOMPENSAS = new RecompensasPainel(1, 1, 2, 1, 0, 0);

    @Test
    @DisplayName("GET /painel/resumo - sem filtro devolve totais globais rotulados")
    @WithMockUser(roles = "ADMIN")
    void resumoGlobal() throws Exception {
        when(painelAdminService.resumo(null)).thenReturn(new ResumoPainel("GLOBAL", true, null, null,
                "Totais de todas as clínicas", PONTOS, RECOMPENSAS, List.of()));

        mockMvc.perform(get("/painel/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escopo").value("GLOBAL"))
                .andExpect(jsonPath("$.totaisGlobais").value(true))
                .andExpect(jsonPath("$.rotuloEscopo").value("Totais de todas as clínicas"))
                .andExpect(jsonPath("$.pontos.pontosDisponiveis").value(60));
    }

    @Test
    @DisplayName("GET /painel/resumo?idClinica= - devolve só a clínica filtrada")
    @WithMockUser(roles = "ADMIN")
    void resumoPorClinica() throws Exception {
        when(painelAdminService.resumo(7L)).thenReturn(new ResumoPainel("CLINICA", false, 7L, "Centro",
                "Centro", PONTOS, RECOMPENSAS, List.of()));

        mockMvc.perform(get("/painel/resumo").param("idClinica", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escopo").value("CLINICA"))
                .andExpect(jsonPath("$.totaisGlobais").value(false))
                .andExpect(jsonPath("$.idClinica").value(7))
                .andExpect(jsonPath("$.recompensas.recompensasAtivas").value(1));
    }

    @Test
    @DisplayName("GET /painel/resumo - TUTOR recebe 403")
    @WithMockUser(roles = "TUTOR")
    void resumoTutorNegado() throws Exception {
        mockMvc.perform(get("/painel/resumo")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /painel/resumo - sem autenticação recebe 401")
    void resumoSemAutenticacao() throws Exception {
        mockMvc.perform(get("/painel/resumo")).andExpect(status().isUnauthorized());
    }
}