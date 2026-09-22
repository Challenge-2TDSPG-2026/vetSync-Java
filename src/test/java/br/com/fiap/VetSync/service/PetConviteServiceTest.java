package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Pet;
import br.com.fiap.VetSync.entity.PermissaoPet;
import br.com.fiap.VetSync.entity.PetConvite;
import br.com.fiap.VetSync.entity.RelacaoPet;
import br.com.fiap.VetSync.entity.StatusConvitePet;
import br.com.fiap.VetSync.entity.Tutor;
import br.com.fiap.VetSync.repository.PetConviteRepository;
import br.com.fiap.VetSync.repository.PetRepository;
import br.com.fiap.VetSync.repository.TutorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetConviteServiceTest {

    @Mock
    private PetConviteRepository petConviteRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private PetAcessoService petAcessoService;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PetConviteService petConviteService;

    private Tutor dono() {
        return Tutor.builder().idTutor(1L).nmTutor("Dono").dsEmail("dono@teste.com").build();
    }

    private Pet petDoDono(Tutor dono) {
        return Pet.builder().idPet(10L).nmPet("Rex").tutor(dono).build();
    }

    // ---------------------------------------------------------------
    // Criar convite
    // ---------------------------------------------------------------

    @Test
    void criar_TutorProprietario_Sucesso() {
        Tutor dono = dono();
        Pet pet = petDoDono(dono);

        when(petRepository.findById(10L)).thenReturn(Optional.of(pet));
        when(tutorRepository.existsByDsEmail("cuidador@teste.com")).thenReturn(false);
        when(petConviteRepository.findByPet_IdPetAndDsEmailDestinoIgnoreCaseAndDsStatus(
                10L, "cuidador@teste.com", StatusConvitePet.PENDENTE)).thenReturn(List.of());
        when(petConviteRepository.save(any(PetConvite.class))).thenAnswer(i -> i.getArgument(0));

        PetConvite convite = petConviteService.criar(
                10L, 1L, "Cuidador@Teste.com", RelacaoPet.CUIDADOR, PermissaoPet.LEITURA);

        assertThat(convite.getDsEmailDestino()).isEqualTo("cuidador@teste.com"); // normalizado para minúsculo
        assertThat(convite.getDsStatus()).isEqualTo(StatusConvitePet.PENDENTE);
        assertThat(convite.getDsTokenHash()).isNotBlank();
        verify(emailService).enviar(eq("cuidador@teste.com"), anyString(), anyString());
    }

    @Test
    void criar_NaoProprietario_LancaForbidden() {
        Tutor dono = dono();
        Pet pet = petDoDono(dono);
        when(petRepository.findById(10L)).thenReturn(Optional.of(pet));

        assertThatThrownBy(() -> petConviteService.criar(
                10L, 999L, "cuidador@teste.com", RelacaoPet.CUIDADOR, PermissaoPet.LEITURA))
                .isInstanceOf(PetConviteService.ConviteException.class)
                .hasFieldOrPropertyWithValue("status", org.springframework.http.HttpStatus.FORBIDDEN);

        verify(petConviteRepository, never()).save(any());
        verify(emailService, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void criar_EmailJaTemConta_LancaConflict() {
        Tutor dono = dono();
        Pet pet = petDoDono(dono);
        when(petRepository.findById(10L)).thenReturn(Optional.of(pet));
        when(tutorRepository.existsByDsEmail("jatenho@teste.com")).thenReturn(true);

        assertThatThrownBy(() -> petConviteService.criar(
                10L, 1L, "jatenho@teste.com", RelacaoPet.CONJUGE, PermissaoPet.EDICAO))
                .isInstanceOf(PetConviteService.ConviteException.class)
                .hasFieldOrPropertyWithValue("status", org.springframework.http.HttpStatus.CONFLICT);

        verify(petConviteRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // Consultar por token (público)
    // ---------------------------------------------------------------

    @Test
    void consultarPorToken_Valido_NaoExpoeSegredo() {
        Tutor dono = dono();
        Pet pet = petDoDono(dono);
        PetConvite convite = PetConvite.builder()
                .idConvite(5L)
                .pet(pet)
                .tutorOrigem(dono)
                .dsEmailDestino("cuidador@teste.com")
                .dsRelacao(RelacaoPet.CUIDADOR)
                .dsPermissao(PermissaoPet.LEITURA)
                .dsTokenHash("hash-fake")
                .dsStatus(StatusConvitePet.PENDENTE)
                .dtCriacao(LocalDateTime.now())
                .dtExpiracao(LocalDateTime.now().plusHours(1))
                .build();
        when(petConviteRepository.findByDsTokenHash(anyString())).thenReturn(Optional.of(convite));

        var publico = petConviteService.consultarPorToken("token-puro-qualquer");

        assertThat(publico.email()).isEqualTo("cuidador@teste.com");
        assertThat(publico.nomePet()).isEqualTo("Rex");
        assertThat(publico.relacao()).isEqualTo(RelacaoPet.CUIDADOR);
        assertThat(publico.permissao()).isEqualTo(PermissaoPet.LEITURA);
        // ConvitePublico não tem campo de CPF, id do tutor, token ou hash — o record só expõe
        // email/nomePet/relacao/permissao/expiraEm, então não há como vazar dado sensível aqui.
    }

    @Test
    void consultarPorToken_Expirado_Lanca410() {
        Tutor dono = dono();
        Pet pet = petDoDono(dono);
        PetConvite convite = PetConvite.builder()
                .idConvite(5L).pet(pet).tutorOrigem(dono)
                .dsEmailDestino("cuidador@teste.com")
                .dsRelacao(RelacaoPet.CUIDADOR).dsPermissao(PermissaoPet.LEITURA)
                .dsTokenHash("hash-fake").dsStatus(StatusConvitePet.PENDENTE)
                .dtCriacao(LocalDateTime.now().minusDays(5))
                .dtExpiracao(LocalDateTime.now().minusHours(1)) // já passou
                .build();
        when(petConviteRepository.findByDsTokenHash(anyString())).thenReturn(Optional.of(convite));
        when(petConviteRepository.save(any(PetConvite.class))).thenAnswer(i -> i.getArgument(0));

        assertThatThrownBy(() -> petConviteService.consultarPorToken("token-puro"))
                .isInstanceOf(PetConviteService.ConviteException.class)
                .hasFieldOrPropertyWithValue("status", org.springframework.http.HttpStatus.GONE)
                .hasMessage("Convite expirado");
    }

    @Test
    void consultarPorToken_JaAceito_Lanca410() {
        Tutor dono = dono();
        Pet pet = petDoDono(dono);
        PetConvite convite = PetConvite.builder()
                .idConvite(5L).pet(pet).tutorOrigem(dono)
                .dsEmailDestino("cuidador@teste.com")
                .dsRelacao(RelacaoPet.CUIDADOR).dsPermissao(PermissaoPet.LEITURA)
                .dsTokenHash("hash-fake").dsStatus(StatusConvitePet.ACEITO)
                .dtCriacao(LocalDateTime.now().minusDays(1))
                .dtExpiracao(LocalDateTime.now().plusHours(1))
                .build();
        when(petConviteRepository.findByDsTokenHash(anyString())).thenReturn(Optional.of(convite));

        assertThatThrownBy(() -> petConviteService.consultarPorToken("token-puro"))
                .isInstanceOf(PetConviteService.ConviteException.class)
                .hasFieldOrPropertyWithValue("status", org.springframework.http.HttpStatus.GONE);
    }

    @Test
    void consultarPorToken_NaoEncontrado_Lanca404() {
        when(petConviteRepository.findByDsTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> petConviteService.consultarPorToken("token-invalido"))
                .isInstanceOf(PetConviteService.ConviteException.class)
                .hasFieldOrPropertyWithValue("status", org.springframework.http.HttpStatus.NOT_FOUND);
    }

    // ---------------------------------------------------------------
    // Aceitar convite (cadastro do cuidador)
    // ---------------------------------------------------------------

    @Test
    void aceitar_CriaTutorEAcesso_EmailVemDoConvite() {
        Tutor dono = dono();
        Pet pet = petDoDono(dono);
        PetConvite convite = PetConvite.builder()
                .idConvite(5L).pet(pet).tutorOrigem(dono)
                .dsEmailDestino("cuidador@teste.com")
                .dsRelacao(RelacaoPet.CUIDADOR).dsPermissao(PermissaoPet.EDICAO)
                .dsTokenHash("hash-fake").dsStatus(StatusConvitePet.PENDENTE)
                .dtCriacao(LocalDateTime.now())
                .dtExpiracao(LocalDateTime.now().plusHours(1))
                .build();
        when(petConviteRepository.findByDsTokenHash(anyString())).thenReturn(Optional.of(convite));
        when(tutorRepository.existsByDsEmail("cuidador@teste.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hash-bcrypt");
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(i -> {
            Tutor t = i.getArgument(0);
            t.setIdTutor(42L);
            return t;
        });
        when(petConviteRepository.save(any(PetConvite.class))).thenAnswer(i -> i.getArgument(0));

        // O body de cadastro não tem campo de e-mail — só token, nome, cpf, telefone, senha.
        Tutor tutorCriado = petConviteService.aceitar("token-puro", "Maria da Silva", "12345678901",
                "11999999999", "senha123");

        assertThat(tutorCriado.getDsEmail()).isEqualTo("cuidador@teste.com"); // veio do convite, não do body
        assertThat(tutorCriado.getDsSenha()).isEqualTo("hash-bcrypt");
        assertThat(convite.getDsStatus()).isEqualTo(StatusConvitePet.ACEITO);
        assertThat(convite.getDtAceite()).isNotNull();

        verify(petAcessoService).conceder(pet, tutorCriado, RelacaoPet.CUIDADOR, PermissaoPet.EDICAO);
    }

    @Test
    void aceitar_TokenJaAceito_NaoRegistraConta() {
        Tutor dono = dono();
        Pet pet = petDoDono(dono);
        PetConvite convite = PetConvite.builder()
                .idConvite(5L).pet(pet).tutorOrigem(dono)
                .dsEmailDestino("cuidador@teste.com")
                .dsRelacao(RelacaoPet.CUIDADOR).dsPermissao(PermissaoPet.LEITURA)
                .dsTokenHash("hash-fake").dsStatus(StatusConvitePet.ACEITO)
                .dtCriacao(LocalDateTime.now().minusDays(1))
                .dtExpiracao(LocalDateTime.now().plusHours(1))
                .build();
        when(petConviteRepository.findByDsTokenHash(anyString())).thenReturn(Optional.of(convite));

        assertThatThrownBy(() -> petConviteService.aceitar("token-puro", "Maria", "12345678901", "11999999999", "senha123"))
                .isInstanceOf(PetConviteService.ConviteException.class);

        verify(tutorRepository, never()).save(any());
        verify(petAcessoService, never()).conceder(any(), any(), any(), any());
    }

    @Test
    void aceitar_TokenExpirado_NaoRegistraConta() {
        Tutor dono = dono();
        Pet pet = petDoDono(dono);
        PetConvite convite = PetConvite.builder()
                .idConvite(5L).pet(pet).tutorOrigem(dono)
                .dsEmailDestino("cuidador@teste.com")
                .dsRelacao(RelacaoPet.CUIDADOR).dsPermissao(PermissaoPet.LEITURA)
                .dsTokenHash("hash-fake").dsStatus(StatusConvitePet.PENDENTE)
                .dtCriacao(LocalDateTime.now().minusDays(5))
                .dtExpiracao(LocalDateTime.now().minusMinutes(1))
                .build();
        when(petConviteRepository.findByDsTokenHash(anyString())).thenReturn(Optional.of(convite));
        when(petConviteRepository.save(any(PetConvite.class))).thenAnswer(i -> i.getArgument(0));

        assertThatThrownBy(() -> petConviteService.aceitar("token-puro", "Maria", "12345678901", "11999999999", "senha123"))
                .isInstanceOf(PetConviteService.ConviteException.class)
                .hasMessage("Convite expirado");

        verify(tutorRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // Cancelar convite
    // ---------------------------------------------------------------

    @Test
    void cancelar_ConvitePendente_Sucesso() {
        Tutor dono = dono();
        Pet pet = petDoDono(dono);
        PetConvite convite = PetConvite.builder()
                .idConvite(5L).pet(pet).tutorOrigem(dono)
                .dsEmailDestino("cuidador@teste.com")
                .dsRelacao(RelacaoPet.CUIDADOR).dsPermissao(PermissaoPet.LEITURA)
                .dsTokenHash("hash-fake").dsStatus(StatusConvitePet.PENDENTE)
                .dtCriacao(LocalDateTime.now())
                .dtExpiracao(LocalDateTime.now().plusHours(1))
                .build();
        when(petConviteRepository.findById(5L)).thenReturn(Optional.of(convite));
        when(petConviteRepository.save(any(PetConvite.class))).thenAnswer(i -> i.getArgument(0));

        petConviteService.cancelar(10L, 5L);

        assertThat(convite.getDsStatus()).isEqualTo(StatusConvitePet.CANCELADO);
    }
}