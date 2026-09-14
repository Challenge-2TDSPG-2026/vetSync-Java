package br.com.fiap.VetSync.security;

import br.com.fiap.VetSync.entity.StatusEvento;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("eventoSecurity")
@RequiredArgsConstructor
public class EventoSecurity {

    private final EventoSaudeRepository eventoSaudeRepository;

    public boolean isVeterinarioResponsavel(Long idEvento, Authentication authentication) {
        if (authentication == null || idEvento == null) return false;
        return eventoSaudeRepository.findById(idEvento)
                .map(e -> e.getVeterinario() != null
                        && e.getVeterinario().getDsEmail().equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }

    public boolean isProfissionalEsteticaResponsavel(Long idEvento, Authentication authentication) {
        if (authentication == null || idEvento == null) return false;
        return eventoSaudeRepository.findById(idEvento)
                .map(e -> e.getProfissionalEstetica() != null
                        && e.getProfissionalEstetica().getDsEmail().equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }

    public boolean isTutorDoPet(Long idEvento, Authentication authentication) {
        if (authentication == null || idEvento == null) return false;
        return eventoSaudeRepository.findById(idEvento)
                .map(e -> e.getPet() != null && e.getPet().getTutor() != null
                        && e.getPet().getTutor().getDsEmail().equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }


    public boolean isRelacionado(Long idEvento, Authentication authentication) {
        return isVeterinarioResponsavel(idEvento, authentication)
                || isProfissionalEsteticaResponsavel(idEvento, authentication)
                || isTutorDoPet(idEvento, authentication);
    }


    public boolean canDelete(Long idEvento, Authentication authentication) {
        if (authentication == null || idEvento == null) return false;
        return eventoSaudeRepository.findById(idEvento).map(e -> {
            boolean isVet = e.getVeterinario() != null
                    && e.getVeterinario().getDsEmail().equalsIgnoreCase(authentication.getName());
            boolean isProfEstetica = e.getProfissionalEstetica() != null
                    && e.getProfissionalEstetica().getDsEmail().equalsIgnoreCase(authentication.getName());
            boolean isTutorPendente = e.getPet() != null && e.getPet().getTutor() != null
                    && e.getPet().getTutor().getDsEmail().equalsIgnoreCase(authentication.getName())
                    && e.getDsStatus() == StatusEvento.AGENDADO;
            return isVet || isProfEstetica || isTutorPendente;
        }).orElse(false);
    }
}