package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.AdminRepository;
import br.com.fiap.VetSync.repository.LancamentoPontosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PontosService {

    private final LancamentoPontosRepository lancamentoPontosRepository;
    private final AdminRepository adminRepository;

    /**
     * Chamado quando um evento vira CONCLUIDO. Cria o lançamento PENDENTE
     * com os pontos do tipo de evento — não credita nada ainda, só entra na
     * fila de liberação do admin.
     */
    public LancamentoPontos lancarPendente(EventoSaude evento) {
        int pontos = evento.getTipoEvento() != null && evento.getTipoEvento().getNrPontos() != null
                ? evento.getTipoEvento().getNrPontos() : 0;
        LancamentoPontos lancamento = LancamentoPontos.builder()
                .evento(evento)
                .clinica(evento.getClinica())
                .nrPontos(pontos)
                .dsStatus(StatusLancamentoPontos.PENDENTE)
                .build();
        return lancamentoPontosRepository.save(lancamento);
    }


    public LancamentoPontos lancarBonusPendente(PlanoTratamento plano) {
        int bonus = plano.getNrPontosBonus() != null ? plano.getNrPontosBonus() : 0;
        LancamentoPontos lancamento = LancamentoPontos.builder()
                .planoTratamento(plano)
                .clinica(plano.getClinica())
                .nrPontos(bonus)
                .dsStatus(StatusLancamentoPontos.PENDENTE)
                .build();
        return lancamentoPontosRepository.save(lancamento);
    }

    public LancamentoPontos buscarPorId(Long id) {
        return lancamentoPontosRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lançamento de pontos não encontrado com id: " + id));
    }

    public List<LancamentoPontos> listarPendentes() {
        return lancamentoPontosRepository.findByDsStatusOrderByDtLancamentoAsc(StatusLancamentoPontos.PENDENTE);
    }


    public List<LancamentoPontos> listarParaTutor(String email) {
        List<LancamentoPontos> lancamentos = new ArrayList<>();
        lancamentos.addAll(lancamentoPontosRepository.findByEvento_Pet_Tutor_DsEmailOrderByDtLancamentoDesc(email));
        lancamentos.addAll(lancamentoPontosRepository.findByPlanoTratamento_Pet_Tutor_DsEmailOrderByDtLancamentoDesc(email));
        lancamentos.sort(Comparator.comparing(LancamentoPontos::getDtLancamento).reversed());
        return lancamentos;
    }


    public LancamentoPontos liberar(Long idLancamento, Long idAdmin) {
        LancamentoPontos lancamento = buscarPorId(idLancamento);
        if (lancamento.getDsStatus() != StatusLancamentoPontos.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse lançamento já está " + lancamento.getDsStatus());
        }
        Admin admin = adminRepository.findById(idAdmin).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin não encontrado"));

        lancamento.setAdminValidador(admin);
        lancamento.setDsStatus(StatusLancamentoPontos.LIBERADO);
        return lancamentoPontosRepository.save(lancamento);
    }


    /** Pontos liberados do tutor SOMENTE na clínica informada (pontos não acompanham o tutor entre clínicas). */
    public int calcularPontosLiberados(Long idTutor, Long idClinica) {
        int deEventos = lancamentoPontosRepository
                .findByEvento_Pet_Tutor_IdTutorAndClinica_IdClinicaAndDsStatus(idTutor, idClinica, StatusLancamentoPontos.LIBERADO)
                .stream().mapToInt(LancamentoPontos::getNrPontos).sum();

        int deBonusPlano = lancamentoPontosRepository
                .findByPlanoTratamento_Pet_Tutor_IdTutorAndClinica_IdClinicaAndDsStatus(idTutor, idClinica, StatusLancamentoPontos.LIBERADO)
                .stream().mapToInt(LancamentoPontos::getNrPontos).sum();

        return deEventos + deBonusPlano;
    }
}