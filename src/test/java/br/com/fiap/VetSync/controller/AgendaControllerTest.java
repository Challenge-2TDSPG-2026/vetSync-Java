package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.entity.Pet;
import br.com.fiap.VetSync.entity.Raca;
import br.com.fiap.VetSync.entity.StatusEvento;
import br.com.fiap.VetSync.entity.TipoEvento;
import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AgendaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventoSaudeRepository eventoSaudeRepository;

    private EventoSaude evento(long id, String hora, String nomePet, StatusEvento status) {
        Pet pet = Pet.builder()
                .idPet(id)
                .nmPet(nomePet)
                .raca(Raca.builder().nmRaca("Labrador").build())
                .tutor(Tutor.builder().nmTutor("Fernanda Lima").build())
                .build();
        TipoEvento tipo = TipoEvento.builder().idTipoEvento(1L).nmTipoEvento("Consulta clínica").dsCategoria("CLINICO").build();
        return EventoSaude.builder()
                .idEvento(id)
                .pet(pet)
                .tipoEvento(tipo)
                .dtEvento(LocalDate.of(2026, 10, 1))
                .hrEvento(hora)
                .dsStatus(status)
                .build();
    }

    @Test
    @DisplayName("GET /agenda/dia - ADMIN recebe os atendimentos ordenados por horário")
    @WithMockUser(roles = "ADMIN")
    void atendimentosDoDia_OrdenadoPorHorario() throws Exception {
        when(eventoSaudeRepository.findAgendaDoDia(any())).thenReturn(List.of(
                evento(2L, "15:00", "Buddy", StatusEvento.AGENDADO),
                evento(1L, "08:00", "Thor", StatusEvento.CONCLUIDO),
                evento(3L, null, "Sem Horário", StatusEvento.AGENDADO)
        ));

        mockMvc.perform(get("/agenda/dia").param("data", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nmPet").value("Thor"))
                .andExpect(jsonPath("$[0].hrEvento").value("08:00"))
                .andExpect(jsonPath("$[0].status").value("CONCLUIDO"))
                .andExpect(jsonPath("$[0].nmTutor").value("Fernanda Lima"))
                .andExpect(jsonPath("$[0].raca").value("Labrador"))
                .andExpect(jsonPath("$[0].nmTipoEvento").value("Consulta clínica"))
                .andExpect(jsonPath("$[1].nmPet").value("Buddy"))
                .andExpect(jsonPath("$[2].nmPet").value("Sem Horário"));
    }

    @Test
    @DisplayName("GET /agenda/dia - dia sem eventos devolve lista vazia")
    @WithMockUser(roles = "ADMIN")
    void atendimentosDoDia_Vazio() throws Exception {
        when(eventoSaudeRepository.findAgendaDoDia(any())).thenReturn(List.of());

        mockMvc.perform(get("/agenda/dia").param("data", "2026-10-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /agenda/dia - TUTOR não tem acesso (403)")
    @WithMockUser(roles = "TUTOR")
    void atendimentosDoDia_Tutor() throws Exception {
        mockMvc.perform(get("/agenda/dia").param("data", "2026-10-01"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /agenda/dia - não autenticado retorna 401")
    void atendimentosDoDia_NaoAutenticado() throws Exception {
        mockMvc.perform(get("/agenda/dia").param("data", "2026-10-01"))
                .andExpect(status().isUnauthorized());
    }
}