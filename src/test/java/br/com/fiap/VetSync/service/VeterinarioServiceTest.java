package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.entity.Veterinario;
import br.com.fiap.VetSync.repository.ClinicaRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VeterinarioServiceTest {

    @Mock
    private VeterinarioRepository veterinarioRepository;

    @Mock
    private ClinicaRepository clinicaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private VeterinarioService veterinarioService;

    @Test
    @DisplayName("Deve cadastrar veterinário, gerar CRM de 6 dígitos, senha temporária e enviar e-mail")
    void cadastrar_Sucesso() {
        Clinica clinica = Clinica.builder().idClinica(1L).nmClinica("Clyvo Vet").build();
        when(veterinarioRepository.existsByDsEmail("vet@clyvovet.com")).thenReturn(false);
        when(clinicaRepository.findById(1L)).thenReturn(Optional.of(clinica));
        when(veterinarioRepository.existsByNrCrmv(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pwd");
        when(veterinarioRepository.save(any(Veterinario.class))).thenAnswer(inv -> {
            Veterinario v = inv.getArgument(0);
            v.setIdVeterinario(10L);
            return v;
        });

        VeterinarioService.NovoVeterinario novo = veterinarioService.cadastrar("Dr. Pedro", "vet@clyvovet.com", 1L);

        assertThat(novo.veterinario().getIdVeterinario()).isEqualTo(10L);
        assertThat(novo.veterinario().getNrCrmv()).matches("^\\d{6}$");
        assertThat(novo.senhaTemporaria()).hasSize(10);
        verify(emailService).enviar(eq("vet@clyvovet.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve falhar ao cadastrar veterinário com e-mail duplicado")
    void cadastrar_EmailDuplicado() {
        when(veterinarioRepository.existsByDsEmail("existente@vet.com")).thenReturn(true);

        assertThatThrownBy(() -> veterinarioService.cadastrar("Nome", "existente@vet.com", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("E-mail já cadastrado");

        verify(veterinarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar veterinário por ID")
    void buscarPorId_Sucesso() {
        Veterinario v = Veterinario.builder().idVeterinario(1L).nmVeterinario("Dra. Paula").build();
        when(veterinarioRepository.findById(1L)).thenReturn(Optional.of(v));

        Veterinario res = veterinarioService.buscarPorId(1L);
        assertThat(res.getNmVeterinario()).isEqualTo("Dra. Paula");
    }

    @Test
    @DisplayName("Deve atualizar dados do veterinário")
    void atualizar_Sucesso() {
        Veterinario v = Veterinario.builder().idVeterinario(1L).nmVeterinario("Nome Antigo").build();
        Clinica clinica = Clinica.builder().idClinica(2L).build();

        when(veterinarioRepository.findById(1L)).thenReturn(Optional.of(v));
        when(clinicaRepository.findById(2L)).thenReturn(Optional.of(clinica));
        when(veterinarioRepository.save(any(Veterinario.class))).thenAnswer(inv -> inv.getArgument(0));

        Veterinario res = veterinarioService.atualizar(1L, "Nome Atualizado", 2L);
        assertThat(res.getNmVeterinario()).isEqualTo("Nome Atualizado");
        assertThat(res.getClinica()).isEqualTo(clinica);
    }
}
