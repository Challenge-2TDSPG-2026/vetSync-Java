package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import br.com.fiap.VetSync.service.RecompensaService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RecompensaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecompensaService recompensaService;

    @MockBean
    private TutorService tutorService;

    @MockBean
    private VeterinarioRepository veterinarioRepository;

    @Test
    @DisplayName("POST /recompensas - ADMIN cria recompensa multipart com sucesso")
    @WithMockUser(roles = "ADMIN")
    void criar_AdminSucesso() throws Exception {
        Recompensa r = Recompensa.builder()
                .idRecompensa(1L)
                .nmRecompensa("Desconto 20%")
                .dsDescricao("Vale desconto")
                .nrCustoPontos(100)
                .dsTipo(TipoRecompensa.CUPOM_DESCONTO)
                .flAtivo(true)
                .build();

        when(recompensaService.criar(eq("Desconto 20%"), eq("Vale desconto"), eq(100), eq(TipoRecompensa.CUPOM_DESCONTO), isNull()))
                .thenReturn(r);

        mockMvc.perform(multipart("/recompensas")
                        .param("nome", "Desconto 20%")
                        .param("descricao", "Vale desconto")
                        .param("custoPontos", "100")
                        .param("tipo", "CUPOM_DESCONTO"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idRecompensa").value(1))
                .andExpect(jsonPath("$.nome").value("Desconto 20%"))
                .andExpect(jsonPath("$.custoPontos").value(100));
    }

    @Test
    @DisplayName("POST /recompensas - Falha 403 para TUTOR")
    @WithMockUser(roles = "TUTOR")
    void criar_TutorNegado() throws Exception {
        mockMvc.perform(multipart("/recompensas")
                        .param("nome", "Desconto 20%")
                        .param("descricao", "Vale")
                        .param("custoPontos", "100")
                        .param("tipo", "CUPOM_DESCONTO"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /recompensas - Catálogo de recompensas ativas")
    @WithMockUser
    void listarAtivas_Sucesso() throws Exception {
        Recompensa r = Recompensa.builder()
                .idRecompensa(1L)
                .nmRecompensa("Petisco")
                .dsDescricao("Petisco sabor carne")
                .nrCustoPontos(50)
                .dsTipo(TipoRecompensa.PRODUTO)
                .flAtivo(true)
                .build();
        when(recompensaService.listarAtivas()).thenReturn(List.of(r));

        mockMvc.perform(get("/recompensas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Petisco"))
                .andExpect(jsonPath("$[0].custoPontos").value(50));
    }

    @Test
    @DisplayName("GET /recompensas/saldo - TUTOR consulta seu saldo líquido")
    @WithMockUser(username = "tutor@teste.com", roles = "TUTOR")
    void saldo_TutorSucesso() throws Exception {
        Tutor tutor = Tutor.builder().idTutor(1L).build();
        when(tutorService.buscarPorEmail("tutor@teste.com")).thenReturn(Optional.of(tutor));
        when(recompensaService.calcularSaldo(1L)).thenReturn(85);

        mockMvc.perform(get("/recompensas/saldo"))
                .andExpect(status().isOk())
                .andExpect(content().string("85"));
    }

    @Test
    @DisplayName("PATCH /recompensas/{id}/resgatar - TUTOR solicita resgate com sucesso")
    @WithMockUser(username = "tutor@teste.com", roles = "TUTOR")
    void resgatar_Sucesso() throws Exception {
        Tutor tutor = Tutor.builder().idTutor(1L).build();
        when(tutorService.buscarPorEmail("tutor@teste.com")).thenReturn(Optional.of(tutor));

        Recompensa r = Recompensa.builder().nmRecompensa("Brinquedo").nrCustoPontos(50).build();
        Resgate resgate = Resgate.builder()
                .idResgate(50L)
                .recompensa(r)
                .dsStatus(StatusResgate.PENDENTE)
                .dtResgate(LocalDateTime.now())
                .build();

        when(recompensaService.solicitarResgate(1L, 10L)).thenReturn(resgate);

        mockMvc.perform(patch("/recompensas/10/resgatar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idResgate").value(50))
                .andExpect(jsonPath("$.nmRecompensa").value("Brinquedo"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @DisplayName("PATCH /recompensas/{id}/resgatar - Falha 400 por saldo insuficiente")
    @WithMockUser(username = "tutor@teste.com", roles = "TUTOR")
    void resgatar_SaldoInsuficiente() throws Exception {
        Tutor tutor = Tutor.builder().idTutor(1L).build();
        when(tutorService.buscarPorEmail("tutor@teste.com")).thenReturn(Optional.of(tutor));
        when(recompensaService.solicitarResgate(1L, 10L))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente"));

        mockMvc.perform(patch("/recompensas/10/resgatar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Saldo insuficiente"));
    }

    @Test
    @DisplayName("PATCH /recompensas/resgates/{idResgate}/validar - VETERINARIO valida resgate")
    @WithMockUser(username = "vet@teste.com", roles = "VETERINARIO")
    void validarResgate_VetSucesso() throws Exception {
        Veterinario vet = Veterinario.builder().idVeterinario(3L).nmVeterinario("Dr. V").build();
        when(veterinarioRepository.findByDsEmail("vet@teste.com")).thenReturn(Optional.of(vet));

        Recompensa r = Recompensa.builder().nmRecompensa("Petisco").nrCustoPontos(30).build();
        Resgate resgate = Resgate.builder()
                .idResgate(50L)
                .recompensa(r)
                .veterinarioValidador(vet)
                .dsStatus(StatusResgate.VALIDADO)
                .dtResgate(LocalDateTime.now())
                .build();

        when(recompensaService.validar(50L, 3L, true)).thenReturn(resgate);

        var req = new RecompensaController.ValidarResgateRequest(true);

        mockMvc.perform(patch("/recompensas/resgates/50/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idResgate").value(50))
                .andExpect(jsonPath("$.status").value("VALIDADO"))
                .andExpect(jsonPath("$.nmVeterinarioValidador").value("Dr. V"));
    }
}
