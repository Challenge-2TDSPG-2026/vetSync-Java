package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Pet;
import br.com.fiap.VetSync.entity.ProfissionalEstetica;
import br.com.fiap.VetSync.entity.Veterinario;
import br.com.fiap.VetSync.entity.VinculoTutorClinica;
import br.com.fiap.VetSync.repository.PetRepository;
import br.com.fiap.VetSync.repository.ProfissionalEsteticaRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import br.com.fiap.VetSync.repository.VinculoTutorClinicaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Consultas sobre quem participa de uma clinica: profissionais, tutores vinculados e seus pets. */
@Service
@RequiredArgsConstructor
public class ClinicaParticipantesService {

    private final VeterinarioRepository veterinarioRepository;
    private final ProfissionalEsteticaRepository esteticaRepository;
    private final VinculoTutorClinicaRepository vinculoRepository;
    private final PetRepository petRepository;

    public List<Veterinario> listarVeterinarios(Long idClinica) {
        return veterinarioRepository.findByClinica_IdClinicaOrderByNmVeterinario(idClinica);
    }

    public List<ProfissionalEstetica> listarProfissionaisEstetica(Long idClinica) {
        return esteticaRepository.findByClinica_IdClinicaOrderByNmProfissionalEstetica(idClinica);
    }

    public List<VinculoTutorClinica> listarVinculosAtivos(Long idClinica) {
        return vinculoRepository.findByClinica_IdClinicaAndDtEncerramentoIsNull(idClinica);
    }

    public List<Pet> listarPetsDaClinica(Long idClinica) {
        return listarVinculosAtivos(idClinica).stream()
                .flatMap(v -> petRepository.findByTutor_IdTutor(v.getTutor().getIdTutor()).stream())
                .toList();
    }
}
