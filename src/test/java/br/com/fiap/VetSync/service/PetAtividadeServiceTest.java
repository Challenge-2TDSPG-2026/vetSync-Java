package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Auditoria;
import br.com.fiap.VetSync.entity.PetAcesso;
import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.repository.AuditoriaRepository;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import br.com.fiap.VetSync.repository.PetAcessoRepository;
import br.com.fiap.VetSync.repository.TutorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetAtividadeServiceTest {

    @Mock private AuditoriaRepository auditoriaRepository;
    @Mock private PetAcessoRepository petAcessoRepository;
    @Mock private EventoSaudeRepository eventoSaudeRepository;
    @Mock private TutorRepository tutorRepository;
    @InjectMocks private PetAtividadeService service;

    private static Auditoria registro(long id, String entidade, long idEntidade, String acao, String ator,
                                      String perfil, String novo) {
        return Auditoria.builder().idAuditoria(id).dsEntidade(entidade).idEntidade(idEntidade).dsAcao(acao)
                .dsAtor(ator).dsPerfil(perfil).dsValorNovo(novo).dtOcorrencia(LocalDateTime.of(2026, 10, 13, 10, 0)).build();
    }

    @Test
    void listar_traduzAuditoriaParaAtividadesSemVazarEmail() {
        Tutor maria = Tutor.builder().idTutor(2L).nmTutor("Maria").dsEmail("maria@x.com").build();
        Tutor ana = Tutor.builder().idTutor(1L).nmTutor("Ana").dsEmail("ana@x.com").build();
        when(petAcessoRepository.findByPet_IdPet(7L))
                .thenReturn(List.of(PetAcesso.builder().idAcesso(50L).tutor(maria).build()));
        when(eventoSaudeRepository.findByPet_IdPet(7L)).thenReturn(List.of());
        when(tutorRepository.findByDsEmail("ana@x.com")).thenReturn(Optional.of(ana));
        when(auditoriaRepository.findAtividadeDoPet(eq(7L), any(), any(), any(Pageable.class))).thenReturn(List.of(
                registro(3, "ACESSO_PET", 50, "ACESSO_REVOGADO", "ana@x.com", "TUTOR", "REVOGADO"),
                registro(2, "PET_CONVITE", 7, "CONVITE_ENVIADO", "ana@x.com", "TUTOR", "maria@x.com (LEITURA)"),
                registro(1, "PRONTUARIO", 7, "PRONTUARIO_EXPORTADO", "dr@clinica.com", "VETERINARIO", null)));

        var lista = service.listar(7L, null);

        assertThat(lista).extracting("tipo").containsExactly("ACESSO_REVOGADO", "CONVITE_ENVIADO", "PRONTUARIO_VISUALIZADO");
        assertThat(lista.get(0).descricao()).isEqualTo("Acesso de Maria removido");
        assertThat(lista.get(0).autorNome()).isEqualTo("Ana");
        assertThat(lista.get(1).descricao()).isEqualTo("Convite enviado para maria@x.com");
        assertThat(lista.get(2).autorNome()).isEqualTo("Equipe da clínica");
        assertThat(lista).noneMatch(a -> a.autorNome().contains("@"));
    }

    @Test
    void listar_semAcessosNemEventos_usaIdSentinelaELimitaOTamanho() {
        when(petAcessoRepository.findByPet_IdPet(7L)).thenReturn(List.of());
        when(eventoSaudeRepository.findByPet_IdPet(7L)).thenReturn(List.of());
        when(auditoriaRepository.findAtividadeDoPet(eq(7L), eq(List.of(-1L)), eq(List.of(-1L)), any(Pageable.class)))
                .thenReturn(List.of());

        service.listar(7L, 9999);

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(auditoriaRepository).findAtividadeDoPet(eq(7L), any(), any(), page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(PetAtividadeService.LIMITE_MAXIMO);
    }
}