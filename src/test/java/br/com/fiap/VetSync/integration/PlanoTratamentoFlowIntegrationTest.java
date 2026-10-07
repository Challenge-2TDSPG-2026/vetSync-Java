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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PlanoTratamentoFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClinicaRepository clinicaRepository;

    @Autowired
    private TipoEventoRepository tipoEventoRepository;

    @Test
    @DisplayName("Ponta a ponta: Criação do Plano -> Agendamento de Itens -> Conclusão Sequencial -> Conclusão do Plano e Bônus")
    void fluxoPlanoTratamento() throws Exception {
        Clinica clinica = clinicaRepository.save(Clinica.builder()
                .nmClinica("Clínica Integrada")
                .dsCnpj("55443322000188")
                .stContratante("A")
                .build());

        TipoEvento t1 = tipoEventoRepository.save(TipoEvento.builder().nmTipoEvento("Dose 1").dsCategoria("TERAPEUTICO").nrPontos(10).build());
        TipoEvento t2 = tipoEventoRepository.save(TipoEvento.builder().nmTipoEvento("Dose 2").dsCategoria("TERAPEUTICO").nrPontos(10).build());

        var adminReq = new AdminController.AdminBootstrapRequest("Admin P", "admin.p@vetsync.com", "boot-secret-test-key-12345");
        MvcResult adminRes = mockMvc.perform(post("/admins/bootstrap").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(adminReq))).andReturn();
        String adminPwd = objectMapper.readTree(adminRes.getResponse().getContentAsString()).get("senhaTemporaria").asText();

        MvcResult adminLogin = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthController.LoginRequest("admin.p@vetsync.com", adminPwd)))).andReturn();
        String adminToken = objectMapper.readTree(adminLogin.getResponse().getContentAsString()).get("token").asText();

        MvcResult codigoClinicaRes = mockMvc.perform(post("/vinculos-clinica/clinicas/" + clinica.getIdClinica() + "/codigo")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn();
        String codigoClinica = objectMapper.readTree(codigoClinicaRes.getResponse().getContentAsString()).get("codigo").asText();

        MvcResult sessaoVinculoRes = mockMvc.perform(post("/vinculos-clinica/validar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VinculoClinicaController.CodigoRequest(codigoClinica))))
                .andExpect(status().isOk())
                .andReturn();
        String sessaoVinculo = objectMapper.readTree(sessaoVinculoRes.getResponse().getContentAsString()).get("sessaoVinculo").asText();

        MvcResult vetRes = mockMvc.perform(post("/veterinarios")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VeterinarioController.VeterinarioRequest("Dr. Paulo", "paulo.vet@vetsync.com", clinica.getIdClinica())))).andReturn();
        JsonNode vetNode = objectMapper.readTree(vetRes.getResponse().getContentAsString());
        Long idVet = vetNode.get("idVeterinario").asLong();
        String vetPwd = vetNode.get("senhaTemporaria").asText();

        MvcResult vetLogin = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthController.LoginRequest("paulo.vet@vetsync.com", vetPwd)))).andReturn();
        String vetToken = objectMapper.readTree(vetLogin.getResponse().getContentAsString()).get("token").asText();

        MvcResult tutorRes = mockMvc.perform(post("/auth/registrar").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthController.RegistrarRequest("Tatiana", "tatiana@teste.com", "senha123", "99988811122", "11988882222", "01310100", "Avenida Paulista", "1000", null, "Bela Vista", "São Paulo", "SP", sessaoVinculo))))
                .andExpect(status().isCreated())
                .andReturn();
        String tutorToken = objectMapper.readTree(tutorRes.getResponse().getContentAsString()).get("token").asText();

        MvcResult petRes = mockMvc.perform(post("/pets")
                .header("Authorization", "Bearer " + tutorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PetController.PetRequest("Nala", EspecieCategoria.GATO, null, "Siamês", LocalDate.now().minusYears(1), new BigDecimal("3.0"), "F")))).andReturn();
        Long idPet = objectMapper.readTree(petRes.getResponse().getContentAsString()).get("idPet").asLong();

        var planoReq = new PlanoTratamentoController.PlanoTratamentoRequest(idPet, 50, List.of(t1.getIdTipoEvento(), t2.getIdTipoEvento()));
        MvcResult planoRes = mockMvc.perform(post("/planos")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planoReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andReturn();
        JsonNode planoNode = objectMapper.readTree(planoRes.getResponse().getContentAsString());
        Long idPlano = planoNode.get("idPlano").asLong();
        Long idItem1 = planoNode.get("itens").get(0).get("idItem").asLong();
        Long idItem2 = planoNode.get("itens").get(1).get("idItem").asLong();

        var agendarItem1 = new PlanoTratamentoController.PlanoItemAgendarRequest(idVet, LocalDate.now(), "09:00", "Dose 1");
        MvcResult item1Res = mockMvc.perform(patch("/planos/itens/" + idItem1 + "/agendar")
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agendarItem1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGENDADO"))
                .andReturn();
        Long idEvento1 = objectMapper.readTree(item1Res.getResponse().getContentAsString()).get("idEvento").asLong();

        mockMvc.perform(patch("/eventos/" + idEvento1 + "/concluir")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EventoController.EventoConcluirRequest("Dose 1 aplicada", BigDecimal.ZERO))))
                .andExpect(status().isOk());

        var agendarItem2 = new PlanoTratamentoController.PlanoItemAgendarRequest(idVet, LocalDate.now().plusWeeks(2), "10:30", "Dose 2");
        MvcResult item2Res = mockMvc.perform(patch("/planos/itens/" + idItem2 + "/agendar")
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agendarItem2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGENDADO"))
                .andReturn();
        Long idEvento2 = objectMapper.readTree(item2Res.getResponse().getContentAsString()).get("idEvento").asLong();

        mockMvc.perform(patch("/eventos/" + idEvento2 + "/concluir")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EventoController.EventoConcluirRequest("Dose 2 aplicada", BigDecimal.ZERO))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/planos/" + idPlano)
                        .header("Authorization", "Bearer " + tutorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"));
    }
}
