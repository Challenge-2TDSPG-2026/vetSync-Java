package br.com.fiap.VetSync.integration;

import br.com.fiap.VetSync.controller.AuthController;
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

    @Test
    @DisplayName("Fluxo completo de Auth: Registro -> Login -> Me -> Logout -> Me com token revogado (401)")
    void fluxoAutenticacaoCompleto() throws Exception {
        // 1. Registro de novo tutor
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
                "SP"
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
