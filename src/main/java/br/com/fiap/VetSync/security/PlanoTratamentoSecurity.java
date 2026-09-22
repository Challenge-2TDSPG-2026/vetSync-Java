package br.com.fiap.VetSync.security;

import br.com.fiap.VetSync.repository.PlanoItemRepository;
import br.com.fiap.VetSync.repository.PlanoTratamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("planoTratamentoSecurity")
@RequiredArgsConstructor
public class PlanoTratamentoSecurity {

    private final PlanoTratamentoRepository planoTratamentoRepository;
    private final PlanoItemRepository planoItemRepository;
    private final PetAccessSecurity petAccessSecurity;

    /** Veterinário que prescreveu, OU tutor com acesso ao pet (proprietário, cuidador/cônjuge) — visualização. */
    public boolean isRelacionadoAoPlano(Long idPlano, Authentication authentication) {
        if (authentication == null || idPlano == null) return false;
        return planoTratamentoRepository.findById(idPlano).map(plano -> {
            boolean isVet = plano.getVeterinario() != null
                    && plano.getVeterinario().getDsEmail().equalsIgnoreCase(authentication.getName());
            boolean temAcesso = plano.getPet() != null
                    && petAccessSecurity.canView(plano.getPet().getIdPet(), authentication);
            return isVet || temAcesso;
        }).orElse(false);
    }

    /** Proprietário OU cuidador/cônjuge com EDICAO — agendar o próximo item do plano. */
    public boolean isTutorDoItem(Long idItem, Authentication authentication) {
        if (authentication == null || idItem == null) return false;
        return planoItemRepository.findById(idItem).map(item -> {
            var plano = item.getPlano();
            return plano != null && plano.getPet() != null
                    && petAccessSecurity.canEdit(plano.getPet().getIdPet(), authentication);
        }).orElse(false);
    }
}