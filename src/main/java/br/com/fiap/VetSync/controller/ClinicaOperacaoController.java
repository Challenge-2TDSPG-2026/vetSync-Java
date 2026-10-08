package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import br.com.fiap.VetSync.repository.ServicoClinicaRepository;
import br.com.fiap.VetSync.security.ClinicaAdminAccess;
import br.com.fiap.VetSync.security.PermissaoClinica;
import br.com.fiap.VetSync.service.EventoService;
import br.com.fiap.VetSync.service.PontosService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/clinica-admin")
@PreAuthorize("hasRole('ADMIN_CLINICA')")
@RequiredArgsConstructor
public class ClinicaOperacaoController {
    private final ClinicaAdminAccess access;
    private final EventoSaudeRepository eventoRepository;
    private final ServicoClinicaRepository servicoRepository;
    private final EventoService eventoService;
    private final PontosService pontosService;

    public record EventoDia(Long idEvento, Long idPet, String nomePet, String nomeTutor,
                            String nomeServico, String nomeProfissional, LocalDate data, String hora, String status) {}
    public record AgendarRequest(@NotNull Long idPet, @NotNull Long idServico, Long idVeterinario,
                                 Long idProfissionalEstetica, @NotNull LocalDate data, @NotBlank String hora,
                                 String observacao) {}
    public record ReagendarRequest(@NotNull LocalDate data, @NotBlank String hora) {}
    public record CancelarRequest(@NotBlank String motivo) {}
    public record PontoDados(Long idLancamento, Long idTutor, String nomeTutor, Long idPet, String nomePet,
                            int pontos, String status, LocalDate lancadoEm) {}

    @GetMapping("/agenda/dia")
    public List<EventoDia> agenda(Authentication auth, @RequestParam LocalDate data) {
        Admin admin = access.exigir(auth, PermissaoClinica.AGENDA_VER);
        return eventoRepository.findByClinica_IdClinicaAndDtEvento(admin.getClinica().getIdClinica(), data)
                .stream().map(e -> new EventoDia(e.getIdEvento(), e.getPet().getIdPet(), e.getPet().getNmPet(),
                        e.getPet().getTutor().getNmTutor(), e.getServicoClinica() == null
                                ? e.getTipoEvento().getNmTipoEvento() : e.getServicoClinica().getNmServico(),
                        e.getVeterinario() != null ? e.getVeterinario().getNmVeterinario()
                                : e.getProfissionalEstetica() != null ? e.getProfissionalEstetica().getNmProfissionalEstetica() : null,
                        e.getDtEvento(), e.getHrEvento(), e.getDsStatus().name()))
                .sorted(java.util.Comparator.comparing(EventoDia::hora,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))).toList();
    }

    @PostMapping("/agenda")
    @ResponseStatus(HttpStatus.CREATED)
    public EventoDia agendar(Authentication auth, @Valid @RequestBody AgendarRequest req) {
        Admin admin = access.exigir(auth, PermissaoClinica.AGENDA_CRIAR);
        servicoRepository.findByIdServicoClinicaAndClinica_IdClinica(
                req.idServico(), admin.getClinica().getIdClinica()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Serviço de outra clínica"));
        EventoSaude evento = EventoSaude.builder().dtEvento(req.data()).hrEvento(req.hora())
                .dsObservacao(req.observacao()).dsObservacaoClinica(req.observacao()).build();
        EventoSaude salvo = eventoService.agendarServico(evento, req.idPet(), req.idServico(),
                req.idVeterinario(), req.idProfissionalEstetica());
        return new EventoDia(salvo.getIdEvento(), salvo.getPet().getIdPet(), salvo.getPet().getNmPet(),
                salvo.getPet().getTutor().getNmTutor(), salvo.getServicoClinica().getNmServico(),
                salvo.getVeterinario() != null ? salvo.getVeterinario().getNmVeterinario()
                        : salvo.getProfissionalEstetica().getNmProfissionalEstetica(),
                salvo.getDtEvento(), salvo.getHrEvento(), salvo.getDsStatus().name());
    }

    @PatchMapping("/agenda/{idEvento}/reagendar")
    public void reagendar(Authentication auth, @PathVariable Long idEvento, @Valid @RequestBody ReagendarRequest req) {
        Admin admin = access.exigir(auth, PermissaoClinica.AGENDA_REAGENDAR);
        exigirEventoDaClinica(admin, eventoService.buscarPorId(idEvento));
        eventoService.reagendar(idEvento, req.data(), req.hora());
    }

    @PatchMapping("/agenda/{idEvento}/cancelar")
    public void cancelar(Authentication auth, @PathVariable Long idEvento, @Valid @RequestBody CancelarRequest req) {
        Admin admin = access.exigir(auth, PermissaoClinica.AGENDA_CANCELAR);
        exigirEventoDaClinica(admin, eventoService.buscarPorId(idEvento));
        eventoService.cancelar(idEvento, req.motivo(), null, null);
    }

    @GetMapping("/pontos")
    public List<PontoDados> pontos(Authentication auth, @RequestParam(required = false) StatusLancamentoPontos status) {
        Admin admin = access.exigir(auth, PermissaoClinica.PONTOS_VER);
        return pontosService.listar(admin.getClinica().getIdClinica(), status, null).stream()
                .map(this::pontoDados).toList();
    }

    @PatchMapping("/pontos/{idLancamento}/liberar")
    public PontoDados liberar(Authentication auth, @PathVariable Long idLancamento) {
        Admin admin = access.exigir(auth, PermissaoClinica.PONTOS_LANCAR);
        return pontoDados(pontosService.liberar(idLancamento, admin.getClinica().getIdClinica(), admin.getIdAdmin()));
    }

    private PontoDados pontoDados(LancamentoPontos ponto) {
        Pet pet = ponto.petOrigem();
        Tutor tutor = ponto.tutorOrigem();
        return new PontoDados(ponto.getIdLancamento(), tutor == null ? null : tutor.getIdTutor(),
                tutor == null ? null : tutor.getNmTutor(), pet == null ? null : pet.getIdPet(),
                pet == null ? null : pet.getNmPet(), ponto.getNrPontos(),
                ponto.statusEfetivo(LocalDate.now()).name(), ponto.getDtLancamento());
    }

    private void exigirEventoDaClinica(Admin admin, EventoSaude evento) {
        if (evento.getClinica() == null || !admin.getClinica().getIdClinica().equals(evento.getClinica().getIdClinica())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Evento de outra clínica");
        }
    }
}
