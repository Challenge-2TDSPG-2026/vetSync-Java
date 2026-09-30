package br.com.fiap.VetSync.security;

import br.com.fiap.VetSync.entity.StatusEvento;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import br.com.fiap.VetSync.repository.ProfissionalEsteticaRepository;
import br.com.fiap.VetSync.repository.VinculoTutorClinicaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("eventoSecurity")
@RequiredArgsConstructor
public class EventoSecurity {

    private final EventoSaudeRepository eventoSaudeRepository;
    private final PetAccessSecurity petAccessSecurity;
    private final VeterinarioRepository veterinarioRepository;
    private final ProfissionalEsteticaRepository profissionalEsteticaRepository;
    private final VinculoTutorClinicaRepository vinculoTutorClinicaRepository;

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

    /** Mantido por compatibilidade: só o proprietário do pet (não considera cuidador/cônjuge). */
    public boolean isTutorDoPet(Long idEvento, Authentication authentication) {
        if (authentication == null || idEvento == null) return false;
        return eventoSaudeRepository.findById(idEvento)
                .map(e -> e.getPet() != null && e.getPet().getTutor() != null
                        && e.getPet().getTutor().getDsEmail().equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }

    /** Proprietário OU cuidador/cônjuge com acesso ativo (LEITURA ou EDICAO) — para visualizar o evento. */
    public boolean isTutorComAcesso(Long idEvento, Authentication authentication) {
        if (authentication == null || idEvento == null) return false;
        Long idPet = eventoSaudeRepository.findById(idEvento)
                .map(e -> e.getPet() != null ? e.getPet().getIdPet() : null)
                .orElse(null);
        return idPet != null && petAccessSecurity.canView(idPet, authentication);
    }

    /** Proprietário OU cuidador/cônjuge com EDICAO — para cancelar/editar o evento. */
    public boolean isTutorComEdicao(Long idEvento, Authentication authentication) {
        if (authentication == null || idEvento == null) return false;
        Long idPet = eventoSaudeRepository.findById(idEvento)
                .map(e -> e.getPet() != null ? e.getPet().getIdPet() : null)
                .orElse(null);
        return idPet != null && petAccessSecurity.canEdit(idPet, authentication);
    }


    public boolean isRelacionado(Long idEvento, Authentication authentication) {
        return isVeterinarioResponsavel(idEvento, authentication)
                || isProfissionalEsteticaResponsavel(idEvento, authentication)
                || isProfissionalDaClinicaAtivaDoTutor(idEvento, authentication)
                || isTutorComAcesso(idEvento, authentication);
    }

    private boolean isProfissionalDaClinicaAtivaDoTutor(Long idEvento, Authentication authentication) {
        if (authentication == null || idEvento == null) return false;
        Long idClinica = veterinarioRepository.findByDsEmail(authentication.getName())
                .map(v -> v.getClinica().getIdClinica())
                .or(() -> profissionalEsteticaRepository.findByDsEmail(authentication.getName())
                        .map(p -> p.getClinica().getIdClinica()))
                .orElse(null);
        if (idClinica == null) return false;
        return eventoSaudeRepository.findById(idEvento)
                .map(e -> e.getPet() != null && e.getPet().getTutor() != null
                        && vinculoTutorClinicaRepository.findByTutor_IdTutorAndDtEncerramentoIsNull(e.getPet().getTutor().getIdTutor())
                        .map(v -> v.getClinica().estaContratanteAtiva() && v.getClinica().getIdClinica().equals(idClinica))
                        .orElse(false))
                .orElse(false);
    }


    public boolean canDelete(Long idEvento, Authentication authentication) {
        if (authentication == null || idEvento == null) return false;
        return eventoSaudeRepository.findById(idEvento).map(e -> {
            boolean isVet = e.getVeterinario() != null
                    && e.getVeterinario().getDsEmail().equalsIgnoreCase(authentication.getName());
            boolean isProfEstetica = e.getProfissionalEstetica() != null
                    && e.getProfissionalEstetica().getDsEmail().equalsIgnoreCase(authentication.getName());
            boolean isTutorPendente = e.getPet() != null
                    && e.getDsStatus() == StatusEvento.AGENDADO
                    && petAccessSecurity.canEdit(e.getPet().getIdPet(), authentication);
            return isVet || isProfEstetica || isTutorPendente;
        }).orElse(false);
    }
}