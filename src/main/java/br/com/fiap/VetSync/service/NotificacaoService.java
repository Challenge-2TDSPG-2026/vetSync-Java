package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final TutorRepository tutorRepository;
    private final DispositivoPushRepository dispositivoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final PreferenciaNotificacaoRepository preferenciaRepository;

    /** Notificações pertencem somente a tutores; qualquer outro perfil recebe 403. */
    private Tutor tutorDoUsuario(String email) {
        return tutorRepository.findByDsEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "A conta autenticada não possui notificações de tutor"));
    }

    @Transactional
    public DispositivoPush registrarDispositivo(String email, String token, PlataformaPush plataforma,
                                                String nomeDispositivo, String fusoHorario) {
        Tutor tutor = tutorDoUsuario(email);
        String tokenLimpo = token.trim();

        // Se o aparelho trocou de conta, desativa o token nas contas anteriores.
        List<DispositivoPush> deOutros = dispositivoRepository
                .findAtivosDeOutrosTutores(tokenLimpo, tutor.getIdTutor());
        deOutros.forEach(d -> d.setAtivo(false));
        dispositivoRepository.saveAll(deOutros);

        DispositivoPush dispositivo = dispositivoRepository
                .findByTutorAndTokenAndPlataforma(tutor, tokenLimpo, plataforma)
                .orElseGet(() -> DispositivoPush.builder()
                        .tutor(tutor).token(tokenLimpo).plataforma(plataforma).build());
        dispositivo.setNomeDispositivo(nomeDispositivo);
        dispositivo.setFusoHorario(fusoHorario);
        dispositivo.setAtivo(true);
        dispositivo.setUltimoUsoEm(LocalDateTime.now());
        return dispositivoRepository.save(dispositivo);
    }

    @Transactional
    public void removerDispositivo(String email, String token) {
        Tutor tutor = tutorDoUsuario(email);
        List<DispositivoPush> dispositivos = dispositivoRepository.findByTutorAndToken(tutor, token.trim());
        LocalDateTime agora = LocalDateTime.now();
        dispositivos.forEach(d -> {
            d.setAtivo(false);
            d.setUltimoUsoEm(agora);
        });
        dispositivoRepository.saveAll(dispositivos);
    }

    @Transactional(readOnly = true)
    public Page<Notificacao> listar(String email, Boolean lida, Pageable pageable) {
        Tutor tutor = tutorDoUsuario(email);
        return lida == null
                ? notificacaoRepository.findByTutor(tutor, pageable)
                : notificacaoRepository.findByTutorAndLida(tutor, lida, pageable);
    }

    @Transactional
    public Notificacao marcarComoLida(String email, Long id) {
        Tutor tutor = tutorDoUsuario(email);
        Notificacao notificacao = notificacaoRepository.findByIdNotificacaoAndTutor(id, tutor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificação não encontrada"));
        notificacao.setLida(true);
        return notificacaoRepository.save(notificacao);
    }

    @Transactional
    public int marcarTodasComoLidas(String email) {
        return notificacaoRepository.markAllAsReadByTutor(tutorDoUsuario(email));
    }

    @Transactional
    public PreferenciaNotificacao buscarPreferencias(String email) {
        Tutor tutor = tutorDoUsuario(email);
        return preferenciaRepository.findByTutor(tutor)
                .orElseGet(() -> preferenciaRepository.save(
                        PreferenciaNotificacao.builder().tutor(tutor).build()));
    }

    @Transactional
    public PreferenciaNotificacao atualizarPreferencias(String email, PreferenciaNotificacao atualizada) {
        PreferenciaNotificacao atual = buscarPreferencias(email);
        atual.setPushAtivo(atualizada.isPushAtivo());
        atual.setLembreteSeteDias(atualizada.isLembreteSeteDias());
        atual.setLembreteUmDia(atualizada.isLembreteUmDia());
        atual.setLembreteDuasHoras(atualizada.isLembreteDuasHoras());
        atual.setVacinasVencendo(atualizada.isVacinasVencendo());
        atual.setRetornosPendentes(atualizada.isRetornosPendentes());
        atual.setConvitesDeAcesso(atualizada.isConvitesDeAcesso());
        atual.setResgates(atualizada.isResgates());
        return preferenciaRepository.save(atual);
    }
}
