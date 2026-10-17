package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Admin;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.repository.ProfissionalEsteticaRepository;
import br.com.fiap.VetSync.repository.TutorRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private VeterinarioRepository veterinarioRepository;

    @Mock
    private ProfissionalEsteticaRepository esteticaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AdminService adminService;

    private static final String CHAVE_TESTE = "chave-secreta-bootstrap-123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminService, "bootstrapKey", CHAVE_TESTE);
    }

    @Test
    @DisplayName("Deve realizar bootstrap com sucesso quando não há admins e a chave está correta")
    void bootstrap_Sucesso() {
        when(adminRepository.count()).thenReturn(0L);
        when(adminRepository.findByDsEmail("admin@vetsync.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pwd");
        when(adminRepository.save(any(Admin.class))).thenAnswer(inv -> {
            Admin a = inv.getArgument(0);
            a.setIdAdmin(1L);
            return a;
        });

        AdminService.NovoAdmin resultado = adminService.bootstrap("Admin Inicial", "admin@vetsync.com", CHAVE_TESTE);

        assertThat(resultado).isNotNull();
        assertThat(resultado.admin().getIdAdmin()).isEqualTo(1L);
        assertThat(resultado.admin().getNmAdmin()).isEqualTo("Admin Inicial");
        assertThat(resultado.senhaTemporaria()).isNotBlank().hasSize(10);
        verify(emailService).enviar(eq("admin@vetsync.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve falhar bootstrap com 409 quando já existe admin cadastrado")
    void bootstrap_ConflitoQuandoJaExisteAdmin() {
        when(adminRepository.count()).thenReturn(1L);

        assertThatThrownBy(() -> adminService.bootstrap("Admin", "admin@vetsync.com", CHAVE_TESTE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Já existe pelo menos um admin");

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar bootstrap com 403 quando a chave fornecida é inválida")
    void bootstrap_ChaveInvalida() {
        when(adminRepository.count()).thenReturn(0L);

        assertThatThrownBy(() -> adminService.bootstrap("Admin", "admin@vetsync.com", "chave-errada"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Chave de bootstrap inválida");

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve cadastrar novo admin com sucesso")
    void cadastrar_Sucesso() {
        when(adminRepository.findByDsEmail("novo@vetsync.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash-123");
        when(adminRepository.save(any(Admin.class))).thenAnswer(inv -> {
            Admin a = inv.getArgument(0);
            a.setIdAdmin(2L);
            return a;
        });

        AdminService.NovoAdmin resultado = adminService.cadastrar("Segundo Admin", "novo@vetsync.com");

        assertThat(resultado.admin().getIdAdmin()).isEqualTo(2L);
        verify(emailService).enviar(eq("novo@vetsync.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve falhar cadastro com 409 quando e-mail já existe")
    void cadastrar_EmailDuplicado() {
        when(adminRepository.findByDsEmail("existente@vetsync.com")).thenReturn(Optional.of(new Admin()));

        assertThatThrownBy(() -> adminService.cadastrar("Nome", "existente@vetsync.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("E-mail já cadastrado");

        verify(adminRepository, never()).save(any());
    }
}
