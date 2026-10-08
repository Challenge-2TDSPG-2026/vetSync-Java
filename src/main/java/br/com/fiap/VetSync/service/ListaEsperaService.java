package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListaEsperaService {

    private static final Pattern HORA = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");
    private static final long JANELA_MAXIMA_DIAS = 60;
    private static final Set<StatusListaEspera> ATIVOS = Set.of(StatusListaEspera.AGUARDANDO, StatusListaEspera.NOTIFICADO);

    private final ListaEsperaRepository listaEsperaRepository;
    private final TutorRepository tutorRepository;
    private final PetRepository petRepository;
    private final ServicoClinicaRepository servicoClinicaRepository;
    private final VinculoTutorClinicaRepository vinculoRepository;
    private final NotificacaoEnvioService notificacaoEnvioService;

    @Value("${app.espera.max-notificacoes-por-vaga:3}")
    private int maxNotificacoesPorVaga;

    @Value("${app.espera.horas-para-reabrir:12}")
    private long horasParaReabrir;

    @Value("${app.notificacoes.job-habilitado:true}")
    private boolean jobHabilitado;

    public record EntradaListaEspera(Long idPet, Long idServico, LocalDate dataInicio, LocalDate dataFim,
                                     String horaMin, String horaMax,
                                     Long idVeterinario, Long idProfissionalEstetica) {}

    /** Horário que acabou de ficar livre. */
    public record VagaLiberada(Long idServicoClinica, Long idVeterinario, Long idProfissionalEstetica,
                               Long idPetQueLiberou, LocalDate data, String hora) {}

    // ───────────────────────── Fluxo do tutor ─────────────────────────

    @Transactional
    public ListaEspera entrar(String email, EntradaListaEspera dados) {
        Tutor tutor = tutorDoUsuario(email);
        Long idClinica = clinicaDoTutor(tutor);

        ServicoClinica servico = servicoClinicaRepository
                .findByIdServicoClinicaAndClinica_IdClinica(dados.idServico(), idClinica)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Serviço não encontrado"));
        if (!Boolean.TRUE.equals(servico.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Serviço indisponível");
        }
        Pet pet = petRepository.findById(dados.idPet())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet não encontrado"));

        validarJanela(dados);

        if (listaEsperaRepository.existsByTutorAndPet_IdPetAndServicoClinica_IdServicoClinicaAndStatusIn(
                tutor, pet.getIdPet(), servico.getIdServicoClinica(), ATIVOS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse pet já está na lista de espera desse serviço");
        }

        return listaEsperaRepository.save(ListaEspera.builder()
                .tutor(tutor)
                .pet(pet)
                .servicoClinica(servico)
                .idVeterinario(dados.idVeterinario())
                .idProfissionalEstetica(dados.idProfissionalEstetica())
                .dataInicio(dados.dataInicio())
                .dataFim(dados.dataFim())
                .horaMin(vazioParaNull(dados.horaMin()))
                .horaMax(vazioParaNull(dados.horaMax()))
                .build());
    }

    @Transactional(readOnly = true)
    public List<ListaEspera> listarAtivas(String email) {
        return listaEsperaRepository.findByTutorAndStatusInOrderByCriadaEmDesc(tutorDoUsuario(email), ATIVOS);
    }

    @Transactional
    public void cancelar(String email, Long idEspera) {
        ListaEspera entrada = listaEsperaRepository.findByIdEsperaAndTutor(idEspera, tutorDoUsuario(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada não encontrada"));
        if (!ATIVOS.contains(entrada.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa entrada já foi encerrada");
        }
        entrada.setStatus(StatusListaEspera.CANCELADO);
        listaEsperaRepository.save(entrada);
    }

    // ───────────────────────── Aviso de vaga ─────────────────────────

    /**
     * Chamado quando um horário fica livre (cancelamento ou reagendamento). Avisa, por ordem de chegada,
     * os primeiros da fila cujo serviço, data, horário e profissional são compatíveis.
     * O processamento roda depois do commit da operação que liberou o horário e em outra thread:
     * falha aqui nunca desfaz o cancelamento, e ninguém é avisado de uma vaga que não chegou a existir.
     */
    public void notificarVagaLiberada(VagaLiberada vaga) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    CompletableFuture.runAsync(() -> processarVaga(vaga));
                }
            });
        } else {
            CompletableFuture.runAsync(() -> processarVaga(vaga));
        }
    }

    void processarVaga(VagaLiberada vaga) {
        try {
            if (vaga == null || vaga.idServicoClinica() == null || vaga.data() == null || vaga.hora() == null
                    || vaga.data().isBefore(LocalDate.now())) {
                return;
            }

            List<ListaEspera> candidatos = listaEsperaRepository.candidatosParaVaga(
                            StatusListaEspera.AGUARDANDO, vaga.idServicoClinica(), vaga.data()).stream()
                    .filter(l -> !l.getPet().getIdPet().equals(vaga.idPetQueLiberou()))
                    .filter(l -> aceitaProfissional(l, vaga))
                    .filter(l -> aceitaHorario(l, vaga.hora()))
                    .limit(Math.max(1, maxNotificacoesPorVaga))
                    .toList();

            for (ListaEspera entrada : candidatos) {
                entrada.setStatus(StatusListaEspera.NOTIFICADO);
                entrada.setDataVaga(vaga.data());
                entrada.setHoraVaga(vaga.hora());
                entrada.setTipoProfissionalVaga(vaga.idVeterinario() != null ? "VETERINARIO" : "ESTETICA");
                entrada.setIdProfissionalVaga(vaga.idVeterinario() != null ? vaga.idVeterinario() : vaga.idProfissionalEstetica());
                entrada.setNotificadaEm(LocalDateTime.now());
                listaEsperaRepository.save(entrada);

                String quando = vaga.data().format(DateTimeFormatter.ofPattern("dd/MM")) + " às " + vaga.hora();
                notificacaoEnvioService.notificar(
                        entrada.getTutor(),
                        TipoNotificacao.VAGA_DISPONIVEL,
                        "Surgiu uma vaga!",
                        entrada.getServicoClinica().getNmServico() + " para " + entrada.getPet().getNmPet()
                                + " em " + quando + ". Toque para agendar antes que alguém pegue.",
                        "LISTA_ESPERA",
                        entrada.getIdEspera(),
                        Map.of(
                                "rota", "/(tutor)/agendar-servico",
                                "petId", String.valueOf(entrada.getPet().getIdPet()),
                                "servicoId", entrada.getServicoClinica().getIdServicoClinica(),
                                "data", vaga.data().toString(),
                                "hora", vaga.hora()));
            }
        } catch (Exception e) {
            log.warn("Não foi possível avisar a lista de espera: {}", e.getMessage());
        }
    }

    // ───────────────────────── Manutenção periódica ─────────────────────────

    /**
     * Encerra o que perdeu o sentido: período vencido (EXPIRADO), tutor que já agendou (ATENDIDO)
     * e vaga avisada que ninguém pegou (volta para AGUARDANDO).
     */
    @Scheduled(fixedDelayString = "${app.notificacoes.job-intervalo-ms:300000}",
            initialDelayString = "${app.notificacoes.job-delay-inicial-ms:60000}")
    @Transactional
    public void manterFila() {
        if (!jobHabilitado) return;
        LocalDate hoje = LocalDate.now();
        LocalDateTime limiteReabertura = LocalDateTime.now().minus(horasParaReabrir, ChronoUnit.HOURS);

        for (ListaEspera e : listaEsperaRepository.findByStatusIn(ATIVOS)) {
            if (e.getDataFim().isBefore(hoje)) {
                e.setStatus(StatusListaEspera.EXPIRADO);
            } else if (jaAgendou(e)) {
                e.setStatus(StatusListaEspera.ATENDIDO);
            } else if (e.getStatus() == StatusListaEspera.NOTIFICADO
                    && e.getNotificadaEm() != null && e.getNotificadaEm().isBefore(limiteReabertura)) {
                e.setStatus(StatusListaEspera.AGUARDANDO);
                e.limparVaga();
            } else {
                continue;
            }
            listaEsperaRepository.save(e);
        }
    }

    private boolean jaAgendou(ListaEspera e) {
        return listaEsperaRepository.contarEventos(e.getPet().getIdPet(), e.getServicoClinica().getIdServicoClinica(),
                StatusEvento.AGENDADO, e.getDataInicio(), e.getDataFim()) > 0;
    }

    // ───────────────────────── Regras auxiliares ─────────────────────────

    private boolean aceitaProfissional(ListaEspera l, VagaLiberada v) {
        if (l.getIdVeterinario() != null) return l.getIdVeterinario().equals(v.idVeterinario());
        if (l.getIdProfissionalEstetica() != null) return l.getIdProfissionalEstetica().equals(v.idProfissionalEstetica());
        return true;
    }

    private boolean aceitaHorario(ListaEspera l, String hora) {
        if (l.getHoraMin() != null && hora.compareTo(l.getHoraMin()) < 0) return false;
        return l.getHoraMax() == null || hora.compareTo(l.getHoraMax()) <= 0;
    }

    private void validarJanela(EntradaListaEspera d) {
        if (d.dataInicio() == null || d.dataFim() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o período desejado");
        }
        if (d.dataInicio().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O período deve começar hoje ou depois");
        }
        if (d.dataFim().isBefore(d.dataInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data final deve ser igual ou posterior à inicial");
        }
        if (ChronoUnit.DAYS.between(d.dataInicio(), d.dataFim()) > JANELA_MAXIMA_DIAS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O período da lista de espera pode ter no máximo " + JANELA_MAXIMA_DIAS + " dias");
        }
        String min = vazioParaNull(d.horaMin());
        String max = vazioParaNull(d.horaMax());
        if ((min != null && !HORA.matcher(min).matches()) || (max != null && !HORA.matcher(max).matches())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horários devem estar no formato HH:mm");
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O horário inicial deve ser antes do final");
        }
        if (d.idVeterinario() != null && d.idProfissionalEstetica() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha um único profissional (ou nenhum)");
        }
    }

    private Tutor tutorDoUsuario(String email) {
        return tutorRepository.findByDsEmail(email).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta de tutor necessária"));
    }

    private Long clinicaDoTutor(Tutor tutor) {
        VinculoTutorClinica vinculo = vinculoRepository.findByTutor_IdTutorAndDtEncerramentoIsNull(tutor.getIdTutor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Tutor sem clínica vinculada"));
        if (!vinculo.getClinica().estaContratanteAtiva()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Clínica com contrato inativo");
        }
        return vinculo.getClinica().getIdClinica();
    }

    private static String vazioParaNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}