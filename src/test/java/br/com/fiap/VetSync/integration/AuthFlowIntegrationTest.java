package br.com.fiap.VetSync.integration;

import br.com.fiap.VetSync.controller.AuthController;
import br.com.fiap.VetSync.controller.AdminController;
import br.com.fiap.VetSync.controller.VinculoClinicaController;
import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.repository.ClinicaRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClinicaRepository clinicaRepository;

    @Test
    @DisplayName("Fluxo completo de Auth: Registro -> Login -> Me -> Logout -> Me com token revogado (401)")
    void fluxoAutenticacaoCompleto() throws Exception {
        Clinica clinica = clinicaRepository.save(Clinica.builder()
                .nmClinica("Clínica Auth")
                .dsCnpj("33445566000177")
                .stContratante("A")
                .build());

        var adminReq = new AdminController.AdminBootstrapRequest("Admin Auth", "admin.auth@vetsync.com", "boot-secret-test-key-12345");
        MvcResult adminRes = mockMvc.perform(post("/admins/bootstrap").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String adminPwd = objectMapper.readTree(adminRes.getResponse().getContentAsString()).get("senhaTemporaria").asText();
        MvcResult adminLogin = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthController.LoginRequest("admin.auth@vetsync.com", adminPwd))))
                .andExpect(status().isOk())
                .andReturn();
        String adminToken = objectMapper.readTree(adminLogin.getResponse().getContentAsString()).get("token").asText();

        MvcResult codigoRes = mockMvc.perform(post("/vinculos-clinica/clinicas/" + clinica.getIdClinica() + "/codigo")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn();
        String codigo = objectMapper.readTree(codigoRes.getResponse().getContentAsString()).get("codigo").asText();
        MvcResult sessaoRes = mockMvc.perform(post("/vinculos-clinica/validar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VinculoClinicaController.CodigoRequest(codigo))))
                .andExpect(status().isOk())
                .andReturn();
        String sessaoVinculo = objectMapper.readTree(sessaoRes.getResponse().getContentAsString()).get("sessaoVinculo").asText();

        // 1. Registro de tutor com vínculo confirmado à clínica
        var registrarReq = new AuthController.RegistrarRequest(
                "Lucas Ferreira",
                "lucas.integ@teste.com",
                "senhaForte123",
                "12345678909",
                "11988887777",
                "01310100",
                "Avenida Paulista",
                "1000",
                null,
                "Bela Vista",
                "São Paulo",
                "SP",
                sessaoVinculo
        );

        mockMvc.perform(post("/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrarReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("lucas.integ@teste.com"))
                .andExpect(jsonPath("$.perfil").value("TUTOR"))
                .andReturn();

        // 2. Login com as credenciais cadastradas
        var loginReq = new AuthController.LoginRequest("lucas.integ@teste.com", "senhaForte123");

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode loginNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String jwtToken = loginNode.get("token").asText();
        assertThat(jwtToken).isNotBlank();

        // 3. Consulta de sessão /auth/me com o Bearer token válido
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("lucas.integ@teste.com"))
                .andExpect(jsonPath("$.nome").value("Lucas Ferreira"))
                .andExpect(jsonPath("$.perfil").value("TUTOR"));

        // 4. Logout com revogação de token
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        // 5. Tentativa de acessar /auth/me com o token que foi colocado na blacklist -> 401 Unauthorized
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isUnauthorized());
    }
}
