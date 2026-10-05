package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.Clinica;
import br.com.fiap.VetSync.entity.Medicamento;
import br.com.fiap.VetSync.repository.MedicamentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicamentoServiceTest {

    @Mock
    private MedicamentoRepository medicamentoRepository;

    @Mock
    private ClinicaService clinicaService;

    @InjectMocks
    private MedicamentoService medicamentoService;

    private final Clinica clinica = Clinica.builder().idClinica(7L).nmClinica("Clínica Centro").build();

    @Test
    @DisplayName("Deve criar medicamento com sucesso")
    void criar_Sucesso() {
        when(clinicaService.buscarObrigatoria(7L)).thenReturn(clinica);
        when(medicamentoRepository.save(any(Medicamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Medicamento m = medicamentoService.criar("Vermífugo Plus", "Praziquantel", new BigDecimal("45.00"), 7L);
        assertThat(m.getClinica()).isEqualTo(clinica);
        assertThat(m.getNmMedicamento()).isEqualTo("Vermífugo Plus");
        assertThat(m.getDsPrincipio()).isEqualTo("Praziquantel");
        assertThat(m.getVlPrecoRef()).isEqualByComparingTo("45.00");
    }

    @Test
    @DisplayName("Deve buscar medicamento por ID com sucesso")
    void buscarPorId_Sucesso() {
        Medicamento m = Medicamento.builder().idMedicamento(1L).nmMedicamento("Antipulgas").build();
        when(medicamentoRepository.findById(1L)).thenReturn(Optional.of(m));

        Medicamento encontrado = medicamentoService.buscarPorId(1L);
        assertThat(encontrado.getNmMedicamento()).isEqualTo("Antipulgas");
    }

    @Test
    @DisplayName("Deve lançar 404 ao buscar medicamento inexistente")
    void buscarPorId_NaoEncontrado() {
        when(medicamentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicamentoService.buscarPorId(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Medicamento não encontrado");
    }

    @Test
    @DisplayName("Deve listar todos os medicamentos")
    void listar_Sucesso() {
        when(medicamentoRepository.findAll()).thenReturn(List.of(
                Medicamento.builder().idMedicamento(1L).build(),
                Medicamento.builder().idMedicamento(2L).build()
        ));

        List<Medicamento> lista = medicamentoService.listar();
        assertThat(lista).hasSize(2);
    }

    @Test
    @DisplayName("Deve atualizar medicamento com sucesso")
    void atualizar_Sucesso() {
        Medicamento existente = Medicamento.builder().idMedicamento(1L).nmMedicamento("Nome Antigo").build();
        when(medicamentoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clinicaService.buscarObrigatoria(7L)).thenReturn(clinica);
        when(medicamentoRepository.save(any(Medicamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Medicamento atualizado = medicamentoService.atualizar(1L, "Nome Novo", "Novo Princípio", new BigDecimal("60.00"), 7L);
        assertThat(atualizado.getClinica()).isEqualTo(clinica);
        assertThat(atualizado.getNmMedicamento()).isEqualTo("Nome Novo");
        assertThat(atualizado.getDsPrincipio()).isEqualTo("Novo Princípio");
    }

    @Test
    @DisplayName("Deve deletar medicamento com sucesso")
    void deletar_Sucesso() {
        Medicamento m = Medicamento.builder().idMedicamento(1L).build();
        when(medicamentoRepository.findById(1L)).thenReturn(Optional.of(m));

        medicamentoService.deletar(1L);
        verify(medicamentoRepository).delete(m);
    }
}