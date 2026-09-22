package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.security.PetAccessSecurity;
import br.com.fiap.VetSync.service.EventoService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EventoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventoService eventoService;

    // Substitui o bean real (que consultaria o banco de verdade) para os testes
    // conseguirem simular proprietário/cuidador sem persistir Pet/PetAcesso.
    @MockBean
    private PetAccessSecurity petAccessSecurity;

    @Test
    @DisplayName("POST /eventos - Agendar com sucesso por TUTOR proprietário")
    @WithMockUser(username = "tutor@teste.com", roles = "TUTOR")
    void agendar_Sucesso() throws Exception {
        Tutor tutor = Tutor.builder().dsEmail("tutor@teste.com").build();
        Pet pet = Pet.builder().idPet(1L).tutor(tutor).build();
        TipoEvento tipo = TipoEvento.builder().idTipoEvento(2L).nmTipoEvento("Vacina").build();
        Veterinario vet = Veterinario.builder().idVeterinario(3L).nmVeterinario("Dr. V").build();

        when(petAccessSecurity.canEdit(eq(1L), any())).thenReturn(true);

        EventoSaude eventoCriado = EventoSaude.builder()
                .idEvento(100L)
                .pet(pet)
                .tipoEvento(tipo)
                .veterinario(vet)
                .dtEvento(LocalDate.now().plusDays(2))
                .hrEvento("14:30")
                .dsStatus(StatusEvento.AGENDADO)
                .build();

        when(eventoService.agendar(any(), eq(1L), eq(2L), eq(3L))).thenReturn(eventoCriado);

        var req = new EventoController.EventoAgendarRequest(1L, 2L, 3L, LocalDate.now().plusDays(2), "14:30", "Obs");

        mockMvc.perform(post("/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idEvento").value(100))
                .andExpect(jsonPath("$.status").value("AGENDADO"))
                .andExpect(jsonPath("$.nmTipoEvento").value("Vacina"));
    }

    @Test
    @DisplayName("POST /eventos - Agendar com sucesso por cuidador com permissão EDICAO")
    @WithMockUser(username = "cuidador@teste.com", roles = "TUTOR")
    void agendar_SucessoComoCuidadorEdicao() throws Exception {
        Tutor dono = Tutor.builder().dsEmail("dono@teste.com").build();
        Pet pet = Pet.builder().idPet(1L).tutor(dono).build();
        TipoEvento tipo = TipoEvento.builder().idTipoEvento(2L).nmTipoEvento("Vacina").build();
        Veterinario vet = Veterinario.builder().idVeterinario(3L).nmVeterinario("Dr. V").build();

        when(petAccessSecurity.canEdit(eq(1L), any())).thenReturn(true);

        EventoSaude eventoCriado = EventoSaude.builder()
                .idEvento(101L)
                .pet(pet)
                .tipoEvento(tipo)
                .veterinario(vet)
                .dtEvento(LocalDate.now().plusDays(2))
                .hrEvento("14:30")
                .dsStatus(StatusEvento.AGENDADO)
                .build();

        when(eventoService.agendar(any(), eq(1L), eq(2L), eq(3L))).thenReturn(eventoCriado);

        var req = new EventoController.EventoAgendarRequest(1L, 2L, 3L, LocalDate.now().plusDays(2), "14:30", "Obs");

        mockMvc.perform(post("/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idEvento").value(101));
    }

    @Test
    @DisplayName("POST /eventos - Falha com 403 se tutor não é dono nem tem acesso EDICAO ao pet")
    @WithMockUser(username = "outro_tutor@teste.com", roles = "TUTOR")
    void agendar_SemPermissaoNoPet() throws Exception {
        when(petAccessSecurity.canEdit(eq(1L), any())).thenReturn(false);

        var req = new EventoController.EventoAgendarRequest(1L, 2L, 3L, LocalDate.now().plusDays(2), "14:30", "Obs");

        mockMvc.perform(post("/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Você não tem permissão para agendar eventos para esse pet"));
    }

    @Test
    @DisplayName("GET /eventos - Listar eventos do tutor")
    @WithMockUser(username = "tutor@teste.com", roles = "TUTOR")
    void listar_Tutor() throws Exception {
        TipoEvento tipo = TipoEvento.builder().nmTipoEvento("Banho").build();
        EventoSaude ev = EventoSaude.builder().idEvento(10L).tipoEvento(tipo).dsStatus(StatusEvento.AGENDADO).build();
        when(eventoService.listarParaTutor("tutor@teste.com")).thenReturn(List.of(ev));

        mockMvc.perform(get("/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idEvento").value(10))
                .andExpect(jsonPath("$[0].nmTipoEvento").value("Banho"));
    }

    @Test
    @DisplayName("GET /eventos/pet/{idPet}/gasto-total - Retorna total gasto para quem pode ver o pet")
    @WithMockUser(roles = "TUTOR")
    void gastoTotal_Sucesso() throws Exception {
        when(petAccessSecurity.canView(eq(1L), any())).thenReturn(true);
        when(eventoService.calcularGastoTotal(1L)).thenReturn(new BigDecimal("250.00"));

        mockMvc.perform(get("/eventos/pet/1/gasto-total"))
                .andExpect(status().isOk())
                .andExpect(content().string("250.00"));
    }

    @Test
    @DisplayName("GET /eventos/pet/{idPet}/gasto-total - 403 para quem não tem acesso ao pet")
    @WithMockUser(roles = "TUTOR")
    void gastoTotal_SemAcesso() throws Exception {
        when(petAccessSecurity.canView(eq(1L), any())).thenReturn(false);

        mockMvc.perform(get("/eventos/pet/1/gasto-total"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /eventos/pet/{idPet}/alertas - Retorna lista de alertas para quem pode ver o pet")
    @WithMockUser(roles = "TUTOR")
    void alertas_Sucesso() throws Exception {
        when(petAccessSecurity.canView(eq(1L), any())).thenReturn(true);
        var alerta = new EventoService.AlertaEvento("Vacina", LocalDate.now().minusMonths(13), 13, true, "Atrasado");
        when(eventoService.gerarAlertas(1L)).thenReturn(List.of(alerta));

        mockMvc.perform(get("/eventos/pet/1/alertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nmTipoEvento").value("Vacina"))
                .andExpect(jsonPath("$[0].atrasado").value(true));
    }
}