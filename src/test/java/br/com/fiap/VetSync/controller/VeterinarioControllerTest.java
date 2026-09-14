package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.BloqueioAgenda;
import br.com.fiap.VetSync.entity.Disponibilidade;
import br.com.fiap.VetSync.security.VeterinarioSecurity;
import br.com.fiap.VetSync.service.AgendaService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class VeterinarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VeterinarioService veterinarioService;

    @MockBean
    private AgendaService agendaService;

    @MockBean
    private VeterinarioSecurity veterinarioSecurity;

    @Test
    @DisplayName("GET /veterinarios - Listar todos os veterinários")
    @WithMockUser
    void listar_Sucesso() throws Exception {
        Veterinario vet = Veterinario.builder().idVeterinario(1L).nmVeterinario("Dr. Pedro").nrCrmv("123456").build();
        when(veterinarioService.listarTodos()).thenReturn(List.of(vet));

        mockMvc.perform(get("/veterinarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idVeterinario").value(1))
                .andExpect(jsonPath("$[0].nmVeterinario").value("Dr. Pedro"));
    }

    @Test
    @DisplayName("POST /veterinarios - ADMIN cadastra veterinário com sucesso (201)")
    @WithMockUser(roles = "ADMIN")
    void cadastrar_AdminSucesso() throws Exception {
        Veterinario vet = Veterinario.builder().idVeterinario(10L).nmVeterinario("Dr. Lucas").dsEmail("lucas@vet.com").nrCrmv("654321").build();
        when(veterinarioService.cadastrar("Dr. Lucas", "lucas@vet.com", 1L))
                .thenReturn(new VeterinarioService.NovoVeterinario(vet, "TempPwd123"));

        var req = new VeterinarioController.VeterinarioRequest("Dr. Lucas", "lucas@vet.com", 1L);

        mockMvc.perform(post("/veterinarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idVeterinario").value(10))
                .andExpect(jsonPath("$.crm").value("654321"))
                .andExpect(jsonPath("$.senhaTemporaria").value("TempPwd123"));
    }

    @Test
    @DisplayName("POST /veterinarios - TUTOR não tem permissão (403)")
    @WithMockUser(roles = "TUTOR")
    void cadastrar_TutorNegado() throws Exception {
        var req = new VeterinarioController.VeterinarioRequest("Dr. Lucas", "lucas@vet.com", 1L);

        mockMvc.perform(post("/veterinarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /veterinarios/{id}/disponibilidade - Próprio veterinário adiciona (201)")
    @WithMockUser(username = "vet@teste.com", roles = "VETERINARIO")
    void adicionarDisponibilidade_Sucesso() throws Exception {
        when(veterinarioSecurity.isSelf(eq(1L), any())).thenReturn(true);

        Disponibilidade disp = Disponibilidade.builder().idDisponibilidade(5L).nrDiaSemana(2).hrInicio("08:00").hrFim("12:00").build();
        when(agendaService.adicionarDisponibilidade(1L, 2, "08:00", "12:00")).thenReturn(disp);

        var req = new VeterinarioController.DisponibilidadeRequest(2, "08:00", "12:00");

        mockMvc.perform(post("/veterinarios/1/disponibilidade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idDisponibilidade").value(5))
                .andExpect(jsonPath("$.nrDiaSemana").value(2));
    }

    @Test
    @DisplayName("DELETE /veterinarios/{id}/disponibilidade/{idDisp} - Próprio veterinário remove (204)")
    @WithMockUser(username = "vet@teste.com", roles = "VETERINARIO")
    void removerDisponibilidade_Sucesso() throws Exception {
        when(veterinarioSecurity.isSelf(eq(1L), any())).thenReturn(true);

        mockMvc.perform(delete("/veterinarios/1/disponibilidade/5"))
                .andExpect(status().isNoContent());

        verify(agendaService).removerDisponibilidade(1L, 5L);
    }

    @Test
    @DisplayName("POST /veterinarios/{id}/bloqueios - Próprio veterinário adiciona bloqueio (201)")
    @WithMockUser(username = "vet@teste.com", roles = "VETERINARIO")
    void adicionarBloqueio_Sucesso() throws Exception {
        when(veterinarioSecurity.isSelf(eq(1L), any())).thenReturn(true);

        LocalDate inicio = LocalDate.now().plusDays(1);
        LocalDate fim = LocalDate.now().plusDays(3);
        BloqueioAgenda bloqueio = BloqueioAgenda.builder().idBloqueio(12L).dtInicio(inicio).dtFim(fim).dsMotivo("Congresso").build();

        when(agendaService.adicionarBloqueio(1L, inicio, fim, "Congresso")).thenReturn(bloqueio);

        var req = new VeterinarioController.BloqueioRequest(inicio, fim, "Congresso");

        mockMvc.perform(post("/veterinarios/1/bloqueios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idBloqueio").value(12))
                .andExpect(jsonPath("$.motivo").value("Congresso"));
    }

    @Test
    @DisplayName("DELETE /veterinarios/{id}/bloqueios/{idBloqueio} - Próprio veterinário remove bloqueio (204)")
    @WithMockUser(username = "vet@teste.com", roles = "VETERINARIO")
    void removerBloqueio_Sucesso() throws Exception {
        when(veterinarioSecurity.isSelf(eq(1L), any())).thenReturn(true);

        mockMvc.perform(delete("/veterinarios/1/bloqueios/12"))
                .andExpect(status().isNoContent());

        verify(agendaService).removerBloqueio(1L, 12L);
    }
}
