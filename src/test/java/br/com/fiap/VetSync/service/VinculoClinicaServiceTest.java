package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.repository.ClinicaRepository;
import br.com.fiap.VetSync.repository.CodigoVinculoClinicaRepository;
import br.com.fiap.VetSync.repository.EventoSaudeRepository;
import br.com.fiap.VetSync.repository.SessaoVinculoClinicaRepository;
import br.com.fiap.VetSync.repository.TutorRepository;
import br.com.fiap.VetSync.repository.VinculoTutorClinicaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VinculoClinicaServiceTest {

    @Mock
    private ClinicaRepository clinicaRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private EventoSaudeRepository eventoRepository;

    @Mock
    private CodigoVinculoClinicaRepository codigoRepository;

    @Mock
    private SessaoVinculoClinicaRepository sessaoRepository;

    @Mock
    private VinculoTutorClinicaRepository vinculoRepository;

    @InjectMocks
    private VinculoClinicaService vinculoService;

    @Test
    @DisplayName("Clínica nova nasce com contrato inativo")
    void clinicaNova_NasceInativa() {
        Clinica clinica = Clinica.builder().nmClinica("Clínica Nova").dsCnpj("12345678000190").build();

        assertThat(clinica.estaContratanteAtiva()).isFalse();
    }

    @Test
    @DisplayName("Não deve emitir código para clínica com contrato inativo")
    void emitirCodigo_ClinicaInativa_Conflito() {
        Clinica inativa = Clinica.builder().idClinica(1L).nmClinica("Clínica Nova").stContratante("I").build();
        when(clinicaRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(inativa));

        assertThatThrownBy(() -> vinculoService.emitirCodigo(1L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(codigoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Deve emitir código de 18 caracteres para clínica com contrato ativo")
    void emitirCodigo_ClinicaAtiva_Sucesso() {
        Clinica ativa = Clinica.builder().idClinica(2L).nmClinica("Clínica Ativa").stContratante("A").build();
        when(clinicaRepository.findByIdParaAtualizacao(2L)).thenReturn(Optional.of(ativa));
        when(codigoRepository.findByClinica_IdClinicaAndStAtivo(2L, "A")).thenReturn(List.of());

        var emitido = vinculoService.emitirCodigo(2L);

        assertThat(emitido.codigo()).hasSize(18).matches("[A-HJ-NP-Z2-9]+");
        assertThat(emitido.clinica()).isEqualTo(ativa);
        verify(codigoRepository).saveAndFlush(any());
    }
}