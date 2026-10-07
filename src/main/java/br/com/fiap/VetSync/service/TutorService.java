package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TutorService {

    private final TutorRepository tutorRepository;

    public Tutor cadastrar(Tutor tutor) {
        tutorRepository.findByDsEmail(tutor.getDsEmail()).ifPresent(t -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe um tutor com o e-mail: " + tutor.getDsEmail());
        });
        return tutorRepository.save(tutor);
    }

    public Optional<Tutor> buscarPorEmail(String email) {
        return tutorRepository.findByDsEmail(email);
    }

    @Cacheable(value = "tutores", key = "#id")
    /** Igual a {@link #buscarPorId}, mas trava o tutor até o fim da transação (use dentro de @Transactional). */
    public Tutor buscarParaAtualizar(Long id) {
        return tutorRepository.findByIdParaAtualizar(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tutor não encontrado com id: " + id)
        );
    }

    public Tutor buscarPorId(Long id) {
        return tutorRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tutor não encontrado com id: " + id)
        );
    }

    public List<Tutor> listarTodos() {
        return tutorRepository.findAll();
    }

    @CacheEvict(value = "tutores", key = "#id")
    public Tutor atualizar(Long id, Tutor tutorAtualizado) {
        Tutor tutor = buscarPorId(id);
        tutor.setNmTutor(tutorAtualizado.getNmTutor());
        tutor.setNrTelefone(tutorAtualizado.getNrTelefone());
        tutor.setNrCep(tutorAtualizado.getNrCep());
        tutor.setDsLogradouro(tutorAtualizado.getDsLogradouro());
        tutor.setNrEndereco(tutorAtualizado.getNrEndereco());
        tutor.setDsComplemento(tutorAtualizado.getDsComplemento());
        tutor.setDsBairro(tutorAtualizado.getDsBairro());
        tutor.setNmCidade(tutorAtualizado.getNmCidade());
        tutor.setSgUf(tutorAtualizado.getSgUf());
        return tutorRepository.save(tutor);
    }

    @CacheEvict(value = "tutores", key = "#id")
    public void deletar(Long id) {
        Tutor tutor = buscarPorId(id);
        tutorRepository.delete(tutor);
    }
}