package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.EventoSaude;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import br.com.fiap.VetSync.repository.ServicoClinicaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/** Consultas de agenda usadas pelo painel da clinica. */
@Service
@RequiredArgsConstructor
public class ClinicaAgendaConsultaService {

    private final EventoSaudeRepository eventoRepository;
    private final ServicoClinicaRepository servicoRepository;

    public List<EventoSaude> listarEventosDoDia(Long idClinica, LocalDate data) {
        return eventoRepository.findByClinica_IdClinicaAndDtEvento(idClinica, data);
    }

    public void exigirServicoDaClinica(Long idServico, Long idClinica) {
        servicoRepository.findByIdServicoClinicaAndClinica_IdClinica(idServico, idClinica)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Serviço de outra clínica"));
    }
}
