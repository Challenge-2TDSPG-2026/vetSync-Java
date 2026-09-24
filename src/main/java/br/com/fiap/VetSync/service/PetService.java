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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final TutorService tutorService;
    private final EspecieRepository especieRepository;
    private final RacaRepository racaRepository;
    private final PetAcessoRepository petAcessoRepository;

    private static final long TAMANHO_MAXIMO_FOTO_BYTES = 5L * 1024 * 1024; // 5MB
    private static final Set<String> TIPOS_FOTO_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");

    /** O número do pet é o próprio id e aparece com 4 dígitos, então o limite é 9999. */
    public static final long NUMERO_MAXIMO_PET = 9999L;

    @Transactional
    public Pet cadastrar(Pet pet, Long idTutor, EspecieCategoria categoria, String especieOutro, String nmRaca) {
        Tutor tutor = tutorService.buscarPorId(idTutor);
        Raca raca = resolverRaca(categoria, especieOutro, nmRaca);

        pet.setTutor(tutor);
        pet.setRaca(raca);
        Pet salvo = petRepository.save(pet);
        if (salvo.getIdPet() != null && salvo.getIdPet() > NUMERO_MAXIMO_PET) {
            // a exceção desfaz o INSERT (rollback) e nenhum pet com número de 5 dígitos é criado
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Limite de " + NUMERO_MAXIMO_PET + " pets cadastrados atingido (número do pet tem até 4 dígitos)");
        }
        return salvo;
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

    /** NOVO: define ou substitui a foto do pet (perfil e carteirinha). */
    public Pet atualizarFoto(Long idPet, MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie o arquivo da foto no campo 'foto'");
        }
        if (foto.getSize() > TAMANHO_MAXIMO_FOTO_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A foto deve ter no máximo 5MB");
        }
        String contentType = foto.getContentType();
        if (contentType == null || !TIPOS_FOTO_PERMITIDOS.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Formato de imagem inválido. Envie um arquivo JPEG, PNG ou WEBP");
        }

        byte[] bytes;
        try {
            bytes = foto.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo de imagem enviado");
        }
        // O Content-Type vem do cliente e pode ser falsificado: confere também a assinatura real do arquivo.
        String tipoReal = detectarTipoImagem(bytes);
        if (tipoReal == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O arquivo enviado não é uma imagem JPEG, PNG ou WEBP válida");
        }

        Pet pet = buscarPorId(idPet);
        pet.setDsFoto(bytes);
        pet.setDsFotoTipo(tipoReal);
        return petRepository.save(pet);
    }

    /** NOVO: remove a foto do pet. */
    public Pet removerFoto(Long idPet) {
        Pet pet = buscarPorId(idPet);
        pet.setDsFoto(null);
        pet.setDsFotoTipo(null);
        return petRepository.save(pet);
    }

    private String detectarTipoImagem(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
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