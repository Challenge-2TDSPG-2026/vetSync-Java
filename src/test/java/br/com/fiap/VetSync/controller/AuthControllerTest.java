package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.repository.TutorRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import br.com.fiap.VetSync.security.TokenBlacklist;
import br.com.fiap.VetSync.service.JwtService;
import br.com.fiap.VetSync.service.VinculoClinicaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authManager;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TutorRepository tutorRepository;

    @MockBean
    private VeterinarioRepository veterinarioRepository;

    @MockBean
    private AdminRepository adminRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private TokenBlacklist tokenBlacklist;

    @MockBean
    private VinculoClinicaService vinculoClinicaService;

    @Test
    @DisplayName("POST /auth/login - Sucesso")
    void login_Sucesso() throws Exception {
        var req = new AuthController.LoginRequest("tutor@teste.com", "senha123");
        Tutor tutor = Tutor.builder().idTutor(1L).nmTutor("Tutor Teste").dsEmail("tutor@teste.com").build();

        when(authManager.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken("tutor@teste.com", "senha123"));
        when(jwtService.gerarToken("tutor@teste.com")).thenReturn("fake-jwt-token");
        when(tutorRepository.findByDsEmail("tutor@teste.com")).thenReturn(Optional.of(tutor));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"))
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.perfil").value("TUTOR"));
    }

    @Test
    @DisplayName("POST /auth/login - Credenciais Inválidas -> 401 Unauthorized")
    void login_CredenciaisInvalidas() throws Exception {
        var req = new AuthController.LoginRequest("tutor@teste.com", "senha-errada");
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
    }

    @Test
    @DisplayName("POST /auth/registrar - Sucesso")
    void registrar_Sucesso() throws Exception {
        var req = new AuthController.RegistrarRequest("Novo Tutor", "novo@teste.com", "senha123", "12345678901", "11999990000", "01310100", "Avenida Paulista", "1000", "Apto 10", "Bela Vista", "São Paulo", "SP", "sessao-valida");

        when(tutorRepository.existsByDsEmail("novo@teste.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hashed-pwd");
        when(vinculoClinicaService.criarTutorComVinculo(any(Tutor.class), eq("sessao-valida"))).thenAnswer(inv -> {
            Tutor t = inv.getArgument(0);
            t.setIdTutor(10L);
            return t;
        });
        when(jwtService.gerarToken("novo@teste.com")).thenReturn("novo-jwt-token");

        mockMvc.perform(post("/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("novo-jwt-token"))
                .andExpect(jsonPath("$.idUsuario").value(10))
                .andExpect(jsonPath("$.perfil").value("TUTOR"));
    }

    @Test
    @DisplayName("POST /auth/registrar - Falha validação (campos vazios) -> 400 Bad Request")
    void registrar_CamposInvalidos() throws Exception {
        var req = new AuthController.RegistrarRequest("", "email-invalido", "123", "cpf-invalido", "123", "12", "", "", "", "", "", "");

        mockMvc.perform(post("/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.campos").isMap());
    }

    @Test
    @DisplayName("POST /auth/registrar - E-mail Duplicado -> 409 Conflict")
    void registrar_EmailDuplicado() throws Exception {
        var req = new AuthController.RegistrarRequest("Novo", "existe@teste.com", "senha123", "12345678901", "11999990000", "01310100", "Avenida Paulista", "1000", null, "Bela Vista", "São Paulo", "SP", "sessao-valida");
        when(tutorRepository.existsByDsEmail("existe@teste.com")).thenReturn(true);

        mockMvc.perform(post("/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado"));
    }

    @Test
    @DisplayName("POST /auth/logout - Revoga token e retorna 204")
    @WithMockUser(username = "tutor@teste.com")
    void logout_Sucesso() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer meu-token-jwt"))
                .andExpect(status().isNoContent());

        verify(tokenBlacklist).revogar("meu-token-jwt");
    }

    @Test
    @DisplayName("GET /auth/me - Retorna perfil do usuário autenticado")
    @WithMockUser(username = "tutor@teste.com", roles = "TUTOR")
    void me_Sucesso() throws Exception {
        Tutor tutor = Tutor.builder().idTutor(1L).nmTutor("Tutor Me").dsEmail("tutor@teste.com").build();
        when(tutorRepository.findByDsEmail("tutor@teste.com")).thenReturn(Optional.of(tutor));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.email").value("tutor@teste.com"))
                .andExpect(jsonPath("$.perfil").value("TUTOR"));
    }
}
