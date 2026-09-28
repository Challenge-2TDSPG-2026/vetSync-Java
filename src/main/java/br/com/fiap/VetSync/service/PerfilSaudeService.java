package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PerfilSaudeService {
    private final PerfilSaudeRepository perfilRepository;
    private final HistoricoPesoRepository pesoRepository;
    private final PetService petService;
    private final VeterinarioRepository veterinarioRepository;

    public PerfilSaude buscar(Long idPet) {
        Pet pet = petService.buscarPorId(idPet);
        return perfilRepository.findByPet_IdPet(idPet).orElseGet(() -> PerfilSaude.builder().pet(pet).build());
    }

    @Transactional
    public PerfilSaude atualizar(Long idPet, PerfilSaude dados, Long idVeterinario) {
        Pet pet = petService.buscarPorId(idPet);
        PerfilSaude perfil = perfilRepository.findByPet_IdPet(idPet)
                .orElseGet(() -> PerfilSaude.builder().pet(pet).build());
        if (dados.getPesoAtual() != null && (perfil.getPesoAtual() == null
                || perfil.getPesoAtual().compareTo(dados.getPesoAtual()) != 0)) {
            registrarPesoInterno(pet, dados.getPesoAtual(), LocalDate.now(), null);
        }
        perfil.setPesoAtual(dados.getPesoAtual());
        perfil.setPesoAtualizadoEm(dados.getPesoAtual() == null ? null : LocalDate.now());
        perfil.setAlergias(dados.getAlergias());
        perfil.setMedicamentosContinuos(dados.getMedicamentosContinuos());
        perfil.setRestricoesAlimentares(dados.getRestricoesAlimentares());
        perfil.setCondicoesPreExistentes(dados.getCondicoesPreExistentes());
        perfil.setObservacoesImportantes(dados.getObservacoesImportantes());
        perfil.setContatoEmergencia(dados.getContatoEmergencia());
        perfil.setVeterinarioPreferencial(idVeterinario == null ? null : veterinarioRepository.findById(idVeterinario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veterinário não encontrado")));
        pet.setNrPesoKg(dados.getPesoAtual());
        return perfilRepository.save(perfil);
    }

    @Transactional
    public HistoricoPeso registrarPeso(Long idPet, BigDecimal peso, LocalDate data, String observacao) {
        if (peso == null || peso.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Peso deve ser maior que zero");
        }
        Pet pet = petService.buscarPorId(idPet);
        HistoricoPeso registro = registrarPesoInterno(pet, peso, data == null ? LocalDate.now() : data, observacao);
        PerfilSaude perfil = perfilRepository.findByPet_IdPet(idPet)
                .orElseGet(() -> PerfilSaude.builder().pet(pet).build());
        perfil.setPesoAtual(peso);
        perfil.setPesoAtualizadoEm(registro.getDataMedicao());
        pet.setNrPesoKg(peso);
        perfilRepository.save(perfil);
        return registro;
    }

    private HistoricoPeso registrarPesoInterno(Pet pet, BigDecimal peso, LocalDate data, String observacao) {
        return pesoRepository.save(HistoricoPeso.builder().pet(pet).pesoKg(peso)
                .dataMedicao(data).observacao(observacao).build());
    }

    public List<HistoricoPeso> historicoPeso(Long idPet) {
        petService.buscarPorId(idPet);
        return pesoRepository.findByPet_IdPetOrderByDataMedicaoDesc(idPet);
    }
}
