package br.com.fiap.VetSync.security;

import br.com.fiap.VetSync.entity.PermissaoPet;
import br.com.fiap.VetSync.entity.StatusAcessoPet;
import br.com.fiap.VetSync.repository.PetAcessoRepository;
import br.com.fiap.VetSync.repository.PetRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import br.com.fiap.VetSync.repository.VinculoTutorClinicaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("petAccessSecurity")
@RequiredArgsConstructor
public class PetAccessSecurity {

    private final PetRepository petRepository;
    private final PetSecurity petSecurity;
    private final PetAcessoRepository petAcessoRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final VinculoTutorClinicaRepository vinculoRepository;

    public boolean isOwner(Long idPet, Authentication authentication) {
        return petSecurity.isOwner(idPet, authentication);
    }

    public boolean canView(Long idPet, Authentication authentication) {
        if (isOwner(idPet, authentication)) {
            return true;
        }
        return temAcessoAtivo(idPet, authentication, null);
    }

    /** Veterinario so acessa dados de pets cujos tutores tem vinculo ativo com sua clinica contratante. */
    public boolean canVeterinarioView(Long idPet, Authentication authentication) {
        if (idPet == null) {
            return false;
        }
        return petRepository.findById(idPet)
                .map(pet -> pet.getTutor() != null
                        && canVeterinarioViewTutor(pet.getTutor().getIdTutor(), authentication))
                .orElse(false);
    }

    /** Escopo para a listagem de pets de um tutor por veterinario: mesmo vinculo clinico ativo. */
    public boolean canVeterinarioViewTutor(Long idTutor, Authentication authentication) {
        if (authentication == null || idTutor == null || !PerfilUtils.isVeterinario(authentication)) {
            return false;
        }
        Long idClinica = veterinarioRepository.findByDsEmail(authentication.getName())
                .map(v -> v.getClinica() != null ? v.getClinica().getIdClinica() : null)
                .orElse(null);
        return idClinica != null && vinculoRepository
                .findByTutor_IdTutorAndDtEncerramentoIsNull(idTutor)
                .map(v -> v.getClinica().estaContratanteAtiva()
                        && idClinica.equals(v.getClinica().getIdClinica()))
                .orElse(false);
    }
    public boolean canEdit(Long idPet, Authentication authentication) {
        if (isOwner(idPet, authentication)) {
            return true;
        }
        return temAcessoAtivo(idPet, authentication, PermissaoPet.EDICAO)
                || temAcessoAtivo(idPet, authentication, PermissaoPet.EDITAR_PERFIL)
                || temAcessoAtivo(idPet, authentication, PermissaoPet.CRIAR_EVENTO);
    }

    private boolean temAcessoAtivo(Long idPet, Authentication authentication, PermissaoPet permissaoExigida) {
        if (authentication == null || idPet == null) {
            return false;
        }
        return petAcessoRepository.findByPet_IdPetAndDsStatus(idPet, StatusAcessoPet.ATIVO).stream()
                .anyMatch(acesso -> acesso.getTutor() != null
                        && acesso.getTutor().getDsEmail().equalsIgnoreCase(authentication.getName())
                        && (permissaoExigida == null
                        || acesso.getDsPermissao() == permissaoExigida
                        || (permissaoExigida == PermissaoPet.EDICAO && acesso.getDsPermissao() == PermissaoPet.CRIAR_EVENTO)
                        || (permissaoExigida == PermissaoPet.EDICAO && acesso.getDsPermissao() == PermissaoPet.EDITAR_PERFIL)));
    }
}
