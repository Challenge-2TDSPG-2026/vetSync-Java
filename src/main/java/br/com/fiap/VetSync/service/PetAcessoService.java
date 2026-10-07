package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.entity.Pet;
import br.com.fiap.VetSync.entity.PetAcesso;
import br.com.fiap.VetSync.entity.PermissaoPet;
import br.com.fiap.VetSync.entity.RelacaoPet;
import br.com.fiap.VetSync.entity.StatusAcessoPet;
import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.entity.VinculoTutorClinica;
import br.com.fiap.VetSync.repository.PetAcessoRepository;
import br.com.fiap.VetSync.repository.VinculoTutorClinicaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PetAcessoService {

    private final PetAcessoRepository petAcessoRepository;
    private final VinculoTutorClinicaRepository vinculoRepository;
    @Autowired(required = false)
    private AuditoriaService auditoriaService;


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
        if (auditoriaService != null) {
            auditoriaService.registrarAcao(AuditoriaTipos.ACESSO_PET, acesso.getIdAcesso(), "ACESSO_REVOGADO",
                    clinicaDoPet(acesso.getPet()), StatusAcessoPet.ATIVO.name(), StatusAcessoPet.REVOGADO.name(),
                    emailDe(acesso.getTutor()), "TUTOR");
        }
    }

    public void revogarDoResponsavel(Long idTutorProprietario, Long idTutorResponsavel) {
        List<PetAcesso> acessos = petAcessoRepository
                .findByPet_Tutor_IdTutorAndTutor_IdTutorAndDsStatus(
                        idTutorProprietario, idTutorResponsavel, StatusAcessoPet.ATIVO);
        LocalDateTime agora = LocalDateTime.now();
        acessos.forEach(acesso -> {
            acesso.setDsStatus(StatusAcessoPet.REVOGADO);
            acesso.setDtRevogado(agora);
        });
        petAcessoRepository.saveAll(acessos);
    }

    public PetAcesso atualizar(Long idPet, Long idAcesso, RelacaoPet relacao, PermissaoPet permissao) {
        PetAcesso acesso = petAcessoRepository.findById(idAcesso)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acesso não encontrado"));
        if (acesso.getPet() == null || !idPet.equals(acesso.getPet().getIdPet())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Acesso não encontrado para esse pet");
        }
        if (acesso.getDsStatus() == StatusAcessoPet.REVOGADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse acesso já está revogado");
        }
        if (relacao != null) acesso.setDsRelacao(relacao);
        if (permissao != null) acesso.setDsPermissao(permissao);
        PetAcesso atualizado = petAcessoRepository.save(acesso);
        if (auditoriaService != null) {
            auditoriaService.registrarAcao(AuditoriaTipos.ACESSO_PET, atualizado.getIdAcesso(), "ACESSO_ATUALIZADO",
                    clinicaDoPet(atualizado.getPet()), null, atualizado.getDsPermissao().name(),
                    emailDe(atualizado.getTutor()), "TUTOR");
        }
        return atualizado;
    }

    /** Clínica em cujo contexto o acesso foi mexido: a do vínculo ativo do tutor dono do pet (nula se não houver). */
    private Clinica clinicaDoPet(Pet pet) {
        if (pet == null || pet.getTutor() == null || vinculoRepository == null) return null;
        return vinculoRepository.findByTutor_IdTutorAndDtEncerramentoIsNull(pet.getTutor().getIdTutor())
                .map(VinculoTutorClinica::getClinica).orElse(null);
    }

    private static String emailDe(Tutor tutor) {
        return tutor == null ? null : tutor.getDsEmail();
    }
}