package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VacinaService {
    private final VacinaPetRepository vacinaRepository;
    private final TipoVacinaRepository tipoRepository;
    private final PetService petService;
    private final EventoSaudeRepository eventoRepository;
    private final ClinicaService clinicaService;

    public List<VacinaPet> listar(Long idPet) {
        return vacinaRepository.findByPet_IdPetOrderByDtAplicacaoDesc(idPet);
    }

    @Transactional
    public TipoVacina criarTipo(String nome, Integer periodicidadeDias, Long idClinica) {
        if (nome == null || nome.isBlank() || periodicidadeDias == null || periodicidadeDias <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome e periodicidade válida são obrigatórios");
        }
        Clinica clinica = clinicaService.buscarObrigatoria(idClinica);
        tipoRepository.findByNmTipoVacinaIgnoreCase(nome.trim()).ifPresent(existente -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tipo de vacina já cadastrado");
        });
        return tipoRepository.save(TipoVacina.builder().nmTipoVacina(nome.trim()).clinica(clinica)
                .nrPeriodicidadeDias(periodicidadeDias).ativo(true).build());
    }

    @Transactional
    public VacinaPet registrar(Long idPet, Long idTipoVacina, Long idEvento, LocalDate aplicacao,
                               LocalDate proximaDose, String comprovanteUrl) {
        Pet pet = petService.buscarPorId(idPet);
        TipoVacina tipo = tipoRepository.findById(idTipoVacina).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de vacina não encontrado"));
        if (aplicacao == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de aplicação é obrigatória");
        }
        EventoSaude evento = idEvento == null ? null : eventoRepository.findById(idEvento).orElseThrow(() ->
                                                                                                       new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento relacionado não encontrado"));
        if (evento != null && (evento.getPet() == null || !idPet.equals(evento.getPet().getIdPet()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O evento não pertence ao pet informado");
        }
        if (proximaDose == null) {
            proximaDose = aplicacao.plusDays(tipo.getNrPeriodicidadeDias());
        }
        return vacinaRepository.save(VacinaPet.builder()
                .pet(pet).tipoVacina(tipo).evento(evento).dtAplicacao(aplicacao)
                .dtProximaDose(proximaDose).comprovanteUrl(comprovanteUrl)
                .criadoEm(LocalDateTime.now()).build());
    }

    public StatusVacina status(VacinaPet vacina, LocalDate hoje) {
        if (vacina.getDtAplicacao().isAfter(hoje)) return StatusVacina.FUTURA;
        if (vacina.getDtProximaDose() == null || vacina.getDtProximaDose().isAfter(hoje.plusDays(30))) {
            return StatusVacina.EM_DIA;
        }
        return vacina.getDtProximaDose().isBefore(hoje) ? StatusVacina.ATRASADA : StatusVacina.VENCENDO;
    }

    /**
     * Status considerando reaplicações. Uma dose já substituída por outra mais recente do mesmo tipo
     * é apenas histórico: seu prazo vencido não pode gerar atraso. Nesse caso retorna EM_DIA.
     */
    public StatusVacina status(VacinaPet vacina, LocalDate hoje, Set<VacinaPet> substituidas) {
        return substituidas.contains(vacina) ? StatusVacina.EM_DIA : status(vacina, hoje);
    }

    /**
     * Doses já aplicadas que foram substituídas por uma aplicação mais recente do mesmo tipo de vacina
     * (mesmo pet). Doses futuras (dtAplicacao &gt; hoje) nunca substituem nem são substituídas.
     * O conjunto usa identidade de objeto, pois a lista recebida vem de uma única consulta.
     */
    public Set<VacinaPet> substituidas(List<VacinaPet> vacinas, LocalDate hoje) {
        Map<Long, VacinaPet> vigentePorTipo = new HashMap<>();
        for (VacinaPet v : vacinas) {
            if (v.getDtAplicacao().isAfter(hoje)) continue;
            Long tipo = v.getTipoVacina().getIdTipoVacina();
            VacinaPet atual = vigentePorTipo.get(tipo);
            if (atual == null || maisRecente(v, atual)) vigentePorTipo.put(tipo, v);
        }
        Set<VacinaPet> resultado = Collections.newSetFromMap(new IdentityHashMap<>());
        for (VacinaPet v : vacinas) {
            if (v.getDtAplicacao().isAfter(hoje)) continue;
            if (vigentePorTipo.get(v.getTipoVacina().getIdTipoVacina()) != v) resultado.add(v);
        }
        return resultado;
    }

    private boolean maisRecente(VacinaPet candidata, VacinaPet atual) {
        int porData = candidata.getDtAplicacao().compareTo(atual.getDtAplicacao());
        if (porData != 0) return porData > 0;
        Long a = candidata.getIdVacina();
        Long b = atual.getIdVacina();
        return a != null && (b == null || a > b);
    }
}