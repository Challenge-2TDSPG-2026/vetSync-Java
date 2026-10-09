package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.*;
import br.com.fiap.VetSync.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProntuarioRegistroServiceTest {

    @Mock private OrientacaoClinicaRepository orientacaoRepository;
    @Mock private ResultadoExameRepository exameRepository;
    @Mock private EventoSaudeRepository eventoSaudeRepository;
    @Mock private PetRepository petRepository;
    @Mock private VeterinarioRepository veterinarioRepository;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks private ProntuarioRegistroService service;

    private final Pet pet = Pet.builder().idPet(10L).nmPet("Luna").build();
    private final Veterinario ana = Veterinario.builder().idVeterinario(1L).nmVeterinario("Dra. Ana").dsEmail("ana@vet.com").build();

    private EventoSaude evento(StatusEvento status) {
        return EventoSaude.builder().idEvento(100L).pet(pet).veterinario(ana).dsStatus(status).build();
    }

    private static HttpStatus status(Throwable t) {
        return HttpStatus.valueOf(((ResponseStatusException) t).getStatusCode().value());
    }

    private ProntuarioRegistroService.DadosExame exame(Long idEvento, LocalDate coleta, LocalDate resultado) {
        return new ProntuarioRegistroService.DadosExame(idEvento, " Hemograma ", "Lab X", coleta, resultado, "Normal", null);
    }

    // ------------------------------------------------------------- orientações

    @Test
    void criarOrientacao_veterinarioResponsavelSalvaEAudita() {
        when(eventoSaudeRepository.findById(100L)).thenReturn(Optional.of(evento(StatusEvento.CONCLUIDO)));
        when(orientacaoRepository.save(any(OrientacaoClinica.class))).thenAnswer(i -> i.getArgument(0));

        OrientacaoClinica o = service.criarOrientacao(100L, "  Repouso  ", " Evitar escadas por 7 dias ", "ANA@vet.com");

        assertThat(o.getDsTitulo()).isEqualTo("Repouso");
        assertThat(o.getDsTexto()).isEqualTo("Evitar escadas por 7 dias");
        assertThat(o.getVeterinario()).isSameAs(ana);
        assertThat(o.getDtCriacao()).isNotNull();
        verify(auditoriaService).registrarAcao(eq(AuditoriaTipos.PRONTUARIO), eq(10L), eq("ORIENTACAO_CRIADA"),
                isNull(), isNull(), eq("Repouso"), any(), any());
    }

    @Test
    void criarOrientacao_outroVeterinarioRecebe403() {
        when(eventoSaudeRepository.findById(100L)).thenReturn(Optional.of(evento(StatusEvento.CONCLUIDO)));

        assertThatThrownBy(() -> service.criarOrientacao(100L, "t", "x", "outro@vet.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.FORBIDDEN));
        verify(orientacaoRepository, never()).save(any());
    }

    @Test
    void criarOrientacao_eventoCanceladoRecebe409() {
        when(eventoSaudeRepository.findById(100L)).thenReturn(Optional.of(evento(StatusEvento.CANCELADO)));

        assertThatThrownBy(() -> service.criarOrientacao(100L, "t", "x", "ana@vet.com"))
                .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void criarOrientacao_tituloEmBrancoRecebe400() {
        when(eventoSaudeRepository.findById(100L)).thenReturn(Optional.of(evento(StatusEvento.CONCLUIDO)));

        assertThatThrownBy(() -> service.criarOrientacao(100L, "   ", "texto", "ana@vet.com"))
                .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(orientacaoRepository, never()).save(any());
    }

    @Test
    void removerOrientacao_soOVeterinarioResponsavel() {
        when(eventoSaudeRepository.findById(100L)).thenReturn(Optional.of(evento(StatusEvento.CONCLUIDO)));

        assertThatThrownBy(() -> service.removerOrientacao(100L, 1L, "outro@vet.com"))
                .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.FORBIDDEN));
        verify(orientacaoRepository, never()).delete(any());
    }

    @Test
    void removerOrientacao_removeEAudita() {
        OrientacaoClinica o = OrientacaoClinica.builder().idOrientacao(1L).dsTitulo("Repouso").build();
        when(eventoSaudeRepository.findById(100L)).thenReturn(Optional.of(evento(StatusEvento.CONCLUIDO)));
        when(orientacaoRepository.findByIdOrientacaoAndEvento_IdEvento(1L, 100L)).thenReturn(Optional.of(o));

        service.removerOrientacao(100L, 1L, "ana@vet.com");

        verify(orientacaoRepository).delete(o);
        verify(auditoriaService).registrarAcao(eq(AuditoriaTipos.PRONTUARIO), eq(10L), eq("ORIENTACAO_REMOVIDA"),
                isNull(), eq("Repouso"), isNull(), any(), any());
    }

    // ------------------------------------------------------------------ exames

    @Test
    void registrarExame_salvaComDadosLimposEAudita() {
        when(petRepository.findById(10L)).thenReturn(Optional.of(pet));
        when(veterinarioRepository.findByDsEmail("ana@vet.com")).thenReturn(Optional.of(ana));
        when(eventoSaudeRepository.findById(100L)).thenReturn(Optional.of(evento(StatusEvento.CONCLUIDO)));
        when(exameRepository.save(any(ResultadoExame.class))).thenAnswer(i -> i.getArgument(0));

        ResultadoExame x = service.registrarExame(10L, exame(100L, LocalDate.now().minusDays(2), LocalDate.now()), "ana@vet.com");

        assertThat(x.getNmExame()).isEqualTo("Hemograma");
        assertThat(x.getEvento().getIdEvento()).isEqualTo(100L);
        assertThat(x.getVeterinario()).isSameAs(ana);
        verify(auditoriaService).registrarAcao(eq(AuditoriaTipos.PRONTUARIO), eq(10L), eq("EXAME_REGISTRADO"),
                isNull(), isNull(), eq("Hemograma"), any(), any());
    }

    @Test
    void registrarExame_dataDoResultadoFuturaRecebe400() {
        when(petRepository.findById(10L)).thenReturn(Optional.of(pet));
        when(veterinarioRepository.findByDsEmail("ana@vet.com")).thenReturn(Optional.of(ana));

        assertThatThrownBy(() -> service.registrarExame(10L, exame(null, null, LocalDate.now().plusDays(1)), "ana@vet.com"))
                .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(exameRepository, never()).save(any());
    }

    @Test
    void registrarExame_coletaDepoisDoResultadoRecebe400() {
        when(petRepository.findById(10L)).thenReturn(Optional.of(pet));
        when(veterinarioRepository.findByDsEmail("ana@vet.com")).thenReturn(Optional.of(ana));

        assertThatThrownBy(() -> service.registrarExame(10L,
                exame(null, LocalDate.now(), LocalDate.now().minusDays(1)), "ana@vet.com"))
                .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void registrarExame_eventoDeOutroPetRecebe400() {
        Pet outro = Pet.builder().idPet(99L).build();
        EventoSaude deOutroPet = EventoSaude.builder().idEvento(200L).pet(outro).veterinario(ana).build();
        when(petRepository.findById(10L)).thenReturn(Optional.of(pet));
        when(veterinarioRepository.findByDsEmail("ana@vet.com")).thenReturn(Optional.of(ana));
        when(eventoSaudeRepository.findById(200L)).thenReturn(Optional.of(deOutroPet));

        assertThatThrownBy(() -> service.registrarExame(10L, exame(200L, null, LocalDate.now()), "ana@vet.com"))
                .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(exameRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- arquivos

    private ResultadoExame exameDaAna() {
        return ResultadoExame.builder().idResultado(5L).pet(pet).veterinario(ana).nmExame("Hemograma")
                .dtResultado(LocalDate.now()).build();
    }

    @Test
    void anexarArquivo_pdfValidoComNomeSaneado() {
        ResultadoExame x = exameDaAna();
        when(exameRepository.findByIdResultadoAndPet_IdPet(5L, 10L)).thenReturn(Optional.of(x));
        when(exameRepository.save(any(ResultadoExame.class))).thenAnswer(i -> i.getArgument(0));
        byte[] pdf = "%PDF-1.4 conteudo".getBytes(StandardCharsets.ISO_8859_1);
        MultipartFile arquivo = new MockMultipartFile("arquivo", "..\\..\\etc/laudo\".pdf", "application/pdf", pdf);

        ResultadoExame salvo = service.anexarArquivo(10L, 5L, arquivo, "ana@vet.com");

        assertThat(salvo.getNmArquivo()).isEqualTo("laudo.pdf");
        assertThat(salvo.getDsMimeType()).isEqualTo("application/pdf");
        assertThat(salvo.getNrTamanho()).isEqualTo(pdf.length);
        assertThat(salvo.temArquivo()).isTrue();
    }

    @Test
    void anexarArquivo_tipoNaoPermitidoRecebe415() {
        when(exameRepository.findByIdResultadoAndPet_IdPet(5L, 10L)).thenReturn(Optional.of(exameDaAna()));
        MultipartFile svg = new MockMultipartFile("arquivo", "x.svg", "image/svg+xml", "<svg/>".getBytes());

        assertThatThrownBy(() -> service.anexarArquivo(10L, 5L, svg, "ana@vet.com"))
                .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
        verify(exameRepository, never()).save(any());
    }

    @Test
    void anexarArquivo_pdfFalsoRecebe415() {
        when(exameRepository.findByIdResultadoAndPet_IdPet(5L, 10L)).thenReturn(Optional.of(exameDaAna()));
        MultipartFile falso = new MockMultipartFile("arquivo", "x.pdf", "application/pdf", "nao sou pdf".getBytes());

        assertThatThrownBy(() -> service.anexarArquivo(10L, 5L, falso, "ana@vet.com"))
                .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    void anexarArquivo_grandeDemaisRecebe413() {
        when(exameRepository.findByIdResultadoAndPet_IdPet(5L, 10L)).thenReturn(Optional.of(exameDaAna()));
        MultipartFile grande = mock(MultipartFile.class);
        when(grande.isEmpty()).thenReturn(false);
        when(grande.getSize()).thenReturn(ProntuarioRegistroService.TAMANHO_MAXIMO_ARQUIVO + 1);

        assertThatThrownBy(() -> service.anexarArquivo(10L, 5L, grande, "ana@vet.com"))
                .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @Test
    void anexarArquivo_outroVeterinarioRecebe403() {
        when(exameRepository.findByIdResultadoAndPet_IdPet(5L, 10L)).thenReturn(Optional.of(exameDaAna()));
        MultipartFile pdf = new MockMultipartFile("arquivo", "x.pdf", "application/pdf", "%PDF-1.7".getBytes());

        assertThatThrownBy(() -> service.anexarArquivo(10L, 5L, pdf, "outro@vet.com"))
                .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void buscarExameComArquivo_semArquivoRecebe404() {
        when(exameRepository.findByIdResultadoAndPet_IdPet(5L, 10L)).thenReturn(Optional.of(exameDaAna()));

        assertThatThrownBy(() -> service.buscarExameComArquivo(10L, 5L))
                .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void removerExame_soQuemRegistrou() {
        ResultadoExame x = exameDaAna();
        when(exameRepository.findByIdResultadoAndPet_IdPet(5L, 10L)).thenReturn(Optional.of(x));

        assertThatThrownBy(() -> service.removerExame(10L, 5L, "outro@vet.com"))
                .satisfies(e -> assertThat(status(e)).isEqualTo(HttpStatus.FORBIDDEN));
        verify(exameRepository, never()).delete(any());

        service.removerExame(10L, 5L, "ANA@vet.com");
        verify(exameRepository).delete(x);
    }

    @Test
    void nomeSeguro_removeCaminhoAspasEControle() {
        assertThat(ProntuarioRegistroService.nomeSeguro("C:\\temp\\laudo.pdf")).isEqualTo("laudo.pdf");
        assertThat(ProntuarioRegistroService.nomeSeguro("a/b/\"x\".png")).isEqualTo("x.png");
        assertThat(ProntuarioRegistroService.nomeSeguro("  ")).isEqualTo("exame");
        assertThat(ProntuarioRegistroService.nomeSeguro(null)).isEqualTo("exame");
    }
}