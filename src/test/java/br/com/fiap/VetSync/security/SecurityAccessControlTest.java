package br.com.fiap.VetSync.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Endpoints públicos e documentação Swagger devem ser acessíveis sem token")
    void endpointsPublicos() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Tentativa de acesso anônimo a endpoint protegido deve retornar 401 Unauthorized")
    void endpointsProtegidosAnonimo() throws Exception {
        mockMvc.perform(get("/pets"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/eventos"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/pontos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TUTOR tentando acessar endpoints exclusivos de ADMIN deve receber 403 Forbidden")
    @WithMockUser(roles = "TUTOR")
    void tutorAcessandoAdmin() throws Exception {
        mockMvc.perform(post("/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Hack\",\"email\":\"hack@adm.com\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/veterinarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Vet\",\"email\":\"v@v.com\",\"idClinica\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("VETERINARIO tentando acessar endpoints exclusivos de ADMIN deve receber 403 Forbidden")
    @WithMockUser(roles = "VETERINARIO")
    void vetAcessandoAdmin() throws Exception {
        mockMvc.perform(post("/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Hack\",\"email\":\"hack@adm.com\"}"))
                .andExpect(status().isForbidden());
    }

    private static final String CORPO_EDICAO_EQUIPE = "{\"nome\":\"Nome Editado\",\"idClinica\":1}";

    @Test
    @DisplayName("ADMIN deve ter acesso à edição de veterinário e profissional de estética (id inexistente retorna 404, não 403)")
    @WithMockUser(roles = "ADMIN")
    void adminEditandoEquipe() throws Exception {
        mockMvc.perform(put("/veterinarios/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_EDICAO_EQUIPE))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/profissionais-estetica/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_EDICAO_EQUIPE))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TUTOR tentando editar veterinário ou profissional de estética deve receber 403 Forbidden")
    @WithMockUser(roles = "TUTOR")
    void tutorEditandoEquipe() throws Exception {
        mockMvc.perform(put("/veterinarios/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_EDICAO_EQUIPE))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/profissionais-estetica/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_EDICAO_EQUIPE))
                .andExpect(status().isForbidden());
    }
}
