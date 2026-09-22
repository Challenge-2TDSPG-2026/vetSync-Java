package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Pet;
import br.com.fiap.VetSync.entity.PetAcesso;
import br.com.fiap.VetSync.entity.PermissaoPet;
import br.com.fiap.VetSync.entity.RelacaoPet;
import br.com.fiap.VetSync.entity.StatusAcessoPet;
import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.repository.PetAcessoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PetAcessoService {

    private final PetAcessoRepository petAcessoRepository;


    public PetAcesso conceder(Pet pet, Tutor tutor, RelacaoPet relacao, PermissaoPet permissao) {
        return petAcessoRepository.findByPet_IdPetAndTutor_IdTutor(pet.getIdPet(), tutor.getIdTutor())
                .map(acesso -> {
                    acesso.setDsRelacao(relacao);
                    acesso.setDsPermissao(permissao);
                    acesso.setDsStatus(StatusAcessoPet.ATIVO);
                    acesso.setDtConcedido(LocalDateTime.now());
                    acesso.setDtRevogado(null);
                    return petAcessoRepository.save(acesso);
                })
                .orElseGet(() -> petAcessoRepository.save(
                        PetAcesso.builder()
                                .pet(pet)
                                .tutor(tutor)
                                .dsRelacao(relacao)
                                .dsPermissao(permissao)
                                .dsStatus(StatusAcessoPet.ATIVO)
                                .dtConcedido(LocalDateTime.now())
                                .build()
                ));
    }

    public List<PetAcesso> listarAtivosDoPet(Long idPet) {
        return petAcessoRepository.findByPet_IdPetAndDsStatus(idPet, StatusAcessoPet.ATIVO);
    }

    public List<PetAcesso> listarAtivosDoTutor(Long idTutor) {
        return petAcessoRepository.findByTutor_IdTutorAndDsStatus(idTutor, StatusAcessoPet.ATIVO);
    }

    public void revogar(Long idPet, Long idAcesso) {
        PetAcesso acesso = petAcessoRepository.findById(idAcesso)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acesso não encontrado"));

        if (acesso.getPet() == null || !idPet.equals(acesso.getPet().getIdPet())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Acesso não encontrado para esse pet");
        }
        if (acesso.getDsStatus() == StatusAcessoPet.REVOGADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse acesso já está revogado");
        }
        acesso.setDsStatus(StatusAcessoPet.REVOGADO);
        acesso.setDtRevogado(LocalDateTime.now());
        petAcessoRepository.save(acesso);
    }
}