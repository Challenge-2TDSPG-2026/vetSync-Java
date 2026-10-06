package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.service.PontosService;
import br.com.fiap.VetSync.service.TutorService;
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
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PontosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PontosService pontosService;

    @MockBean
    private AdminRepository adminRepository;

    @MockBean
    private TutorService tutorService;

    @Test
    @DisplayName("GET /pontos - ADMIN vê lançamentos de todas as clínicas")
    @WithMockUser(roles = "ADMIN")
    void listar_Admin() throws Exception {
        LancamentoPontos lanc = LancamentoPontos.builder()
                .idLancamento(1L)
                .dsStatus(StatusLancamentoPontos.PENDENTE)
                .nrPontos(30)
                .dtLancamento(LocalDate.now())
                .build();
        when(pontosService.listar(null, null, null)).thenReturn(List.of(lanc));

        mockMvc.perform(get("/pontos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idLancamento").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$[0].nrPontos").value(30));
    }

    @Test
    @DisplayName("GET /pontos - TUTOR vê os próprios lançamentos")
    @WithMockUser(username = "tutor@teste.com", roles = "TUTOR")
    void listar_Tutor() throws Exception {
        LancamentoPontos lanc = LancamentoPontos.builder()
                .idLancamento(2L)
                .dsStatus(StatusLancamentoPontos.LIBERADO)
                .nrPontos(15)
                .dtLancamento(LocalDate.now())
                .build();
        when(pontosService.listarParaTutor("tutor@teste.com", null, null)).thenReturn(List.of(lanc));

        mockMvc.perform(get("/pontos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idLancamento").value(2))
                .andExpect(jsonPath("$[0].status").value("LIBERADO"));
    }

    @Test
    @DisplayName("PATCH /pontos/{id}/liberar - ADMIN libera pontos com sucesso")
    @WithMockUser(username = "admin@teste.com", roles = "ADMIN")
    void liberar_AdminSucesso() throws Exception {
        Admin admin = Admin.builder().idAdmin(5L).dsEmail("admin@teste.com").build();
        when(adminRepository.findByDsEmail("admin@teste.com")).thenReturn(Optional.of(admin));

        LancamentoPontos liberado = LancamentoPontos.builder()
                .idLancamento(10L)
                .dsStatus(StatusLancamentoPontos.LIBERADO)
                .nrPontos(50)
                .dtLancamento(LocalDate.now())
                .build();

        when(pontosService.liberar(10L, 7L, 5L)).thenReturn(liberado);

        mockMvc.perform(patch("/pontos/10/liberar").param("idClinica", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idLancamento").value(10))
                .andExpect(jsonPath("$.status").value("LIBERADO"));
    }

    @Test
    @DisplayName("PATCH /pontos/{id}/liberar - Falha 403 para TUTOR")
    @WithMockUser(roles = "TUTOR")
    void liberar_TutorNegado() throws Exception {
        mockMvc.perform(patch("/pontos/10/liberar").param("idClinica", "7"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /pontos?idClinica&status - ADMIN filtra e recebe clínica, tutor, atendimento, liberação e validade")
    @WithMockUser(roles = "ADMIN")
    void listar_AdminComFiltroEDetalhes() throws Exception {
        Clinica clinica = Clinica.builder().idClinica(7L).nmClinica("Clínica Centro").build();
        Tutor tutor = Tutor.builder().idTutor(3L).nmTutor("Ana").build();
        Pet pet = Pet.builder().idPet(4L).nmPet("Rex").tutor(tutor).build();
        TipoEvento tipo = TipoEvento.builder().nmTipoEvento("Realizar vacinação").build();
        EventoSaude evento = EventoSaude.builder().idEvento(9L).pet(pet).tipoEvento(tipo)
                .dtEvento(LocalDate.of(2026, 9, 1)).build();
        LancamentoPontos lanc = LancamentoPontos.builder()
                .idLancamento(1L).evento(evento).clinica(clinica).nrPontos(100)
                .dsStatus(StatusLancamentoPontos.LIBERADO)
                .dtLancamento(LocalDate.of(2026, 9, 1))
                .dtLiberacao(LocalDate.now()).dtValidade(LocalDate.now().plusDays(365))
                .build();
        when(pontosService.listar(7L, StatusLancamentoPontos.LIBERADO, null)).thenReturn(List.of(lanc));

        mockMvc.perform(get("/pontos").param("idClinica", "7").param("status", "LIBERADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idClinica").value(7))
                .andExpect(jsonPath("$[0].nmClinica").value("Clínica Centro"))
                .andExpect(jsonPath("$[0].idTutor").value(3))
                .andExpect(jsonPath("$[0].nmTutor").value("Ana"))
                .andExpect(jsonPath("$[0].dsAtendimento").value("Realizar vacinação"))
                .andExpect(jsonPath("$[0].dtAtendimento").value("2026-09-01"))
                .andExpect(jsonPath("$[0].dtLiberacao").exists())
                .andExpect(jsonPath("$[0].dtValidade").exists())
                .andExpect(jsonPath("$[0].resgatavel").value(true));
    }

    @Test
    @DisplayName("GET /pontos - lançamento LIBERADO vencido sai como EXPIRADO e não resgatável")
    @WithMockUser(roles = "ADMIN")
    void listar_ExpiradoNaoResgatavel() throws Exception {
        Clinica clinica = Clinica.builder().idClinica(7L).nmClinica("Clínica Centro").build();
        LancamentoPontos lanc = LancamentoPontos.builder()
                .idLancamento(1L).clinica(clinica).nrPontos(100)
                .dsStatus(StatusLancamentoPontos.LIBERADO)
                .dtLancamento(LocalDate.now().minusDays(400))
                .dtLiberacao(LocalDate.now().minusDays(400)).dtValidade(LocalDate.now().minusDays(35))
                .build();
        when(pontosService.listar(null, null, null)).thenReturn(List.of(lanc));

        mockMvc.perform(get("/pontos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("EXPIRADO"))
                .andExpect(jsonPath("$[0].resgatavel").value(false));
    }

    @Test
    @DisplayName("PATCH /pontos/{id}/liberar - exige idClinica")
    @WithMockUser(username = "admin@teste.com", roles = "ADMIN")
    void liberar_SemClinica() throws Exception {
        mockMvc.perform(patch("/pontos/10/liberar"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /pontos/{id}/bloquear - ADMIN bloqueia com motivo")
    @WithMockUser(username = "admin@teste.com", roles = "ADMIN")
    void bloquear_Admin() throws Exception {
        Admin admin = Admin.builder().idAdmin(5L).dsEmail("admin@teste.com").build();
        when(adminRepository.findByDsEmail("admin@teste.com")).thenReturn(Optional.of(admin));
        LancamentoPontos bloqueado = LancamentoPontos.builder()
                .idLancamento(10L).nrPontos(50).dsStatus(StatusLancamentoPontos.BLOQUEADO)
                .dsMotivoBloqueio("Fraude").dtLancamento(LocalDate.now()).build();
        when(pontosService.bloquear(10L, 7L, "Fraude", 5L)).thenReturn(bloqueado);

        mockMvc.perform(patch("/pontos/10/bloquear").param("idClinica", "7")
                        .contentType("application/json").content("{\"motivo\":\"Fraude\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOQUEADO"))
                .andExpect(jsonPath("$.motivoBloqueio").value("Fraude"))
                .andExpect(jsonPath("$.resgatavel").value(false));
    }

    @Test
    @DisplayName("PATCH /pontos/{id}/bloquear - motivo vazio retorna 400 e TUTOR recebe 403")
    void bloquear_Validacoes() throws Exception {
        mockMvc.perform(patch("/pontos/10/bloquear").param("idClinica", "7").with(user("admin@teste.com").roles("ADMIN"))
                        .contentType("application/json").content("{\"motivo\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/pontos/10/bloquear").param("idClinica", "7").with(user("t@teste.com").roles("TUTOR"))
                        .contentType("application/json").content("{\"motivo\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /pontos/saldos - uma linha por tutor e clínica, sem somar clínicas")
    @WithMockUser(roles = "ADMIN")
    void saldos_Admin() throws Exception {
        Tutor tutor = Tutor.builder().idTutor(3L).nmTutor("Ana").build();
        Clinica c7 = Clinica.builder().idClinica(7L).nmClinica("Centro").build();
        Clinica c8 = Clinica.builder().idClinica(8L).nmClinica("Norte").build();
        when(pontosService.listarSaldos(null, null)).thenReturn(List.of(
                new PontosService.SaldoTutorClinica(tutor, c7, new PontosService.SaldoPontos(10, 200, 20, 30, 50, 80, 70)),
                new PontosService.SaldoTutorClinica(tutor, c8, new PontosService.SaldoPontos(0, 40, 0, 0, 0, 0, 40))));

        mockMvc.perform(get("/pontos/saldos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].idClinica").value(7))
                .andExpect(jsonPath("$[0].saldoDisponivel").value(70))
                .andExpect(jsonPath("$[0].pontosReservados").value(80))
                .andExpect(jsonPath("$[1].idClinica").value(8))
                .andExpect(jsonPath("$[1].saldoDisponivel").value(40));
    }

    @Test
    @DisplayName("GET /pontos/saldos - TUTOR só enxerga o próprio saldo (ignora idTutor)")
    @WithMockUser(username = "tutor@teste.com", roles = "TUTOR")
    void saldos_Tutor() throws Exception {
        when(tutorService.buscarPorEmail("tutor@teste.com"))
                .thenReturn(Optional.of(Tutor.builder().idTutor(3L).build()));
        when(pontosService.listarSaldos(7L, 3L)).thenReturn(List.of());

        mockMvc.perform(get("/pontos/saldos").param("idClinica", "7").param("idTutor", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}