package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.PlanoItemRepository;
import br.com.fiap.VetSync.repository.PlanoTratamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Reflete no plano de tratamento o que aconteceu com o evento vinculado a um item do plano. */
@Service
@RequiredArgsConstructor
public class PlanoEventoSincronizador {

    private final PlanoItemRepository planoItemRepository;
    private final PlanoTratamentoRepository planoTratamentoRepository;
    private final PontosService pontosService;

    public void aoConcluirEvento(EventoSaude evento) {
        planoItemRepository.findByEvento_IdEvento(evento.getIdEvento()).ifPresent(item -> {
            item.setDsStatus(StatusPlanoItem.CONCLUIDO);
            planoItemRepository.save(item);

            PlanoTratamento plano = item.getPlano();
            if (plano.getDsStatus() != StatusPlanoTratamento.EM_ANDAMENTO) {
                return; // plano já estava QUEBRADO ou CONCLUIDO, não há mais bônus a considerar
            }

            List<PlanoItem> itens = planoItemRepository.findByPlano_IdPlanoOrderByNrOrdemAsc(plano.getIdPlano());
            boolean todosAnterioresConcluidos = itens.stream()
                    .filter(i -> i.getNrOrdem() < item.getNrOrdem())
                    .allMatch(i -> i.getDsStatus() == StatusPlanoItem.CONCLUIDO);

            if (!todosAnterioresConcluidos) {
                plano.setDsStatus(StatusPlanoTratamento.QUEBRADO);
                planoTratamentoRepository.save(plano);
                return;
            }

            boolean ehUltimoItem = itens.stream().mapToInt(PlanoItem::getNrOrdem).max().orElse(0) == item.getNrOrdem();
            if (ehUltimoItem) {
                plano.setDsStatus(StatusPlanoTratamento.CONCLUIDO);
                planoTratamentoRepository.save(plano);
                pontosService.lancarBonusPendente(plano);
            }
        });
    }

    public void aoCancelarEvento(EventoSaude evento) {
        planoItemRepository.findByEvento_IdEvento(evento.getIdEvento()).ifPresent(item -> {
            item.setDsStatus(StatusPlanoItem.QUEBRADO);
            planoItemRepository.save(item);

            PlanoTratamento plano = item.getPlano();
            if (plano.getDsStatus() == StatusPlanoTratamento.EM_ANDAMENTO) {
                plano.setDsStatus(StatusPlanoTratamento.QUEBRADO);
                planoTratamentoRepository.save(plano);
            }
        });
    }
}
