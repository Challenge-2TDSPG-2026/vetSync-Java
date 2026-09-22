package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Pet;
import br.com.fiap.VetSync.entity.PermissaoPet;
import br.com.fiap.VetSync.entity.PetAcesso;
import br.com.fiap.VetSync.entity.RelacaoPet;
import br.com.fiap.VetSync.entity.StatusAcessoPet;
import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.repository.PetAcessoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetAcessoServiceTest {

    @Mock
    private PetAcessoRepository petAcessoRepository;

    @InjectMocks
    private PetAcessoService petAcessoService;

    @Test
    void conceder_SemAcessoAnterior_CriaNovo() {
        Pet pet = Pet.builder().idPet(1L).build();
        Tutor tutor = Tutor.builder().idTutor(2L).build();
        when(petAcessoRepository.findByPet_IdPetAndTutor_IdTutor(1L, 2L)).thenReturn(Optional.empty());
        when(petAcessoRepository.save(any(PetAcesso.class))).thenAnswer(i -> i.getArgument(0));

        PetAcesso acesso = petAcessoService.conceder(pet, tutor, RelacaoPet.CUIDADOR, PermissaoPet.LEITURA);

        assertThat(acesso.getDsStatus()).isEqualTo(StatusAcessoPet.ATIVO);
        assertThat(acesso.getDsRelacao()).isEqualTo(RelacaoPet.CUIDADOR);
        assertThat(acesso.getDsPermissao()).isEqualTo(PermissaoPet.LEITURA);
    }

    @Test
    void conceder_ComAcessoRevogadoAnterior_Reativa() {
        Pet pet = Pet.builder().idPet(1L).build();
        Tutor tutor = Tutor.builder().idTutor(2L).build();
        PetAcesso revogado = PetAcesso.builder()
                .idAcesso(9L).pet(pet).tutor(tutor)
                .dsRelacao(RelacaoPet.CUIDADOR).dsPermissao(PermissaoPet.LEITURA)
                .dsStatus(StatusAcessoPet.REVOGADO)
                .dtConcedido(LocalDateTime.now().minusDays(30))
                .dtRevogado(LocalDateTime.now().minusDays(1))
                .build();
        when(petAcessoRepository.findByPet_IdPetAndTutor_IdTutor(1L, 2L)).thenReturn(Optional.of(revogado));
        when(petAcessoRepository.save(any(PetAcesso.class))).thenAnswer(i -> i.getArgument(0));

        PetAcesso acesso = petAcessoService.conceder(pet, tutor, RelacaoPet.CONJUGE, PermissaoPet.EDICAO);

        assertThat(acesso.getIdAcesso()).isEqualTo(9L); // mesmo registro, reativado (respeita UNIQUE pet+tutor)
        assertThat(acesso.getDsStatus()).isEqualTo(StatusAcessoPet.ATIVO);
        assertThat(acesso.getDsRelacao()).isEqualTo(RelacaoPet.CONJUGE);
        assertThat(acesso.getDsPermissao()).isEqualTo(PermissaoPet.EDICAO);
        assertThat(acesso.getDtRevogado()).isNull();
    }

    @Test
    void revogar_AcessoAtivo_Sucesso() {
        Pet pet = Pet.builder().idPet(1L).build();
        PetAcesso acesso = PetAcesso.builder()
                .idAcesso(9L).pet(pet)
                .dsStatus(StatusAcessoPet.ATIVO)
                .dtConcedido(LocalDateTime.now())
                .build();
        when(petAcessoRepository.findById(9L)).thenReturn(Optional.of(acesso));
        when(petAcessoRepository.save(any(PetAcesso.class))).thenAnswer(i -> i.getArgument(0));

        petAcessoService.revogar(1L, 9L);

        assertThat(acesso.getDsStatus()).isEqualTo(StatusAcessoPet.REVOGADO);
        assertThat(acesso.getDtRevogado()).isNotNull();
    }

    @Test
    void revogar_AcessoDeOutroPet_Lanca404() {
        Pet pet = Pet.builder().idPet(1L).build();
        PetAcesso acesso = PetAcesso.builder().idAcesso(9L).pet(pet).dsStatus(StatusAcessoPet.ATIVO).build();
        when(petAcessoRepository.findById(9L)).thenReturn(Optional.of(acesso));

        assertThatThrownBy(() -> petAcessoService.revogar(999L, 9L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    void revogar_JaRevogado_Lanca409() {
        Pet pet = Pet.builder().idPet(1L).build();
        PetAcesso acesso = PetAcesso.builder().idAcesso(9L).pet(pet).dsStatus(StatusAcessoPet.REVOGADO).build();
        when(petAcessoRepository.findById(9L)).thenReturn(Optional.of(acesso));

        assertThatThrownBy(() -> petAcessoService.revogar(1L, 9L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("já está revogado");
    }
}