package br.com.fiap.VetSync.security;

import br.com.fiap.VetSync.entity.PermissaoPet;
import br.com.fiap.VetSync.entity.StatusAcessoPet;
import br.com.fiap.VetSync.repository.PetAcessoRepository;
import br.com.fiap.VetSync.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("petAccessSecurity")
@RequiredArgsConstructor
public class PetAccessSecurity {

    private final PetRepository petRepository;
    private final PetAcessoRepository petAcessoRepository;

    public boolean isOwner(Long idPet, Authentication authentication) {
        if (authentication == null || idPet == null) {
            return false;
        }
        return petRepository.findById(idPet)
                .map(pet -> pet.getTutor() != null
                        && pet.getTutor().getDsEmail().equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }

    public boolean canView(Long idPet, Authentication authentication) {
        if (isOwner(idPet, authentication)) {
            return true;
        }
        return temAcessoAtivo(idPet, authentication, null);
    }

    public boolean canEdit(Long idPet, Authentication authentication) {
        if (isOwner(idPet, authentication)) {
            return true;
        }
        return temAcessoAtivo(idPet, authentication, PermissaoPet.EDICAO);
    }

    private boolean temAcessoAtivo(Long idPet, Authentication authentication, PermissaoPet permissaoExigida) {
        if (authentication == null || idPet == null) {
            return false;
        }
        return petAcessoRepository.findByPet_IdPetAndDsStatus(idPet, StatusAcessoPet.ATIVO).stream()
                .anyMatch(acesso -> acesso.getTutor() != null
                        && acesso.getTutor().getDsEmail().equalsIgnoreCase(authentication.getName())
                        && (permissaoExigida == null || acesso.getDsPermissao() == permissaoExigida));
    }
}