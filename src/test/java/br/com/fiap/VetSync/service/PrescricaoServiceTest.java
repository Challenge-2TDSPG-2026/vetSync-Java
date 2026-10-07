package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescricaoServiceTest {

    @Mock
    private PrescricaoRepository prescricaoRepository;

    @Mock
    private MedicamentoRepository medicamentoRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private EventoService eventoService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PrescricaoService prescricaoService;

    @Test
    @DisplayName("Deve solicitar prescrição com sucesso pelo veterinário responsável")
    void solicitar_Sucesso() {
        Veterinario vet = Veterinario.builder().idVeterinario(5L).build();
        EventoSaude evento = EventoSaude.builder().idEvento(10L).veterinario(vet).build();
        Medicamento med = Medicamento.builder().idMedicamento(20L).nmMedicamento("Antibiótico").build();

        when(eventoService.buscarPorId(10L)).thenReturn(evento);
        when(medicamentoRepository.findById(20L)).thenReturn(Optional.of(med));
        when(prescricaoRepository.save(any(Prescricao.class))).thenAnswer(inv -> inv.getArgument(0));

        Prescricao p = prescricaoService.solicitar(10L, 20L, "1 comp a cada 12h",
                LocalDate.now(), LocalDate.now().plusDays(7), 2, 5L);

        assertThat(p.getDsStatus()).isEqualTo(StatusPrescricao.SOLICITADO);
        assertThat(p.getDsPosologia()).isEqualTo("1 comp a cada 12h");
        assertThat(p.getMedicamento()).isEqualTo(med);
        assertThat(p.getEvento()).isEqualTo(evento);
    }

    @Test
    @DisplayName("Deve lançar 403 ao solicitar prescrição para evento sob responsabilidade de outro vet")
    void solicitar_VeterinarioNaoResponsavel() {
        Veterinario vet = Veterinario.builder().idVeterinario(5L).build();
        EventoSaude evento = EventoSaude.builder().idEvento(10L).veterinario(vet).build();

        when(eventoService.buscarPorId(10L)).thenReturn(evento);

        assertThatThrownBy(() -> prescricaoService.solicitar(10L, 20L, "Posologia",
                LocalDate.now(), null, 1, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Você não é o veterinário responsável");

        verify(prescricaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve liberar prescrição aprovada e disparar e-mail ao tutor")
    void liberar_AprovadoComEmail() {
        Tutor tutor = Tutor.builder().nmTutor("Carlos").dsEmail("carlos@email.com").build();
        Pet pet = Pet.builder().nmPet("Bidu").tutor(tutor).build();
        EventoSaude evento = EventoSaude.builder().pet(pet).build();
        Medicamento med = Medicamento.builder().nmMedicamento("Anti-inflamatório").build();
        Admin admin = Admin.builder().idAdmin(1L).build();

        Prescricao prescricao = Prescricao.builder()
                .idPrescricao(100L)
                .dsStatus(StatusPrescricao.SOLICITADO)
                .evento(evento)
                .medicamento(med)
                .dsPosologia("1 comprimido ao dia")
                .dtInicio(LocalDate.now())
                .build();

        when(prescricaoRepository.findById(100L)).thenReturn(Optional.of(prescricao));
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(prescricaoRepository.save(any(Prescricao.class))).thenAnswer(inv -> inv.getArgument(0));

        Prescricao liberada = prescricaoService.liberar(100L, 1L, true);

        assertThat(liberada.getDsStatus()).isEqualTo(StatusPrescricao.LIBERADO);
        assertThat(liberada.getAdminValidador()).isEqualTo(admin);
        verify(emailService).enviarComAnexo(eq("carlos@email.com"), anyString(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve negar prescrição e não disparar e-mail")
    void liberar_NegadoSemEmail() {
        Admin admin = Admin.builder().idAdmin(1L).build();
        Prescricao prescricao = Prescricao.builder()
                .idPrescricao(100L)
                .dsStatus(StatusPrescricao.SOLICITADO)
                .build();

        when(prescricaoRepository.findById(100L)).thenReturn(Optional.of(prescricao));
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(prescricaoRepository.save(any(Prescricao.class))).thenAnswer(inv -> inv.getArgument(0));

        Prescricao negada = prescricaoService.liberar(100L, 1L, false);

        assertThat(negada.getDsStatus()).isEqualTo(StatusPrescricao.NEGADO);
        verify(emailService, never()).enviar(anyString(), anyString(), anyString());
    }
}
