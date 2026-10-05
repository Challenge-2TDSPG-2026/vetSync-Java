package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.PetRepository;
import br.com.fiap.VetSync.repository.PlanoItemRepository;
import br.com.fiap.VetSync.repository.PlanoTratamentoRepository;
import br.com.fiap.VetSync.repository.TipoEventoRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;


@Service
@RequiredArgsConstructor
public class PlanoTratamentoService {

    private final PlanoTratamentoRepository planoTratamentoRepository;
    private final PlanoItemRepository planoItemRepository;
    private final PetRepository petRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final TipoEventoRepository tipoEventoRepository;
    private final EventoService eventoService;
    private final VinculoClinicaService vinculoClinicaService;

    private static final int MINIMO_ITENS = 2;

    public PlanoTratamento criar(Long idPet, Long idVeterinarioAutenticado, Integer nrPontosBonus, List<Long> idsTipoEvento) {
        if (idsTipoEvento == null || idsTipoEvento.size() < MINIMO_ITENS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Um plano de tratamento precisa de pelo menos " + MINIMO_ITENS + " itens em sequência");
        }
        Pet pet = petRepository.findById(idPet).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet não encontrado: " + idPet));
        Veterinario vet = veterinarioRepository.findById(idVeterinarioAutenticado).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veterinário não encontrado"));

        // O plano só pode ser feito na clínica em que o tutor está vinculado, e fica na clínica do veterinário
        // que o está criando (que precisa ser essa mesma clínica).
        vinculoClinicaService.exigirClinicaAtivaDoTutor(pet.getTutor().getIdTutor(), vet.getClinica().getIdClinica());

        PlanoTratamento plano = PlanoTratamento.builder()
                .pet(pet)
                .veterinario(vet)
                .clinica(vet.getClinica())
                .nrPontosBonus(nrPontosBonus != null ? nrPontosBonus : 0)
                .dsStatus(StatusPlanoTratamento.EM_ANDAMENTO)
                .build();
        plano = planoTratamentoRepository.save(plano);

        int ordem = 1;
        for (Long idTipoEvento : idsTipoEvento) {
            TipoEvento tipoEvento = tipoEventoRepository.findById(idTipoEvento).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de evento não encontrado: " + idTipoEvento));
            PlanoItem item = PlanoItem.builder()
                    .plano(plano)
                    .nrOrdem(ordem++)
                    .tipoEvento(tipoEvento)
                    .dsStatus(StatusPlanoItem.PENDENTE)
                    .build();
            planoItemRepository.save(item);
        }
        return plano;
    }

    public PlanoTratamento buscarPorId(Long id) {
        return planoTratamentoRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano de tratamento não encontrado com id: " + id));
    }

    public List<PlanoItem> listarItens(Long idPlano) {
        return planoItemRepository.findByPlano_IdPlanoOrderByNrOrdemAsc(idPlano);
    }

    public List<PlanoTratamento> listarParaTutor(String email) {
        return planoTratamentoRepository.findVisiveisParaTutor(email);
    }

    public List<PlanoTratamento> listarParaVeterinario(String email) {
        return planoTratamentoRepository.findByVeterinario_DsEmailOrderByDtCriacaoDesc(email);
    }

    public PlanoItem buscarItemPorId(Long id) {
        return planoItemRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item de plano não encontrado com id: " + id));
    }


    public EventoSaude agendarItem(Long idItem, Long idVeterinario, LocalDate dtEvento, String hrEvento, String dsObservacao) {
        PlanoItem item = buscarItemPorId(idItem);
        if (item.getDsStatus() != StatusPlanoItem.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse item já está " + item.getDsStatus());
        }
        PlanoTratamento plano = item.getPlano();
        if (plano.getDsStatus() != StatusPlanoTratamento.EM_ANDAMENTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse plano de tratamento não está mais em andamento");
        }
        exigirVeterinarioDaClinicaDoPlano(plano, idVeterinario);

        EventoSaude evento = EventoSaude.builder()
                .dtEvento(dtEvento)
                .hrEvento(hrEvento)
                .dsObservacao(dsObservacao)
                .build();
        EventoSaude agendado = eventoService.agendar(
                evento, plano.getPet().getIdPet(), item.getTipoEvento().getIdTipoEvento(), idVeterinario);

        item.setEvento(agendado);
        item.setDsStatus(StatusPlanoItem.AGENDADO);
        planoItemRepository.save(item);
        return agendado;
    }

    /** Itens do plano só podem ser agendados por veterinários da clínica onde o plano foi criado. */
    private void exigirVeterinarioDaClinicaDoPlano(PlanoTratamento plano, Long idVeterinario) {
        if (plano.getClinica() == null) {
            return; // plano legado, criado antes do escopo por clínica
        }
        Veterinario vet = veterinarioRepository.findById(idVeterinario).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veterinário não encontrado"));
        if (!plano.getClinica().getIdClinica().equals(vet.getClinica().getIdClinica())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Os itens do plano só podem ser agendados na clínica em que o plano foi criado");
        }
    }
}