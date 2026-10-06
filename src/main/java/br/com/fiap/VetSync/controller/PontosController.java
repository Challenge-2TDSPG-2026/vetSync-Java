package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.security.PerfilUtils;
import br.com.fiap.VetSync.service.PontosService;
import br.com.fiap.VetSync.service.PontosService.SaldoPontos;
import br.com.fiap.VetSync.service.PontosService.SaldoTutorClinica;
import br.com.fiap.VetSync.service.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/pontos")
@RequiredArgsConstructor
@Tag(name = "Pontos", description = "Lançamentos de pontos por clínica (evento concluído ou bônus de plano). "
        + "Estados: PENDENTE, LIBERADO, BLOQUEADO e EXPIRADO. Só LIBERADO e dentro da validade é resgatável.")
public class PontosController {

    private final PontosService pontosService;
    private final AdminRepository adminRepository;
    private final TutorService tutorService;

    public record LancamentoPontosResponse(
            Long idLancamento,
            String status,
            String origem,
            Integer nrPontos,
            LocalDate dtLancamento,
            LocalDate dtLiberacao,
            LocalDate dtValidade,
            LocalDate dtBloqueio,
            String motivoBloqueio,
            boolean resgatavel,
            Long idEvento,
            String nmTipoEvento,
            String dsAtendimento,
            LocalDate dtAtendimento,
            Long idPlano,
            Long idPet,
            String nmPet,
            Long idTutor,
            String nmTutor,
            Long idClinica,
            String nmClinica
    ) {}

    public record SaldoResponse(
            Long idTutor,
            String nmTutor,
            Long idClinica,
            String nmClinica,
            int pontosPendentes,
            int pontosLiberados,
            int pontosBloqueados,
            int pontosExpirados,
            int pontosResgatados,
            int pontosReservados,
            int saldoDisponivel
    ) {}

    public record BloquearRequest(
            @NotBlank(message = "Motivo do bloqueio é obrigatório")
            @Size(max = 300, message = "Motivo deve ter no máximo 300 caracteres")
            String motivo
    ) {}

    private LancamentoPontosResponse toResponse(LancamentoPontos l) {
        LocalDate hoje = LocalDate.now();
        var evento = l.getEvento();
        var plano = l.getPlanoTratamento();
        var pet = l.petOrigem();
        var tutor = l.tutorOrigem();
        var clinica = l.getClinica();

        String nmTipoEvento = evento != null && evento.getTipoEvento() != null ? evento.getTipoEvento().getNmTipoEvento() : null;
        String dsAtendimento = evento != null ? nmTipoEvento
                : (plano != null ? "Bônus de plano de tratamento #" + plano.getIdPlano() : null);

        return new LancamentoPontosResponse(
                l.getIdLancamento(),
                l.statusEfetivo(hoje).name(),
                evento != null ? "EVENTO" : "BONUS_PLANO",
                l.getNrPontos(),
                l.getDtLancamento(),
                l.getDtLiberacao(),
                l.getDtValidade(),
                l.getDtBloqueio(),
                l.getDsMotivoBloqueio(),
                l.resgatavel(hoje),
                evento != null ? evento.getIdEvento() : null,
                nmTipoEvento,
                dsAtendimento,
                evento != null ? evento.getDtEvento() : null,
                plano != null ? plano.getIdPlano() : null,
                pet != null ? pet.getIdPet() : null,
                pet != null ? pet.getNmPet() : null,
                tutor != null ? tutor.getIdTutor() : null,
                tutor != null ? tutor.getNmTutor() : null,
                clinica != null ? clinica.getIdClinica() : null,
                clinica != null ? clinica.getNmClinica() : null
        );
    }

    private SaldoResponse toResponse(SaldoTutorClinica s) {
        SaldoPontos p = s.saldo();
        return new SaldoResponse(s.tutor().getIdTutor(), s.tutor().getNmTutor(),
                s.clinica().getIdClinica(), s.clinica().getNmClinica(),
                p.pontosPendentes(), p.pontosLiberados(), p.pontosBloqueados(), p.pontosExpirados(),
                p.pontosResgatados(), p.pontosReservados(), p.saldoDisponivel());
    }

    private Long idAdminAutenticado(Authentication authentication) {
        return adminRepository.findByDsEmail(authentication.getName())
                .map(Admin::getIdAdmin)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin autenticado não encontrado"));
    }

    private Long idTutorAutenticado(Authentication authentication) {
        return tutorService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor autenticado não encontrado"))
                .getIdTutor();
    }

    @GetMapping
    @Operation(summary = "Listar lançamentos de pontos",
            description = "Tutor vê os próprios; admin vê todos. Filtros opcionais: ?idClinica=, ?status= "
                    + "(PENDENTE, LIBERADO, BLOQUEADO, EXPIRADO) e, só para admin, ?idTutor=. "
                    + "Cada item traz clínica de origem, tutor, atendimento que gerou os pontos, data de liberação, "
                    + "validade e se é resgatável.")
    public List<LancamentoPontosResponse> listar(Authentication authentication,
                                                 @RequestParam(value = "idClinica", required = false) Long idClinica,
                                                 @RequestParam(value = "status", required = false) StatusLancamentoPontos status,
                                                 @RequestParam(value = "idTutor", required = false) Long idTutor) {
        List<LancamentoPontos> lancamentos = PerfilUtils.isAdmin(authentication)
                ? pontosService.listar(idClinica, status, idTutor)
                : pontosService.listarParaTutor(authentication.getName(), idClinica, status);
        return lancamentos.stream().map(this::toResponse).toList();
    }

    @GetMapping("/saldos")
    @PreAuthorize("hasAnyRole('ADMIN','TUTOR')")
    @Operation(summary = "Saldo de pontos por tutor e clínica",
            description = "Uma linha por par tutor/clínica; pontos de clínicas diferentes nunca são somados. "
                    + "O saldo disponível exclui pendentes, bloqueados, expirados e pontos reservados por resgates pendentes. "
                    + "Filtros: ?idClinica= e, só para admin, ?idTutor=.")
    public List<SaldoResponse> saldos(Authentication authentication,
                                      @RequestParam(value = "idClinica", required = false) Long idClinica,
                                      @RequestParam(value = "idTutor", required = false) Long idTutor) {
        Long tutorFiltro = PerfilUtils.isAdmin(authentication) ? idTutor : idTutorAutenticado(authentication);
        return pontosService.listarSaldos(idClinica, tutorFiltro).stream().map(this::toResponse).toList();
    }

    @PatchMapping("/{id}/liberar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin libera um lançamento de pontos pendente da clínica informada",
            description = "Exige ?idClinica= e ela deve ser a clínica do lançamento. A validade passa a contar da liberação. "
                    + "Só depois disso os pontos entram no saldo do tutor naquela clínica.")
    public LancamentoPontosResponse liberar(@PathVariable Long id,
                                            @RequestParam("idClinica") Long idClinica,
                                            Authentication authentication) {
        return toResponse(pontosService.liberar(id, idClinica, idAdminAutenticado(authentication)));
    }

    @PatchMapping("/{id}/bloquear")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin bloqueia um lançamento pendente ou liberado da clínica informada",
            description = "Exige ?idClinica= e o motivo. Pontos bloqueados não entram no saldo nem são resgatáveis.")
    public LancamentoPontosResponse bloquear(@PathVariable Long id,
                                             @RequestParam("idClinica") Long idClinica,
                                             @Valid @RequestBody BloquearRequest request,
                                             Authentication authentication) {
        return toResponse(pontosService.bloquear(id, idClinica, request.motivo(), idAdminAutenticado(authentication)));
    }

    @PatchMapping("/{id}/desbloquear")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin desbloqueia um lançamento da clínica informada",
            description = "Volta a PENDENTE se nunca foi liberado, ou a LIBERADO (mantendo a validade original; pode virar EXPIRADO).")
    public LancamentoPontosResponse desbloquear(@PathVariable Long id,
                                                @RequestParam("idClinica") Long idClinica,
                                                Authentication authentication) {
        return toResponse(pontosService.desbloquear(id, idClinica, idAdminAutenticado(authentication)));
    }
}