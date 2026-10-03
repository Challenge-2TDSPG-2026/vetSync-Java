package br.com.fiap.VetSync.integration;

import br.com.fiap.VetSync.controller.*;
import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class EventoPontosRecompensaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClinicaRepository clinicaRepository;

    @Autowired
    private TipoEventoRepository tipoEventoRepository;

    @Test
    @DisplayName("Ponta a ponta: Cadastro -> Pet -> Agendamento -> Conclusão -> Liberação de Pontos -> Saldo -> Resgate -> Validação")
    void fluxoCompletoEventoPontosRecompensa() throws Exception {
        Clinica clinica = clinicaRepository.save(Clinica.builder()
                .nmClinica("Clínica VetSync SP")
                .dsCnpj("11222333000199")
                .dsCidade("São Paulo")
                .dsUf("SP")
                .stContratante("A")
                .build());

        TipoEvento tipoVacina = tipoEventoRepository.save(TipoEvento.builder()
                .nmTipoEvento("Vacina Décupla")
                .dsCategoria("PREVENTIVO")
                .nrPontos(30)
                .build());

        var adminReq = new AdminController.AdminBootstrapRequest("Admin Boss", "boss@vetsync.com", "boot-secret-test-key-12345");
        MvcResult adminRes = mockMvc.perform(post("/admins/bootstrap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String adminPwd = objectMapper.readTree(adminRes.getResponse().getContentAsString()).get("senhaTemporaria").asText();

        MvcResult adminLoginRes = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthController.LoginRequest("boss@vetsync.com", adminPwd))))
                .andExpect(status().isOk())
                .andReturn();
        String adminToken = objectMapper.readTree(adminLoginRes.getResponse().getContentAsString()).get("token").asText();

        var vetReq = new VeterinarioController.VeterinarioRequest("Dr. Gabriel", "gabriel.vet@vetsync.com", clinica.getIdClinica());
        MvcResult vetRes = mockMvc.perform(post("/veterinarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vetReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode vetNode = objectMapper.readTree(vetRes.getResponse().getContentAsString());
        Long idVet = vetNode.get("idVeterinario").asLong();
        String vetPwd = vetNode.get("senhaTemporaria").asText();

        MvcResult vetLoginRes = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthController.LoginRequest("gabriel.vet@vetsync.com", vetPwd))))
                .andExpect(status().isOk())
                .andReturn();
        String vetToken = objectMapper.readTree(vetLoginRes.getResponse().getContentAsString()).get("token").asText();

        var recompensaReq = new RecompensaController.RecompensaRequest(
                "Guia Passeio",
                "Guia de alta durabilidade",
                30,
                TipoRecompensa.PRODUTO
        );
        MvcResult recRes = mockMvc.perform(post("/recompensas")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recompensaReq)))
                .andExpect(status().isCreated())
                .andReturn();
        Long idRecompensa = objectMapper.readTree(recRes.getResponse().getContentAsString()).get("idRecompensa").asLong();

        var tutorReq = new AuthController.RegistrarRequest(
                "Camila Silva",
                "camila@teste.com",
                "senha123456",
                "98765432100",
                "11977776666",
                "01310100",
                "Avenida Paulista",
                "1000",
                null,
                "Bela Vista",
                "São Paulo",
                "SP"
        );
        MvcResult tutorRes = mockMvc.perform(post("/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tutorReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String tutorToken = objectMapper.readTree(tutorRes.getResponse().getContentAsString()).get("token").asText();

        var petReq = new PetController.PetRequest(
                "Pipoca",
                EspecieCategoria.CAO,
                null,
                "Golden Retriever",
                LocalDate.now().minusYears(2),
                new BigDecimal("22.5"),
                "M"
        );
        MvcResult petRes = mockMvc.perform(post("/pets")
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(petReq)))
                .andExpect(status().isCreated())
                .andReturn();
        Long idPet = objectMapper.readTree(petRes.getResponse().getContentAsString()).get("idPet").asLong();

        var agendarReq = new EventoController.EventoAgendarRequest(
                idPet,
                tipoVacina.getIdTipoEvento(),
                idVet,
                LocalDate.now(),
                "14:30",
                "Vacinação do Pipoca"
        );
        MvcResult eventoRes = mockMvc.perform(post("/eventos")
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agendarReq)))
                .andExpect(status().isCreated())
                .andReturn();
        Long idEvento = objectMapper.readTree(eventoRes.getResponse().getContentAsString()).get("idEvento").asLong();

        var concluirReq = new EventoController.EventoConcluirRequest("Vacina aplicada sem reações", new BigDecimal("120.00"));
        mockMvc.perform(patch("/eventos/" + idEvento + "/concluir")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(concluirReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"));

        MvcResult pontosListRes = mockMvc.perform(get("/pontos")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode pontosArray = objectMapper.readTree(pontosListRes.getResponse().getContentAsString());
        assertThat(pontosArray.size()).isGreaterThanOrEqualTo(1);
        Long idLancamento = pontosArray.get(0).get("idLancamento").asLong();

        mockMvc.perform(patch("/pontos/" + idLancamento + "/liberar")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIBERADO"));

        mockMvc.perform(get("/recompensas/saldo")
                        .header("Authorization", "Bearer " + tutorToken))
                .andExpect(status().isOk())
                .andExpect(content().string("30"));

        MvcResult resgateRes = mockMvc.perform(patch("/recompensas/" + idRecompensa + "/resgatar")
                        .header("Authorization", "Bearer " + tutorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andReturn();
        Long idResgate = objectMapper.readTree(resgateRes.getResponse().getContentAsString()).get("idResgate").asLong();

        var validarReq = new RecompensaController.ValidarResgateRequest(true);
        mockMvc.perform(patch("/recompensas/resgates/" + idResgate + "/validar")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validarReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDADO"));

        mockMvc.perform(get("/recompensas/saldo")
                        .header("Authorization", "Bearer " + tutorToken))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }
}