package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.controller.AuthController.AuthResponse;
import br.com.fiap.VetSync.service.SocialAuthService;
import br.com.fiap.VetSync.service.SocialAuthService.PendenciaSocial;
import br.com.fiap.VetSync.service.SocialAuthService.ResultadoLogin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrato HTTP dos endpoints sociais. Rotas públicas: nenhum header Authorization é enviado. */
@SpringBootTest
@AutoConfigureMockMvc
class SocialAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SocialAuthService socialAuthService;

    private static final String LOGIN_JSON = "{\"provider\":\"GOOGLE\",\"idToken\":\"tok\"}";

    private static AuthResponse sessao() {
        return new AuthResponse("jwt-interno", 7L, "maria@teste.com", "Maria", "TUTOR", true);
    }

    @Test
    @DisplayName("POST /auth/social-login - vinculado -> 200 com o mesmo contrato do /auth/login")
    void login_vinculado() throws Exception {
        when(socialAuthService.entrar("GOOGLE", "tok")).thenReturn(new ResultadoLogin(sessao(), null));

        mockMvc.perform(post("/auth/social-login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-interno"))
                .andExpect(jsonPath("$.idUsuario").value(7))
                .andExpect(jsonPath("$.perfil").value("TUTOR"))
                .andExpect(jsonPath("$.temVinculoAtivo").value(true));
    }

    @Test
    @DisplayName("POST /auth/social-login - sem vínculo -> 409 com estado claro e sem token")
    void login_pendente() throws Exception {
        var pendencia = new PendenciaSocial(SocialAuthService.CADASTRO_NECESSARIO, "GOOGLE",
                "novo@gmail.com", true, "Novo");
        when(socialAuthService.entrar("GOOGLE", "tok")).thenReturn(new ResultadoLogin(null, pendencia));

        mockMvc.perform(post("/auth/social-login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CADASTRO_NECESSARIO"))
                .andExpect(jsonPath("$.email").value("novo@gmail.com"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("POST /auth/social-login - provedor não suportado -> 400 no formato de erro padrão")
    void login_provedorNaoSuportado() throws Exception {
        when(socialAuthService.entrar(eq("FACEBOOK"), any())).thenThrow(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provedor não suportado. Use GOOGLE ou APPLE"));

        mockMvc.perform(post("/auth/social-login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"FACEBOOK\",\"idToken\":\"tok\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Provedor não suportado. Use GOOGLE ou APPLE"));
    }

    @Test
    @DisplayName("POST /auth/social-login - token inválido -> 401 sem vazar detalhes")
    void login_tokenInvalido() throws Exception {
        when(socialAuthService.entrar(any(), any())).thenThrow(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de identidade inválido ou expirado"));

        mockMvc.perform(post("/auth/social-login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Token de identidade inválido ou expirado"));
    }

    @Test
    @DisplayName("POST /auth/social-login - corpo sem idToken -> 400 de validação")
    void login_semToken() throws Exception {
        mockMvc.perform(post("/auth/social-login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"GOOGLE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/social-registrar - sucesso -> 201")
    void registrar_sucesso() throws Exception {
        when(socialAuthService.registrar(any())).thenReturn(sessao());

        mockMvc.perform(post("/auth/social-registrar").contentType(MediaType.APPLICATION_JSON).content("""
                        {"provider":"GOOGLE","idToken":"tok","cpf":"12345678901","cep":"01310100",
                         "logradouro":"Av. Paulista","numero":"1000","bairro":"Bela Vista",
                         "cidade":"São Paulo","uf":"SP","sessaoVinculo":"sessao"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-interno"));
    }

    @Test
    @DisplayName("POST /auth/social-registrar - sem CPF/sessão de vínculo -> 400")
    void registrar_dadosIncompletos() throws Exception {
        mockMvc.perform(post("/auth/social-registrar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"GOOGLE\",\"idToken\":\"tok\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/social-vincular - sucesso -> 200")
    void vincular_sucesso() throws Exception {
        when(socialAuthService.vincular(any())).thenReturn(sessao());

        mockMvc.perform(post("/auth/social-vincular").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"provider\":\"APPLE\",\"idToken\":\"tok\",\"email\":\"maria@teste.com\",\"senha\":\"senha123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("TUTOR"));
    }
}