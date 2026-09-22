package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Especie;
import br.com.fiap.VetSync.entity.EspecieCategoria;
import br.com.fiap.VetSync.entity.Pet;
import br.com.fiap.VetSync.entity.PetAcesso;
import br.com.fiap.VetSync.entity.Raca;
import br.com.fiap.VetSync.entity.StatusAcessoPet;
import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.repository.EspecieRepository;
import br.com.fiap.VetSync.repository.PetAcessoRepository;
import br.com.fiap.VetSync.repository.PetRepository;
import br.com.fiap.VetSync.repository.RacaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final TutorService tutorService;
    private final EspecieRepository especieRepository;
    private final RacaRepository racaRepository;
    private final PetAcessoRepository petAcessoRepository;

    public Pet cadastrar(Pet pet, Long idTutor, EspecieCategoria categoria, String especieOutro, String nmRaca) {
        Tutor tutor = tutorService.buscarPorId(idTutor);
        Raca raca = resolverRaca(categoria, especieOutro, nmRaca);

        pet.setTutor(tutor);
        pet.setRaca(raca);
        return petRepository.save(pet);
    }

    private Raca resolverRaca(EspecieCategoria categoria, String especieOutro, String nmRaca) {
        String nmEspecie = resolverNomeEspecie(categoria, especieOutro);

        Especie especie = especieRepository.findByNmEspecieIgnoreCase(nmEspecie)
                .orElseGet(() -> especieRepository.save(
                        Especie.builder().nmEspecie(nmEspecie).build()
                ));

        return racaRepository.findByNmRacaIgnoreCaseAndEspecie_IdEspecie(nmRaca.trim(), especie.getIdEspecie())
                .orElseGet(() -> racaRepository.save(
                        Raca.builder().nmRaca(capitalizar(nmRaca)).especie(especie).build()
                ));
    }

    private String resolverNomeEspecie(EspecieCategoria categoria, String especieOutro) {
        if (categoria == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Espécie é obrigatória");
        }
        if (categoria != EspecieCategoria.OUTRO) {
            return categoria.getNomeOficial();
        }
        if (especieOutro == null || especieOutro.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe o nome da espécie quando escolher \"Outro\"");
        }
        return capitalizar(especieOutro);
    }

    private String capitalizar(String texto) {
        String t = texto.trim();
        if (t.isEmpty()) return t;
        return Character.toUpperCase(t.charAt(0)) + t.substring(1).toLowerCase();
    }

    public Pet buscarPorId(Long id) {
        return petRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet não encontrado com id: " + id)
        );
    }

    public List<Pet> listarPorTutor(Long idTutor) {
        return petRepository.findByTutor_IdTutor(idTutor);
    }

    /**
     * Pets próprios + pets com acesso ativo (cuidador/cônjuge) para o tutor autenticado.
     * Usado na listagem principal do app — {@link #listarPorTutor} continua existindo para
     * quem precisa só dos pets de que o tutor é proprietário (ex.: veterinário consultando um tutor).
     */
    public List<Pet> listarAcessiveis(Long idTutor) {
        List<Pet> proprios = petRepository.findByTutor_IdTutor(idTutor);
        List<Pet> compartilhados = petAcessoRepository.findByTutor_IdTutorAndDsStatus(idTutor, StatusAcessoPet.ATIVO)
                .stream()
                .map(PetAcesso::getPet)
                .toList();

        LinkedHashMap<Long, Pet> mesclados = new LinkedHashMap<>();
        proprios.forEach(pet -> mesclados.put(pet.getIdPet(), pet));
        compartilhados.forEach(pet -> mesclados.putIfAbsent(pet.getIdPet(), pet));
        return new ArrayList<>(mesclados.values());
    }

    public Pet atualizar(Long id, Pet petAtualizado, EspecieCategoria categoria, String especieOutro, String nmRaca) {
        Pet pet = buscarPorId(id);
        pet.setNmPet(petAtualizado.getNmPet());
        pet.setNrPesoKg(petAtualizado.getNrPesoKg());
        pet.setDsSexo(petAtualizado.getDsSexo());

        if (petAtualizado.getDtNascimento() != null) {
            pet.setDtNascimento(petAtualizado.getDtNascimento());
        }

        if (categoria != null && nmRaca != null && !nmRaca.isBlank()) {
            pet.setRaca(resolverRaca(categoria, especieOutro, nmRaca));
        }

        return petRepository.save(pet);
    }

    public void deletar(Long id) {
        Pet pet = buscarPorId(id);
        try {
            petRepository.delete(pet);
            petRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível remover este pet: existem eventos de saúde vinculados a ele. " +
                            "Remova ou cancele os eventos primeiro."
            );
        }
    }

    public int calcularIdade(Pet pet) {
        return Period.between(pet.getDtNascimento(), LocalDate.now()).getYears();
    }
}