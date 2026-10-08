package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AgendaServicoClinicaService {
    private final TutorRepository tutorRepository;
    private final VinculoTutorClinicaRepository vinculoRepository;
    private final ServicoClinicaRepository servicoRepository;
    private final ServicoProfissionalRepository profissionalRepository;
    private final HorarioClinicaRepository horarioRepository;
    private final DisponibilidadeRepository disponibilidadeRepository;
    private final DisponibilidadeEsteticaRepository esteticaDisponibilidadeRepository;
    private final EventoSaudeRepository eventoRepository;
    private final AgendaService agendaService;
    private final AgendaEsteticaService esteticaService;

    public record ServicoDisponivel(Long id, String nome, String categoria, int duracaoMinutos) {}
    public record SlotServico(Long idServico, String tipoProfissional, Long idProfissional,
                              String nomeProfissional, String hora) {}

    @Transactional(readOnly = true)
    public List<ServicoDisponivel> servicosDoTutor(String email) {
        Long idClinica = clinicaDoTutor(email);
        return servicoRepository.findByClinica_IdClinicaAndAtivoTrueOrderByNmServico(idClinica).stream()
                .map(s -> new ServicoDisponivel(s.getIdServicoClinica(), s.getNmServico(),
                        s.getDsCategoria(), s.getNrDuracaoMinutos())).toList();
    }

    @Transactional(readOnly = true)
    public List<SlotServico> slots(String email, Long idServico, LocalDate data) {
        Long idClinica = clinicaDoTutor(email);
        ServicoClinica servico = servicoAtivo(idServico, idClinica);
        if (data == null || data.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione hoje ou uma data futura");
        }
        List<SlotServico> slots = new ArrayList<>();
        int dia = data.getDayOfWeek().getValue();
        var horariosClinica = horarioRepository.findByClinica_IdClinicaAndNrDiaSemana(idClinica, dia);
        if (horariosClinica.isEmpty()) return List.of();
        for (ServicoProfissional profissional : profissionalRepository.findByServico_IdServicoClinica(idServico)) {
            Long idVet = profissional.getVeterinario() == null ? null : profissional.getVeterinario().getIdVeterinario();
            Long idEst = profissional.getProfissionalEstetica() == null ? null
                    : profissional.getProfissionalEstetica().getIdProfissionalEstetica();
            if (idVet != null && agendaService.estaBloqueado(idVet, data)) continue;
            if (idEst != null && esteticaService.estaBloqueado(idEst, data)) continue;
            if (idEst != null && servico.getBaseEstetica() != null
                    && !profissional.getProfissionalEstetica().getServicos().contains(servico.getBaseEstetica())) continue;
            List<String[]> intervalos = idVet != null
                    ? disponibilidadeRepository.findByVeterinario_IdVeterinario(idVet).stream()
                        .filter(d -> d.getNrDiaSemana().equals(dia))
                        .map(d -> new String[]{d.getHrInicio(), d.getHrFim()}).toList()
                    : esteticaDisponibilidadeRepository.findByProfissionalEstetica_IdProfissionalEstetica(idEst).stream()
                        .filter(d -> d.getNrDiaSemana().equals(dia))
                        .map(d -> new String[]{d.getHrInicio(), d.getHrFim()}).toList();
            for (String[] intervalo : intervalos) {
                for (LocalTime hora = LocalTime.parse(intervalo[0]);
                     !hora.plusMinutes(servico.getNrDuracaoMinutos()).isAfter(LocalTime.parse(intervalo[1]));
                     hora = hora.plusMinutes(15)) {
                    final LocalTime inicio = hora;
                    boolean aberta = horariosClinica.stream().anyMatch(h ->
                            !inicio.isBefore(LocalTime.parse(h.getHrInicio())) &&
                                    !inicio.plusMinutes(servico.getNrDuracaoMinutos()).isAfter(LocalTime.parse(h.getHrFim())));
                    if (aberta && !ocupado(idVet, idEst, data, hora, servico.getNrDuracaoMinutos(), null)) {
                        slots.add(new SlotServico(idServico, idVet != null ? "VETERINARIO" : "ESTETICA",
                                idVet != null ? idVet : idEst,
                                idVet != null ? profissional.getVeterinario().getNmVeterinario()
                                        : profissional.getProfissionalEstetica().getNmProfissionalEstetica(),
                                hora.toString()));
                    }
                }
            }
        }
        return slots.stream().distinct().sorted(Comparator.comparing(SlotServico::hora)
                .thenComparing(SlotServico::nomeProfissional)).toList();
    }

    @Transactional(readOnly = true)
    public ServicoClinica validarReserva(Long idServico, Long idClinica, Long idVet, Long idEst,
                                          LocalDate data, String horario, Long idEventoIgnorar) {
        ServicoClinica servico = servicoAtivo(idServico, idClinica);
        if ((idVet == null) == (idEst == null) || data == null || data.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profissional ou data inválidos");
        }
        LocalTime hora;
        try { hora = LocalTime.parse(horario); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horário inválido"); }
        boolean atende = profissionalRepository.findByServico_IdServicoClinica(idServico).stream().anyMatch(p ->
                idVet != null ? p.getVeterinario() != null && idVet.equals(p.getVeterinario().getIdVeterinario())
                        : p.getProfissionalEstetica() != null && idEst.equals(p.getProfissionalEstetica().getIdProfissionalEstetica()));
        if (!atende) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Profissional não atende este serviço");
        if (idEst != null && servico.getBaseEstetica() != null && profissionalRepository.findByServico_IdServicoClinica(idServico)
                .stream().filter(p -> p.getProfissionalEstetica() != null
                        && idEst.equals(p.getProfissionalEstetica().getIdProfissionalEstetica()))
                .noneMatch(p -> p.getProfissionalEstetica().getServicos().contains(servico.getBaseEstetica()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Profissional não atende mais o serviço base");
        }
        int dia = data.getDayOfWeek().getValue();
        boolean clinicaAberta = horarioRepository.findByClinica_IdClinicaAndNrDiaSemana(idClinica, dia).stream()
                .anyMatch(h -> !hora.isBefore(LocalTime.parse(h.getHrInicio())) &&
                        !hora.plusMinutes(servico.getNrDuracaoMinutos()).isAfter(LocalTime.parse(h.getHrFim())));
        boolean profissionalDisponivel = idVet != null
                ? disponibilidadeRepository.findByVeterinario_IdVeterinario(idVet).stream()
                    .anyMatch(d -> d.getNrDiaSemana().equals(dia) && !hora.isBefore(LocalTime.parse(d.getHrInicio()))
                            && !hora.plusMinutes(servico.getNrDuracaoMinutos()).isAfter(LocalTime.parse(d.getHrFim())))
                : esteticaDisponibilidadeRepository.findByProfissionalEstetica_IdProfissionalEstetica(idEst).stream()
                    .anyMatch(d -> d.getNrDiaSemana().equals(dia) && !hora.isBefore(LocalTime.parse(d.getHrInicio()))
                            && !hora.plusMinutes(servico.getNrDuracaoMinutos()).isAfter(LocalTime.parse(d.getHrFim())));
        boolean bloqueado = idVet != null ? agendaService.estaBloqueado(idVet, data) : esteticaService.estaBloqueado(idEst, data);
        if (!clinicaAberta || !profissionalDisponivel || bloqueado ||
                ocupado(idVet, idEst, data, hora, servico.getNrDuracaoMinutos(), idEventoIgnorar)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este horário não está disponível");
        }
        return servico;
    }

    private boolean ocupado(Long idVet, Long idEst, LocalDate data, LocalTime hora, int duracao, Long ignorar) {
        List<EventoSaude> eventos = idVet != null
                ? eventoRepository.findByVeterinario_IdVeterinarioAndDtEvento(idVet, data)
                : eventoRepository.findByProfissionalEstetica_IdProfissionalEsteticaAndDtEvento(idEst, data);
        LocalTime fim = hora.plusMinutes(duracao);
        return eventos.stream().filter(e -> e.getDsStatus() == StatusEvento.AGENDADO && e.getHrEvento() != null)
                .filter(e -> ignorar == null || !ignorar.equals(e.getIdEvento()))
                .anyMatch(e -> {
                    LocalTime inicioExistente = LocalTime.parse(e.getHrEvento());
                    int minutos = e.getServicoClinica() != null ? e.getServicoClinica().getNrDuracaoMinutos()
                            : e.getTipoEvento().getNrDuracaoMinutos();
                    return hora.isBefore(inicioExistente.plusMinutes(minutos)) && inicioExistente.isBefore(fim);
                });
    }

    private ServicoClinica servicoAtivo(Long idServico, Long idClinica) {
        ServicoClinica servico = servicoRepository.findByIdServicoClinicaAndClinica_IdClinica(idServico, idClinica)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Serviço não encontrado"));
        if (!Boolean.TRUE.equals(servico.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Serviço indisponível");
        }
        return servico;
    }

    private Long clinicaDoTutor(String email) {
        Tutor tutor = tutorRepository.findByDsEmail(email).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta de tutor necessária"));
        VinculoTutorClinica vinculo = vinculoRepository.findByTutor_IdTutorAndDtEncerramentoIsNull(tutor.getIdTutor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Tutor sem clínica vinculada"));
        if (!vinculo.getClinica().estaContratanteAtiva()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Clínica com contrato inativo");
        }
        return vinculo.getClinica().getIdClinica();
    }
}
