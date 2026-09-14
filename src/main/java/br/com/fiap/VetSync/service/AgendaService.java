package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.BloqueioAgenda;
import br.com.fiap.VetSync.entity.Disponibilidade;
import br.com.fiap.VetSync.repository.BloqueioAgendaRepository;
import br.com.fiap.VetSync.repository.DisponibilidadeRepository;
import lombok.RequiredArgsConstructor;
import br.com.fiap.VetSync.entity.Veterinario;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendaService {

    private final DisponibilidadeRepository disponibilidadeRepository;
    private final BloqueioAgendaRepository bloqueioAgendaRepository;
    private final VeterinarioService veterinarioService;



    public Disponibilidade adicionarDisponibilidade(Long idVeterinario, Integer nrDiaSemana, String hrInicio, String hrFim) {
        if (nrDiaSemana == null || nrDiaSemana < 1 || nrDiaSemana > 7) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dia da semana deve ser entre 1 (segunda) e 7 (domingo)");
        }
        if (hrInicio == null || hrFim == null || hrInicio.compareTo(hrFim) >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horário de início deve ser antes do horário de fim");
        }
        Veterinario vet = veterinarioService.buscarPorId(idVeterinario);
        Disponibilidade disponibilidade = Disponibilidade.builder()
                .veterinario(vet)
                .nrDiaSemana(nrDiaSemana)
                .hrInicio(hrInicio)
                .hrFim(hrFim)
                .build();
        return disponibilidadeRepository.save(disponibilidade);
    }

    public List<Disponibilidade> listarDisponibilidade(Long idVeterinario) {
        return disponibilidadeRepository.findByVeterinario_IdVeterinario(idVeterinario);
    }

    public void removerDisponibilidade(Long idVeterinario, Long idDisponibilidade) {
        Disponibilidade disponibilidade = disponibilidadeRepository.findById(idDisponibilidade).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Disponibilidade não encontrada")
        );
        verificarPertenceAoVeterinario(disponibilidade.getVeterinario().getIdVeterinario(), idVeterinario);
        disponibilidadeRepository.delete(disponibilidade);
    }


    public BloqueioAgenda adicionarBloqueio(Long idVeterinario, LocalDate dtInicio, LocalDate dtFim, String dsMotivo) {
        if (dtInicio == null || dtFim == null || dtFim.isBefore(dtInicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de fim deve ser igual ou depois da data de início");
        }
        Veterinario vet = veterinarioService.buscarPorId(idVeterinario);
        BloqueioAgenda bloqueio = BloqueioAgenda.builder()
                .veterinario(vet)
                .dtInicio(dtInicio)
                .dtFim(dtFim)
                .dsMotivo(dsMotivo)
                .build();
        return bloqueioAgendaRepository.save(bloqueio);
    }

    public List<BloqueioAgenda> listarBloqueios(Long idVeterinario) {
        return bloqueioAgendaRepository.findByVeterinario_IdVeterinario(idVeterinario);
    }

    public void removerBloqueio(Long idVeterinario, Long idBloqueio) {
        BloqueioAgenda bloqueio = bloqueioAgendaRepository.findById(idBloqueio).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bloqueio não encontrado")
        );
        verificarPertenceAoVeterinario(bloqueio.getVeterinario().getIdVeterinario(), idVeterinario);
        bloqueioAgendaRepository.delete(bloqueio);
    }

    private void verificarPertenceAoVeterinario(Long idVeterinarioDoRegistro, Long idVeterinarioDaUrl) {
        if (!idVeterinarioDoRegistro.equals(idVeterinarioDaUrl)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não pertence a este veterinário");
        }
    }


    public boolean estaDentroDaDisponibilidade(Long idVeterinario, LocalDate data, String hora) {
        int diaSemana = data.getDayOfWeek().getValue(); // 1=segunda ... 7=domingo, igual ao nrDiaSemana
        return disponibilidadeRepository.findByVeterinario_IdVeterinario(idVeterinario).stream()
                .filter(d -> d.getNrDiaSemana().equals(diaSemana))
                .anyMatch(d -> hora.compareTo(d.getHrInicio()) >= 0 && hora.compareTo(d.getHrFim()) < 0);
    }


    public boolean estaBloqueado(Long idVeterinario, LocalDate data) {
        return bloqueioAgendaRepository.findByVeterinario_IdVeterinario(idVeterinario).stream()
                .anyMatch(b -> !data.isBefore(b.getDtInicio()) && !data.isAfter(b.getDtFim()));
    }
}