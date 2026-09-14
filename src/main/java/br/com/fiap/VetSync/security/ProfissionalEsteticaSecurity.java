package br.com.fiap.VetSync.security;

import br.com.fiap.VetSync.repository.ProfissionalEsteticaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("profissionalEsteticaSecurity")
@RequiredArgsConstructor
public class ProfissionalEsteticaSecurity {

    private final ProfissionalEsteticaRepository profissionalEsteticaRepository;

    public boolean isSelf(Long idProfissionalEstetica, Authentication authentication) {
        if (authentication == null || idProfissionalEstetica == null) {
            return false;
        }
        return profissionalEsteticaRepository.findById(idProfissionalEstetica)
                .map(p -> p.getDsEmail().equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }
}