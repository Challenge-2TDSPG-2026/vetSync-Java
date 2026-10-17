package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.TipoEvento;
import br.com.fiap.VetSync.repository.TipoEventoRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TipoEventoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TipoEventoRepository tipoEventoRepository;

    @Test
    @DisplayName("GET /tipos-evento - Sucesso (autenticado)")
    @WithMockUser
    void listar_Sucesso() throws Exception {
        TipoEvento t1 = TipoEvento.builder().idTipoEvento(1L).nmTipoEvento("Vacina Raiva").dsCategoria("PREVENTIVO").nrPontos(15).build();
        TipoEvento t2 = TipoEvento.builder().idTipoEvento(2L).nmTipoEvento("Consulta Geral").dsCategoria("CLINICO").nrPontos(10).build();
        when(tipoEventoRepository.findByClinicaIsNull()).thenReturn(List.of(t1, t2));

        mockMvc.perform(get("/tipos-evento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idTipoEvento").value(1))
                .andExpect(jsonPath("$[0].nmTipoEvento").value("Vacina Raiva"))
                .andExpect(jsonPath("$[0].dsCategoria").value("PREVENTIVO"))
                .andExpect(jsonPath("$[0].nrPontos").value(15))
                .andExpect(jsonPath("$[1].idTipoEvento").value(2));
    }

    @Test
    @DisplayName("GET /tipos-evento - Não autenticado retorna 401 Unauthorized")
    void listar_NaoAutenticado() throws Exception {
        mockMvc.perform(get("/tipos-evento"))
                .andExpect(status().isUnauthorized());
    }
}
