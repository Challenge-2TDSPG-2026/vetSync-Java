package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.controller.AuthController.AuthResponse;
import br.com.fiap.VetSync.controller.SocialAuthController.SocialRegistrarRequest;
import br.com.fiap.VetSync.controller.SocialAuthController.SocialVincularRequest;
import br.com.fiap.VetSync.entity.IdentidadeSocial;
import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.repository.IdentidadeSocialRepository;
import br.com.fiap.VetSync.repository.ProfissionalEsteticaRepository;
import br.com.fiap.VetSync.repository.TutorRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import br.com.fiap.VetSync.social.SocialIdentity;
import br.com.fiap.VetSync.social.SocialProvider;
import br.com.fiap.VetSync.social.SocialTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SocialAuthServiceTest {

    private static final String SUB = "google-sub-123";

    @Mock SocialTokenVerifier verifier;
    @Mock IdentidadeSocialRepository identidadeRepository;
    @Mock TutorRepository tutorRepository;
    @Mock VeterinarioRepository veterinarioRepository;
    @Mock ProfissionalEsteticaRepository profissionalEsteticaRepository;
    @Mock AdminRepository adminRepository;
    @Mock VinculoClinicaService vinculoClinicaService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authManager;

    private JwtService jwtService;
    private SocialAuthService service;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "9a7b6c5d4e3f2a1b0c9d8e7f6a5b4c3d2e1f0a9b8c7d6e5f4a3b2c1d0e9f8a7b");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
        service = new SocialAuthService(verifier, identidadeRepository, tutorRepository, veterinarioRepository,
                profissionalEsteticaRepository, adminRepository, vinculoClinicaService, jwtService,
                passwordEncoder, authManager);
        when(passwordEncoder.encode(anyString())).thenReturn("hash-bcrypt");
        when(vinculoClinicaService.temVinculoAtivo(anyLong())).thenReturn(true);
    }

    // ---------- helpers ----------

    private static Tutor tutor() {
        return Tutor.builder().idTutor(7L).nmTutor("Maria Silva").dsEmail("maria@teste.com")
                .dsCpf("12345678901").dsSenha("hash").build();
    }

    private static SocialIdentity google(String email, boolean verificado, String nome) {
        return new SocialIdentity(SocialProvider.GOOGLE, SUB, email, verificado, nome);
    }

    private void tokenValido(SocialIdentity identidade) {
        when(verifier.verificar(eq(identidade.provider()), anyString())).thenReturn(identidade);
    }

    private static SocialRegistrarRequest registro(String provider, String nome) {
        return new SocialRegistrarRequest(provider, "tok", nome, "12345678901", "11999990000", "01310100",
                "Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "SP", "sessao-valida");
    }

    private static void assertStatus(HttpStatus esperado, ResponseStatusException ex) {
        assertEquals(esperado.value(), ex.getStatusCode().value());
    }

    // ---------- /social-login ----------

    @Test
    @DisplayName("Identidade já vinculada: devolve o contrato do login e um JWT interno válido do tutor")
    void entrar_identidadeVinculada() {
        tokenValido(google("qualquer@gmail.com", true, "Nome Google"));
        var vinculo = IdentidadeSocial.builder().tutor(tutor()).provedor(SocialProvider.GOOGLE).subject(SUB).build();
        when(identidadeRepository.findByProvedorAndSubject(SocialProvider.GOOGLE, SUB)).thenReturn(Optional.of(vinculo));

        var resultado = service.entrar("google", "tok");

        assertNull(resultado.pendencia());
        AuthResponse r = resultado.sessao();
        assertEquals(7L, r.idUsuario());
        assertEquals("maria@teste.com", r.email());       // e-mail da conta VetSync, não o do provedor
        assertEquals("Maria Silva", r.nome());
        assertEquals("TUTOR", r.perfil());                // perfil preservado
        assertTrue(r.temVinculoAtivo());
        assertTrue(r.permissoes().isEmpty());
        assertFalse(r.trocaSenhaObrigatoria());
        assertTrue(jwtService.tokenValido(r.token()));    // JWT interno emitido e verificável
        assertEquals("maria@teste.com", jwtService.extrairEmail(r.token()));
    }

    @Test
    @DisplayName("Usuário inexistente: CADASTRO_NECESSARIO, nada é criado")
    void entrar_usuarioInexistente() {
        tokenValido(google("novo@gmail.com", true, "Novo"));
        when(identidadeRepository.findByProvedorAndSubject(any(), anyString())).thenReturn(Optional.empty());

        var resultado = service.entrar("GOOGLE", "tok");

        assertNull(resultado.sessao());
        assertEquals(SocialAuthService.CADASTRO_NECESSARIO, resultado.pendencia().status());
        assertEquals("novo@gmail.com", resultado.pendencia().email());
        verify(tutorRepository, never()).save(any());
        verify(identidadeRepository, never()).save(any());
        verify(identidadeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("E-mail coincide com conta existente, sem vínculo comprovado: VINCULO_NECESSARIO, sem JWT e sem vincular")
    void entrar_emailCoincidenteSemVinculo() {
        tokenValido(google("maria@teste.com", true, "Maria"));
        when(identidadeRepository.findByProvedorAndSubject(any(), anyString())).thenReturn(Optional.empty());
        when(tutorRepository.existsByDsEmail("maria@teste.com")).thenReturn(true);

        var resultado = service.entrar("GOOGLE", "tok");

        assertNull(resultado.sessao());
        assertEquals(SocialAuthService.VINCULO_NECESSARIO, resultado.pendencia().status());
        verify(identidadeRepository, never()).save(any());
        verify(identidadeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("E-mail NÃO verificado pelo provedor não revela a existência de conta")
    void entrar_emailNaoVerificado() {
        tokenValido(google("maria@teste.com", false, null));
        when(identidadeRepository.findByProvedorAndSubject(any(), anyString())).thenReturn(Optional.empty());
        when(tutorRepository.existsByDsEmail("maria@teste.com")).thenReturn(true);

        var resultado = service.entrar("GOOGLE", "tok");

        assertEquals(SocialAuthService.CADASTRO_NECESSARIO, resultado.pendencia().status());
    }

    @Test
    @DisplayName("E-mail pertence a veterinário/estética/admin: EMAIL_EM_USO (não vinculável)")
    void entrar_emailDeOutroPerfil() {
        tokenValido(google("vet@clinica.com", true, "Vet"));
        when(identidadeRepository.findByProvedorAndSubject(any(), anyString())).thenReturn(Optional.empty());
        when(veterinarioRepository.existsByDsEmail("vet@clinica.com")).thenReturn(true);

        assertEquals(SocialAuthService.EMAIL_EM_USO, service.entrar("GOOGLE", "tok").pendencia().status());
    }

    @Test
    @DisplayName("Provedor não suportado -> 400, sem validar token")
    void entrar_provedorNaoSuportado() {
        var ex = assertThrows(ResponseStatusException.class, () -> service.entrar("FACEBOOK", "tok"));
        assertStatus(HttpStatus.BAD_REQUEST, ex);
        verifyNoInteractions(verifier);
    }

    @Test
    @DisplayName("Token inválido: o erro 401 do validador é propagado e nada é consultado no banco")
    void entrar_tokenInvalido() {
        when(verifier.verificar(any(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de identidade inválido ou expirado"));

        var ex = assertThrows(ResponseStatusException.class, () -> service.entrar("GOOGLE", "ruim"));
        assertStatus(HttpStatus.UNAUTHORIZED, ex);
        verifyNoInteractions(identidadeRepository);
    }

    // ---------- /social-registrar ----------

    @Test
    @DisplayName("Registrar: e-mail e subject vêm do token validado; cria tutor + identidade e devolve JWT")
    void registrar_sucesso() {
        tokenValido(google("novo@gmail.com", true, "Novo Tutor"));
        when(identidadeRepository.findByProvedorAndSubject(any(), anyString())).thenReturn(Optional.empty());
        when(vinculoClinicaService.criarTutorComVinculo(any(Tutor.class), eq("sessao-valida"))).thenAnswer(inv -> {
            Tutor t = inv.getArgument(0);
            t.setIdTutor(99L);
            return t;
        });

        AuthResponse r = service.registrar(registro("GOOGLE", "Nome Do App"));

        ArgumentCaptor<Tutor> tutorCap = ArgumentCaptor.forClass(Tutor.class);
        verify(vinculoClinicaService).criarTutorComVinculo(tutorCap.capture(), eq("sessao-valida"));
        assertEquals("novo@gmail.com", tutorCap.getValue().getDsEmail());
        assertEquals("Novo Tutor", tutorCap.getValue().getNmTutor());   // nome do token prevalece
        assertEquals("hash-bcrypt", tutorCap.getValue().getDsSenha());  // senha aleatória, nunca vazia
        ArgumentCaptor<IdentidadeSocial> idCap = ArgumentCaptor.forClass(IdentidadeSocial.class);
        verify(identidadeRepository).saveAndFlush(idCap.capture());
        assertEquals(SocialProvider.GOOGLE, idCap.getValue().getProvedor());
        assertEquals(SUB, idCap.getValue().getSubject());
        assertEquals(99L, r.idUsuario());
        assertEquals("TUTOR", r.perfil());
        assertEquals("novo@gmail.com", jwtService.extrairEmail(r.token()));
    }

    @Test
    @DisplayName("Registrar com Apple sem nome no token: usa o nome informado pelo app; sem nome -> 400")
    void registrar_appleNome() {
        var apple = new SocialIdentity(SocialProvider.APPLE, "apple-sub", "x@privaterelay.appleid.com", true, null);
        tokenValido(apple);
        when(identidadeRepository.findByProvedorAndSubject(any(), anyString())).thenReturn(Optional.empty());
        when(vinculoClinicaService.criarTutorComVinculo(any(Tutor.class), anyString()))
                .thenAnswer(inv -> { Tutor t = inv.getArgument(0); t.setIdTutor(5L); return t; });

        service.registrar(registro("APPLE", "João Souza"));
        ArgumentCaptor<Tutor> cap = ArgumentCaptor.forClass(Tutor.class);
        verify(vinculoClinicaService).criarTutorComVinculo(cap.capture(), anyString());
        assertEquals("João Souza", cap.getValue().getNmTutor());
        assertEquals("x@privaterelay.appleid.com", cap.getValue().getDsEmail());

        var ex = assertThrows(ResponseStatusException.class, () -> service.registrar(registro("APPLE", null)));
        assertStatus(HttpStatus.BAD_REQUEST, ex);
    }

    @Test
    @DisplayName("Registrar com identidade já vinculada -> 409 (sem identidade duplicada)")
    void registrar_identidadeJaVinculada() {
        tokenValido(google("novo@gmail.com", true, "Novo"));
        when(identidadeRepository.findByProvedorAndSubject(any(), anyString()))
                .thenReturn(Optional.of(IdentidadeSocial.builder().tutor(tutor()).build()));

        var ex = assertThrows(ResponseStatusException.class, () -> service.registrar(registro("GOOGLE", null)));
        assertStatus(HttpStatus.CONFLICT, ex);
        verify(vinculoClinicaService, never()).criarTutorComVinculo(any(), anyString());
    }

    @Test
    @DisplayName("Registrar com e-mail não verificado ou ausente -> 422")
    void registrar_emailNaoVerificado() {
        tokenValido(google("novo@gmail.com", false, "Novo"));
        when(identidadeRepository.findByProvedorAndSubject(any(), anyString())).thenReturn(Optional.empty());
        var ex = assertThrows(ResponseStatusException.class, () -> service.registrar(registro("GOOGLE", null)));
        assertStatus(HttpStatus.UNPROCESSABLE_ENTITY, ex);

        tokenValido(google(null, true, "Novo"));
        ex = assertThrows(ResponseStatusException.class, () -> service.registrar(registro("GOOGLE", null)));
        assertStatus(HttpStatus.UNPROCESSABLE_ENTITY, ex);
        verify(vinculoClinicaService, never()).criarTutorComVinculo(any(), anyString());
    }

    @Test
    @DisplayName("Registrar com e-mail já cadastrado -> 409 (não vincula por coincidência de e-mail)")
    void registrar_emailJaCadastrado() {
        tokenValido(google("maria@teste.com", true, "Maria"));
        when(identidadeRepository.findByProvedorAndSubject(any(), anyString())).thenReturn(Optional.empty());
        when(tutorRepository.existsByDsEmail("maria@teste.com")).thenReturn(true);

        var ex = assertThrows(ResponseStatusException.class, () -> service.registrar(registro("GOOGLE", null)));
        assertStatus(HttpStatus.CONFLICT, ex);
        verify(vinculoClinicaService, never()).criarTutorComVinculo(any(), anyString());
        verify(identidadeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Violação de unicidade no banco (CPF/identidade duplicados) -> 409")
    void registrar_duplicidadeNoBanco() {
        tokenValido(google("novo@gmail.com", true, "Novo"));
        when(identidadeRepository.findByProvedorAndSubject(any(), anyString())).thenReturn(Optional.empty());
        when(vinculoClinicaService.criarTutorComVinculo(any(Tutor.class), anyString()))
                .thenAnswer(inv -> { Tutor t = inv.getArgument(0); t.setIdTutor(1L); return t; });
        when(identidadeRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk"));

        var ex = assertThrows(ResponseStatusException.class, () -> service.registrar(registro("GOOGLE", null)));
        assertStatus(HttpStatus.CONFLICT, ex);
    }

    // ---------- /social-vincular ----------

    @Test
    @DisplayName("Vincular com e-mail e senha corretos: salva a identidade e devolve JWT do tutor")
    void vincular_sucesso() {
        tokenValido(google("outro@gmail.com", true, "Maria"));
        when(identidadeRepository.findByProvedorAndSubject(any(), anyString())).thenReturn(Optional.empty());
        when(authManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("maria@teste.com", "senha123"));
        when(tutorRepository.findByDsEmail("maria@teste.com")).thenReturn(Optional.of(tutor()));
        when(identidadeRepository.existsByTutor_IdTutorAndProvedor(7L, SocialProvider.GOOGLE)).thenReturn(false);

        AuthResponse r = service.vincular(new SocialVincularRequest("GOOGLE", "tok", " Maria@Teste.com ", "senha123"));

        ArgumentCaptor<IdentidadeSocial> cap = ArgumentCaptor.forClass(IdentidadeSocial.class);
        verify(identidadeRepository).saveAndFlush(cap.capture());
        assertEquals(7L, cap.getValue().getTutor().getIdTutor());
        assertEquals(SUB, cap.getValue().getSubject());
        assertEquals("TUTOR", r.perfil());
        assertEquals("maria@teste.com", jwtService.extrairEmail(r.token()));
    }

    @Test
    @DisplayName("Vincular com senha incorreta -> 401 e nada é salvo")
    void vincular_senhaIncorreta() {
        tokenValido(google("maria@teste.com", true, "Maria"));
        when(identidadeRepository.findByProvedorAndSubject(any(), anyString())).thenReturn(Optional.empty());
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.vincular(new SocialVincularRequest("GOOGLE", "tok", "maria@teste.com", "errada")));
        assertStatus(HttpStatus.UNAUTHORIZED, ex);
        verify(identidadeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Vincular identidade que já pertence a alguém -> 409, sem sequer testar a senha")
    void vincular_identidadeJaVinculada() {
        tokenValido(google("maria@teste.com", true, "Maria"));
        when(identidadeRepository.findByProvedorAndSubject(any(), anyString()))
                .thenReturn(Optional.of(IdentidadeSocial.builder().tutor(tutor()).build()));

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.vincular(new SocialVincularRequest("GOOGLE", "tok", "maria@teste.com", "senha123")));
        assertStatus(HttpStatus.CONFLICT, ex);
        verifyNoInteractions(authManager);
    }

    @Test
    @DisplayName("Tutor que já tem um vínculo com o mesmo provedor -> 409 (sem duplicar)")
    void vincular_tutorJaTemProvedor() {
        tokenValido(google("maria@teste.com", true, "Maria"));
        when(identidadeRepository.findByProvedorAndSubject(any(), anyString())).thenReturn(Optional.empty());
        when(authManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("maria@teste.com", "senha123"));
        when(tutorRepository.findByDsEmail("maria@teste.com")).thenReturn(Optional.of(tutor()));
        when(identidadeRepository.existsByTutor_IdTutorAndProvedor(7L, SocialProvider.GOOGLE)).thenReturn(true);

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.vincular(new SocialVincularRequest("GOOGLE", "tok", "maria@teste.com", "senha123")));
        assertStatus(HttpStatus.CONFLICT, ex);
        verify(identidadeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Conta autenticada que não é de tutor (vet/admin) -> 403")
    void vincular_contaNaoTutor() {
        tokenValido(google("vet@clinica.com", true, "Vet"));
        when(identidadeRepository.findByProvedorAndSubject(any(), anyString())).thenReturn(Optional.empty());
        when(authManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("vet@clinica.com", "senha123"));
        when(tutorRepository.findByDsEmail("vet@clinica.com")).thenReturn(Optional.empty());

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.vincular(new SocialVincularRequest("GOOGLE", "tok", "vet@clinica.com", "senha123")));
        assertStatus(HttpStatus.FORBIDDEN, ex);
        verify(identidadeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Corrida na unicidade do banco ao vincular -> 409")
    void vincular_duplicidadeNoBanco() {
        tokenValido(google("maria@teste.com", true, "Maria"));
        when(identidadeRepository.findByProvedorAndSubject(any(), anyString())).thenReturn(Optional.empty());
        when(authManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("maria@teste.com", "senha123"));
        when(tutorRepository.findByDsEmail("maria@teste.com")).thenReturn(Optional.of(tutor()));
        when(identidadeRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk"));

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.vincular(new SocialVincularRequest("GOOGLE", "tok", "maria@teste.com", "senha123")));
        assertStatus(HttpStatus.CONFLICT, ex);
    }
}