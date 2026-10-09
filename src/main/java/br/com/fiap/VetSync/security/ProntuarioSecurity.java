package br.com.fiap.VetSync.security;

import br.com.fiap.VetSync.entity.PermissaoPet;
import br.com.fiap.VetSync.entity.StatusAcessoPet;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import br.com.fiap.VetSync.repository.PetAcessoRepository;
import br.com.fiap.VetSync.repository.PetRepository;
import br.com.fiap.VetSync.repository.VeterinarioRepository;
import br.com.fiap.VetSync.repository.VinculoTutorClinicaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Regras de acesso ao prontuário clínico (consulta, exportação e registro de exames).
 * Mais restritivas que o acesso comum ao pet: o prontuário reúne diagnóstico, receitas e exames.
 *
 * <ul>
 *   <li>Tutor proprietário: sempre.</li>
 *   <li>Cuidador/cônjuge: só com permissão {@code VISUALIZAR_PRONTUARIO} ou {@code EDICAO}.</li>
 *   <li>Veterinário: se já atendeu o pet OU se é da clínica ativa/contratante do tutor.</li>
 * </ul>
 */
@Component("prontuarioSecurity")
@RequiredArgsConstructor
public class ProntuarioSecurity {

    /** Permissões de cuidador que dão acesso ao prontuário. Ajuste aqui para mudar a política. */
    static final Set<PermissaoPet> PERMISSOES_PRONTUARIO = Set.of(
            PermissaoPet.VISUALIZAR_PRONTUARIO, PermissaoPet.EDICAO);

    private final PetRepository petRepository;
    private final PetAcessoRepository petAcessoRepository;
    private final EventoSaudeRepository eventoSaudeRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final VinculoTutorClinicaRepository vinculoTutorClinicaRepository;

    /** Só o tutor proprietário (usado para compartilhar/revogar links). */
    public boolean isProprietario(Long idPet, Authentication authentication) {
        if (!autenticado(authentication) || idPet == null || !PerfilUtils.isTutor(authentication)) {
            return false;
        }
        return petRepository.findById(idPet)
                .map(pet -> pet.getTutor() != null && pet.getTutor().getDsEmail() != null
                        && pet.getTutor().getDsEmail().equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }

    /** Pode ler/exportar o prontuário. */
    public boolean canView(Long idPet, Authentication authentication) {
        if (!autenticado(authentication) || idPet == null) {
            return false;
        }
        if (PerfilUtils.isTutor(authentication)) {
            return isProprietario(idPet, authentication) || temPermissaoDeCuidador(idPet, authentication);
        }
        return isVeterinarioDoPet(idPet, authentication);
    }

    /** Pode registrar exames: somente veterinário com vínculo com o pet. */
    public boolean canRegistrar(Long idPet, Authentication authentication) {
        return autenticado(authentication) && idPet != null && isVeterinarioDoPet(idPet, authentication);
    }

    private boolean temPermissaoDeCuidador(Long idPet, Authentication authentication) {
        return petAcessoRepository.findByPet_IdPetAndDsStatus(idPet, StatusAcessoPet.ATIVO).stream()
                .anyMatch(acesso -> acesso.getTutor() != null && acesso.getTutor().getDsEmail() != null
                        && acesso.getTutor().getDsEmail().equalsIgnoreCase(authentication.getName())
                        && PERMISSOES_PRONTUARIO.contains(acesso.getDsPermissao()));
    }

    private boolean isVeterinarioDoPet(Long idPet, Authentication authentication) {
        if (!PerfilUtils.isVeterinario(authentication)) {
            return false;
        }
        String email = authentication.getName();
        if (eventoSaudeRepository.existsByPet_IdPetAndVeterinario_DsEmailIgnoreCase(idPet, email)) {
            return true;
        }
        Long idClinica = veterinarioRepository.findByDsEmail(email)
                .map(v -> v.getClinica() != null ? v.getClinica().getIdClinica() : null)
                .orElse(null);
        if (idClinica == null) {
            return false;
        }
        return petRepository.findById(idPet)
                .map(pet -> pet.getTutor() != null
                        && vinculoTutorClinicaRepository
                        .findByTutor_IdTutorAndDtEncerramentoIsNull(pet.getTutor().getIdTutor())
                        .map(v -> v.getClinica().estaContratanteAtiva()
                                && idClinica.equals(v.getClinica().getIdClinica()))
                        .orElse(false))
                .orElse(false);
    }

    private static boolean autenticado(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() && authentication.getName() != null;
    }
}