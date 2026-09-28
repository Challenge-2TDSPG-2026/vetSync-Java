package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    @Mock TutorRepository tutorRepository;
    @Mock DispositivoPushRepository dispositivoRepository;
    @Mock NotificacaoRepository notificacaoRepository;
    @Mock PreferenciaNotificacaoRepository preferenciaRepository;

    @InjectMocks NotificacaoService service;

    @Test
    void registraDispositivoDeFormaIdempotenteEReativa() {
        Tutor tutor = Tutor.builder().idTutor(1L).dsEmail("tutor@teste.com").build();
        DispositivoPush dispositivo = DispositivoPush.builder().tutor(tutor).token("token")
                .plataforma(PlataformaPush.ANDROID).ativo(false).build();
        when(tutorRepository.findByDsEmail(tutor.getDsEmail())).thenReturn(Optional.of(tutor));
        when(dispositivoRepository.findByTutorAndTokenAndPlataforma(
                tutor, "token", PlataformaPush.ANDROID)).thenReturn(Optional.of(dispositivo));
        when(dispositivoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DispositivoPush salvo = service.registrarDispositivo(tutor.getDsEmail(), "token",
                PlataformaPush.ANDROID, "celular", "America/Sao_Paulo");

        assertThat(salvo.isAtivo()).isTrue();
        assertThat(salvo.getNomeDispositivo()).isEqualTo("celular");
        verify(dispositivoRepository).save(dispositivo);
    }

    @Test
    void criaPreferenciasComValoresPadraoQuandoAindaNaoExistem() {
        Tutor tutor = Tutor.builder().idTutor(1L).dsEmail("tutor@teste.com").build();
        when(tutorRepository.findByDsEmail(tutor.getDsEmail())).thenReturn(Optional.of(tutor));
        when(preferenciaRepository.findByTutor(tutor)).thenReturn(Optional.empty());
        when(preferenciaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PreferenciaNotificacao preferencias = service.buscarPreferencias(tutor.getDsEmail());

        assertThat(preferencias.isPushAtivo()).isTrue();
        assertThat(preferencias.isLembreteSeteDias()).isTrue();
        assertThat(preferencias.isLembreteDuasHoras()).isFalse();
        assertThat(preferencias.getTutor()).isSameAs(tutor);
    }
}
