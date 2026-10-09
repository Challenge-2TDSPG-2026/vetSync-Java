package br.com.fiap.VetSync.security;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProntuarioSecurityTest {

    @Mock private PetRepository petRepository;
    @Mock private PetAcessoRepository petAcessoRepository;
    @Mock private EventoSaudeRepository eventoSaudeRepository;
    @Mock private VeterinarioRepository veterinarioRepository;
    @Mock private VinculoTutorClinicaRepository vinculoTutorClinicaRepository;

    @InjectMocks private ProntuarioSecurity security;

    private static final Long PET = 10L;

    private static Authentication auth(String email, String role) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority(role)));
    }

    private Pet petDoTutor(String emailDono) {
        Tutor dono = Tutor.builder().idTutor(1L).dsEmail(emailDono).build();
        Pet pet = Pet.builder().idPet(PET).tutor(dono).build();
        lenient().when(petRepository.findById(PET)).thenReturn(Optional.of(pet));
        return pet;
    }

    private PetAcesso acesso(String email, PermissaoPet permissao) {
        return PetAcesso.builder().tutor(Tutor.builder().idTutor(2L).dsEmail(email).build())
                .dsPermissao(permissao).dsStatus(StatusAcessoPet.ATIVO).build();
    }

    @Test
    void proprietarioPodeVerEEhProprietario() {
        petDoTutor("dono@x.com");
        Authentication a = auth("DONO@x.com", "ROLE_TUTOR");

        assertThat(security.isProprietario(PET, a)).isTrue();
        assertThat(security.canView(PET, a)).isTrue();
    }

    @Test
    void cuidadorComVisualizarProntuarioPodeVer_masNaoCompartilhar() {
        petDoTutor("dono@x.com");
        when(petAcessoRepository.findByPet_IdPetAndDsStatus(PET, StatusAcessoPet.ATIVO))
                .thenReturn(List.of(acesso("cuidador@x.com", PermissaoPet.VISUALIZAR_PRONTUARIO)));
        Authentication a = auth("cuidador@x.com", "ROLE_TUTOR");

        assertThat(security.canView(PET, a)).isTrue();
        assertThat(security.isProprietario(PET, a)).isFalse();
    }

    @Test
    void cuidadorComEdicaoPodeVer() {
        petDoTutor("dono@x.com");
        when(petAcessoRepository.findByPet_IdPetAndDsStatus(PET, StatusAcessoPet.ATIVO))
                .thenReturn(List.of(acesso("cuidador@x.com", PermissaoPet.EDICAO)));

        assertThat(security.canView(PET, auth("cuidador@x.com", "ROLE_TUTOR"))).isTrue();
    }

    @Test
    void cuidadorSomenteComLeituraNaoVeOProntuario() {
        petDoTutor("dono@x.com");
        when(petAcessoRepository.findByPet_IdPetAndDsStatus(PET, StatusAcessoPet.ATIVO))
                .thenReturn(List.of(acesso("cuidador@x.com", PermissaoPet.LEITURA)));

        assertThat(security.canView(PET, auth("cuidador@x.com", "ROLE_TUTOR"))).isFalse();
    }

    @Test
    void tutorSemRelacaoNaoVe() {
        petDoTutor("dono@x.com");
        when(petAcessoRepository.findByPet_IdPetAndDsStatus(PET, StatusAcessoPet.ATIVO)).thenReturn(List.of());

        assertThat(security.canView(PET, auth("estranho@x.com", "ROLE_TUTOR"))).isFalse();
    }

    @Test
    void veterinarioQueJaAtendeuOPetPodeVerERegistrar() {
        when(eventoSaudeRepository.existsByPet_IdPetAndVeterinario_DsEmailIgnoreCase(PET, "vet@x.com")).thenReturn(true);
        Authentication a = auth("vet@x.com", "ROLE_VETERINARIO");

        assertThat(security.canView(PET, a)).isTrue();
        assertThat(security.canRegistrar(PET, a)).isTrue();
    }

    @Test
    void veterinarioDaClinicaContratanteAtivaDoTutorPodeVer() {
        Clinica clinica = Clinica.builder().idClinica(5L).stContratante("A").build();
        Veterinario vet = Veterinario.builder().idVeterinario(7L).dsEmail("vet@x.com").clinica(clinica).build();
        Pet pet = petDoTutor("dono@x.com");
        when(eventoSaudeRepository.existsByPet_IdPetAndVeterinario_DsEmailIgnoreCase(PET, "vet@x.com")).thenReturn(false);
        when(veterinarioRepository.findByDsEmail("vet@x.com")).thenReturn(Optional.of(vet));
        when(vinculoTutorClinicaRepository.findByTutor_IdTutorAndDtEncerramentoIsNull(pet.getTutor().getIdTutor()))
                .thenReturn(Optional.of(VinculoTutorClinica.builder().tutor(pet.getTutor()).clinica(clinica).build()));

        assertThat(security.canView(PET, auth("vet@x.com", "ROLE_VETERINARIO"))).isTrue();
    }

    @Test
    void veterinarioDeOutraClinicaNaoVe() {
        Clinica daVet = Clinica.builder().idClinica(5L).stContratante("A").build();
        Clinica doTutor = Clinica.builder().idClinica(6L).stContratante("A").build();
        Veterinario vet = Veterinario.builder().idVeterinario(7L).dsEmail("vet@x.com").clinica(daVet).build();
        Pet pet = petDoTutor("dono@x.com");
        when(eventoSaudeRepository.existsByPet_IdPetAndVeterinario_DsEmailIgnoreCase(PET, "vet@x.com")).thenReturn(false);
        when(veterinarioRepository.findByDsEmail("vet@x.com")).thenReturn(Optional.of(vet));
        when(vinculoTutorClinicaRepository.findByTutor_IdTutorAndDtEncerramentoIsNull(pet.getTutor().getIdTutor()))
                .thenReturn(Optional.of(VinculoTutorClinica.builder().tutor(pet.getTutor()).clinica(doTutor).build()));

        assertThat(security.canView(PET, auth("vet@x.com", "ROLE_VETERINARIO"))).isFalse();
    }

    @Test
    void veterinarioDeClinicaSemContratoAtivoNaoVe() {
        Clinica inativa = Clinica.builder().idClinica(5L).stContratante("I").build();
        Veterinario vet = Veterinario.builder().idVeterinario(7L).dsEmail("vet@x.com").clinica(inativa).build();
        Pet pet = petDoTutor("dono@x.com");
        when(eventoSaudeRepository.existsByPet_IdPetAndVeterinario_DsEmailIgnoreCase(PET, "vet@x.com")).thenReturn(false);
        when(veterinarioRepository.findByDsEmail("vet@x.com")).thenReturn(Optional.of(vet));
        when(vinculoTutorClinicaRepository.findByTutor_IdTutorAndDtEncerramentoIsNull(pet.getTutor().getIdTutor()))
                .thenReturn(Optional.of(VinculoTutorClinica.builder().tutor(pet.getTutor()).clinica(inativa).build()));

        assertThat(security.canRegistrar(PET, auth("vet@x.com", "ROLE_VETERINARIO"))).isFalse();
    }

    @Test
    void tutorNuncaPodeRegistrarExames() {
        assertThat(security.canRegistrar(PET, auth("dono@x.com", "ROLE_TUTOR"))).isFalse();
    }

    @Test
    void semAutenticacaoNegaTudo() {
        assertThat(security.canView(PET, null)).isFalse();
        assertThat(security.isProprietario(PET, null)).isFalse();
        assertThat(security.canRegistrar(PET, null)).isFalse();
    }
}