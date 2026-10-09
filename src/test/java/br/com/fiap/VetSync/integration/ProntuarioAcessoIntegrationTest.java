package br.com.fiap.VetSync.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe o contexto completo (H2, schema gerado a partir das entidades) para validar o mapeamento JPA das
 * tabelas novas, o wiring dos beans de segurança usados nos @PreAuthorize e as regras de acesso básicas.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProntuarioAcessoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Rotas autenticadas do prontuário retornam 401 sem token")
    void anonimoRecebe401() throws Exception {
        mockMvc.perform(get("/pets/1/prontuario")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/pets/1/prontuario/exportar")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/pets/1/exames")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/eventos/1/orientacoes")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/pets/1/prontuario/compartilhamentos")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/pets/1/prontuario/compartilhamentos/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Link público inválido responde 404 genérico, sem exigir autenticação e sem cache")
    void linkPublicoInvalido() throws Exception {
        mockMvc.perform(get("/prontuarios-publicos/token-que-nao-existe").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow"));

        mockMvc.perform(get("/prontuarios-publicos/token-que-nao-existe/dados").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/prontuarios-publicos/token-que-nao-existe/exames/1/arquivo"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tutor sem vínculo com o pet não acessa o prontuário nem compartilha")
    @WithMockUser(username = "estranho@x.com", roles = "TUTOR")
    void tutorSemVinculoRecebe403() throws Exception {
        mockMvc.perform(get("/pets/999999/prontuario")).andExpect(status().isForbidden());
        mockMvc.perform(get("/pets/999999/prontuario/exportar")).andExpect(status().isForbidden());
        mockMvc.perform(get("/pets/999999/exames")).andExpect(status().isForbidden());
        mockMvc.perform(post("/pets/999999/prontuario/compartilhamentos")).andExpect(status().isForbidden());
        mockMvc.perform(get("/pets/999999/prontuario/compartilhamentos")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Tutor não registra orientações nem exames (somente veterinário)")
    @WithMockUser(username = "tutor@x.com", roles = "TUTOR")
    void tutorNaoRegistra() throws Exception {
        mockMvc.perform(post("/eventos/1/orientacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"t\",\"texto\":\"x\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/pets/1/exames").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Hemograma\",\"resultadoEm\":\"2026-01-01\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Veterinário sem vínculo com o pet recebe 403 ao ler ou registrar exames")
    @WithMockUser(username = "vet@x.com", roles = "VETERINARIO")
    void veterinarioSemVinculoRecebe403() throws Exception {
        mockMvc.perform(get("/pets/999999/prontuario")).andExpect(status().isForbidden());
        mockMvc.perform(post("/pets/999999/exames").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Hemograma\",\"resultadoEm\":\"2026-01-01\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/pets/999999/prontuario/compartilhamentos")).andExpect(status().isForbidden());
    }
}