package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.BloqueioAgendaEstetica;
import br.com.fiap.VetSync.entity.DisponibilidadeEstetica;
import br.com.fiap.VetSync.entity.ProfissionalEstetica;
import br.com.fiap.VetSync.repository.BloqueioAgendaEsteticaRepository;
import br.com.fiap.VetSync.repository.DisponibilidadeEsteticaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendaEsteticaService {

    private final DisponibilidadeEsteticaRepository disponibilidadeEsteticaRepository;
    private final BloqueioAgendaEsteticaRepository bloqueioAgendaEsteticaRepository;
    private final ProfissionalEsteticaService profissionalEsteticaService;

    public DisponibilidadeEstetica adicionarDisponibilidade(Long idProfissionalEstetica, Integer nrDiaSemana, String hrInicio, String hrFim) {
        if (nrDiaSemana == null || nrDiaSemana < 1 || nrDiaSemana > 7) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dia da semana deve ser entre 1 (segunda) e 7 (domingo)");
        }
        if (hrInicio == null || hrFim == null || hrInicio.compareTo(hrFim) >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horário de início deve ser antes do horário de fim");
        }
        ProfissionalEstetica prof = profissionalEsteticaService.buscarPorId(idProfissionalEstetica);
        DisponibilidadeEstetica disponibilidade = DisponibilidadeEstetica.builder()
                .profissionalEstetica(prof)
                .nrDiaSemana(nrDiaSemana)
                .hrInicio(hrInicio)
                .hrFim(hrFim)
                .build();
        return disponibilidadeEsteticaRepository.save(disponibilidade);
    }

    public List<DisponibilidadeEstetica> listarDisponibilidade(Long idProfissionalEstetica) {
        return disponibilidadeEsteticaRepository.findByProfissionalEstetica_IdProfissionalEstetica(idProfissionalEstetica);
    }

    public void removerDisponibilidade(Long idProfissionalEstetica, Long idDisponibilidade) {
        DisponibilidadeEstetica disponibilidade = disponibilidadeEsteticaRepository.findById(idDisponibilidade).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Disponibilidade não encontrada")
        );
        verificarPertence(disponibilidade.getProfissionalEstetica().getIdProfissionalEstetica(), idProfissionalEstetica);
        disponibilidadeEsteticaRepository.delete(disponibilidade);
    }

    public BloqueioAgendaEstetica adicionarBloqueio(Long idProfissionalEstetica, LocalDate dtInicio, LocalDate dtFim, String dsMotivo) {
        if (dtInicio == null || dtFim == null || dtFim.isBefore(dtInicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de fim deve ser igual ou depois da data de início");
        }
        ProfissionalEstetica prof = profissionalEsteticaService.buscarPorId(idProfissionalEstetica);
        BloqueioAgendaEstetica bloqueio = BloqueioAgendaEstetica.builder()
                .profissionalEstetica(prof)
                .dtInicio(dtInicio)
                .dtFim(dtFim)
                .dsMotivo(dsMotivo)
                .build();
        return bloqueioAgendaEsteticaRepository.save(bloqueio);
    }

    public List<BloqueioAgendaEstetica> listarBloqueios(Long idProfissionalEstetica) {
        return bloqueioAgendaEsteticaRepository.findByProfissionalEstetica_IdProfissionalEstetica(idProfissionalEstetica);
    }

    public void removerBloqueio(Long idProfissionalEstetica, Long idBloqueio) {
        BloqueioAgendaEstetica bloqueio = bloqueioAgendaEsteticaRepository.findById(idBloqueio).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bloqueio não encontrado")
        );
        verificarPertence(bloqueio.getProfissionalEstetica().getIdProfissionalEstetica(), idProfissionalEstetica);
        bloqueioAgendaEsteticaRepository.delete(bloqueio);
    }

    private void verificarPertence(Long idDoRegistro, Long idDaUrl) {
        if (!idDoRegistro.equals(idDaUrl)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não pertence a este profissional");
        }
    }

    public boolean estaBloqueado(Long idProfissionalEstetica, LocalDate data) {
        return bloqueioAgendaEsteticaRepository.findByProfissionalEstetica_IdProfissionalEstetica(idProfissionalEstetica).stream()
                .anyMatch(b -> !data.isBefore(b.getDtInicio()) && !data.isAfter(b.getDtFim()));
    }
}