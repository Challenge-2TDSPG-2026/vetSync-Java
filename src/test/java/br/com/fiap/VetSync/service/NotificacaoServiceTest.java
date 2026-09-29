package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    @Mock TutorRepository tutorRepository;
    @Mock DispositivoPushRepository dispositivoRepository;
    @Mock NotificacaoRepository notificacaoRepository;
    @Mock PreferenciaNotificacaoRepository preferenciaRepository;

    @InjectMocks NotificacaoService service;

    private Tutor tutor() {
        Tutor tutor = Tutor.builder().idTutor(1L).dsEmail("tutor@teste.com").build();
        when(tutorRepository.findByDsEmail(tutor.getDsEmail())).thenReturn(Optional.of(tutor));
        return tutor;
    }

    @Test
    void registraDispositivoDeFormaIdempotenteEReativa() {
        Tutor tutor = tutor();
        DispositivoPush dispositivo = DispositivoPush.builder().tutor(tutor).token("token")
                .plataforma(PlataformaPush.ANDROID).ativo(false).build();
        when(dispositivoRepository.findByTutorAndTokenAndPlataforma(
                tutor, "token", PlataformaPush.ANDROID)).thenReturn(Optional.of(dispositivo));
        when(dispositivoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DispositivoPush salvo = service.registrarDispositivo(tutor.getDsEmail(), "token",
                PlataformaPush.ANDROID, "celular", "America/Sao_Paulo");

        assertThat(salvo).isSameAs(dispositivo);
        assertThat(salvo.isAtivo()).isTrue();
        assertThat(salvo.getNomeDispositivo()).isEqualTo("celular");
        assertThat(salvo.getFusoHorario()).isEqualTo("America/Sao_Paulo");
        assertThat(salvo.getUltimoUsoEm()).isNotNull();
        verify(dispositivoRepository).save(dispositivo);
    }

    @Test
    void criaDispositivoQuandoNaoExiste() {
        Tutor tutor = tutor();
        when(dispositivoRepository.findByTutorAndTokenAndPlataforma(
                tutor, "novo", PlataformaPush.WEB)).thenReturn(Optional.empty());
        when(dispositivoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DispositivoPush salvo = service.registrarDispositivo(tutor.getDsEmail(), "novo",
                PlataformaPush.WEB, "navegador", "America/Sao_Paulo");

        assertThat(salvo.getTutor()).isSameAs(tutor);
        assertThat(salvo.getToken()).isEqualTo("novo");
        assertThat(salvo.getPlataforma()).isEqualTo(PlataformaPush.WEB);
        assertThat(salvo.isAtivo()).isTrue();
    }

    @Test
    void desativaTokenDeOutroTutorAoRegistrarNoMesmoAparelho() {
        Tutor tutor = tutor();
        Tutor anterior = Tutor.builder().idTutor(2L).dsEmail("anterior@teste.com").build();
        DispositivoPush antigo = DispositivoPush.builder().tutor(anterior).token("token")
                .plataforma(PlataformaPush.ANDROID).ativo(true).build();
        when(dispositivoRepository.findAtivosDeOutrosTutores("token", 1L)).thenReturn(List.of(antigo));
        when(dispositivoRepository.findByTutorAndTokenAndPlataforma(
                tutor, "token", PlataformaPush.ANDROID)).thenReturn(Optional.empty());
        when(dispositivoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.registrarDispositivo(tutor.getDsEmail(), "token", PlataformaPush.ANDROID, "celular", null);

        assertThat(antigo.isAtivo()).isFalse();
    }

    @Test
    void removeDesativaSomenteOsDispositivosDoTutorAutenticado() {
        Tutor tutor = tutor();
        DispositivoPush android = DispositivoPush.builder().tutor(tutor).token("token")
                .plataforma(PlataformaPush.ANDROID).ativo(true).build();
        DispositivoPush web = DispositivoPush.builder().tutor(tutor).token("token")
                .plataforma(PlataformaPush.WEB).ativo(true).build();
        when(dispositivoRepository.findByTutorAndToken(tutor, "token")).thenReturn(List.of(android, web));

        service.removerDispositivo(tutor.getDsEmail(), "token");

        assertThat(android.isAtivo()).isFalse();
        assertThat(web.isAtivo()).isFalse();
        verify(dispositivoRepository).saveAll(List.of(android, web));
    }

    @Test
    void removerTokenInexistenteNaoFalha() {
        Tutor tutor = tutor();
        when(dispositivoRepository.findByTutorAndToken(tutor, "nada")).thenReturn(List.of());

        service.removerDispositivo(tutor.getDsEmail(), "nada");

        verify(dispositivoRepository).saveAll(List.of());
    }

    @Test
    void listaComFiltroDeLidaUsaOMetodoFiltrado() {
        Tutor tutor = tutor();
        Pageable pageable = PageRequest.of(0, 50);
        when(notificacaoRepository.findByTutorAndLida(tutor, false, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(service.listar(tutor.getDsEmail(), false, pageable)).isEmpty();

        verify(notificacaoRepository).findByTutorAndLida(tutor, false, pageable);
        verify(notificacaoRepository, never()).findByTutor(any(), any());
    }

    @Test
    void listaSemFiltroUsaFindByTutor() {
        Tutor tutor = tutor();
        Pageable pageable = PageRequest.of(0, 50);
        when(notificacaoRepository.findByTutor(tutor, pageable)).thenReturn(new PageImpl<>(List.of()));

        service.listar(tutor.getDsEmail(), null, pageable);

        verify(notificacaoRepository).findByTutor(tutor, pageable);
        verify(notificacaoRepository, never()).findByTutorAndLida(any(), anyBoolean(), any());
    }

    @Test
    void marcaComoLidaSomenteNotificacaoDoProprioTutor() {
        Tutor tutor = tutor();
        Notificacao notificacao = Notificacao.builder().idNotificacao(5L).tutor(tutor).lida(false).build();
        when(notificacaoRepository.findByIdNotificacaoAndTutor(5L, tutor)).thenReturn(Optional.of(notificacao));
        when(notificacaoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.marcarComoLida(tutor.getDsEmail(), 5L).isLida()).isTrue();
    }

    @Test
    void notificacaoDeOutroTutorOuInexistenteRetorna404() {
        Tutor tutor = tutor();
        when(notificacaoRepository.findByIdNotificacaoAndTutor(99L, tutor)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarComoLida(tutor.getDsEmail(), 99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        verify(notificacaoRepository, never()).save(any());
    }

    @Test
    void marcarTodasComoLidasDelegaAoRepositorioLimitadoAoTutor() {
        Tutor tutor = tutor();
        when(notificacaoRepository.markAllAsReadByTutor(tutor)).thenReturn(3);

        assertThat(service.marcarTodasComoLidas(tutor.getDsEmail())).isEqualTo(3);
    }

    @Test
    void usuarioSemTutorRecebe403() {
        when(tutorRepository.findByDsEmail("vet@teste.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar("vet@teste.com", null, PageRequest.of(0, 50)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
        verifyNoInteractions(notificacaoRepository);
    }

    @Test
    void criaPreferenciasComValoresPadraoQuandoAindaNaoExistem() {
        Tutor tutor = tutor();
        when(preferenciaRepository.findByTutor(tutor)).thenReturn(Optional.empty());
        when(preferenciaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PreferenciaNotificacao preferencias = service.buscarPreferencias(tutor.getDsEmail());

        assertThat(preferencias.isPushAtivo()).isTrue();
        assertThat(preferencias.isLembreteSeteDias()).isTrue();
        assertThat(preferencias.isLembreteUmDia()).isTrue();
        assertThat(preferencias.isLembreteDuasHoras()).isFalse();
        assertThat(preferencias.getTutor()).isSameAs(tutor);
    }

    @Test
    void atualizaPreferenciasExistentes() {
        Tutor tutor = tutor();
        PreferenciaNotificacao atual = PreferenciaNotificacao.builder().tutor(tutor).build();
        when(preferenciaRepository.findByTutor(tutor)).thenReturn(Optional.of(atual));
        when(preferenciaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PreferenciaNotificacao nova = PreferenciaNotificacao.builder().pushAtivo(false)
                .lembreteSeteDias(false).lembreteUmDia(false).lembreteDuasHoras(true)
                .vacinasVencendo(false).retornosPendentes(false).convitesDeAcesso(false).resgates(false).build();

        PreferenciaNotificacao salva = service.atualizarPreferencias(tutor.getDsEmail(), nova);

        assertThat(salva.isPushAtivo()).isFalse();
        assertThat(salva.isLembreteDuasHoras()).isTrue();
        assertThat(salva.isResgates()).isFalse();
    }
}
