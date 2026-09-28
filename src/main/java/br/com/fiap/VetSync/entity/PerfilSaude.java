package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "TB_PERFIL_SAUDE")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilSaude {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_perfil")
    private Long idPerfil;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pet", nullable = false, unique = true)
    private Pet pet;

    @Column(name = "nr_peso_atual", precision = 5, scale = 2)
    private BigDecimal pesoAtual;

    @Column(name = "dt_peso_atualizado")
    private LocalDate pesoAtualizadoEm;

    @Column(name = "ds_alergias", length = 2000)
    private String alergias;
    @Column(name = "ds_medicamentos_continuos", length = 2000)
    private String medicamentosContinuos;
    @Column(name = "ds_restricoes_alimentares", length = 2000)
    private String restricoesAlimentares;
    @Column(name = "ds_condicoes_pre_existentes", length = 2000)
    private String condicoesPreExistentes;
    @Column(name = "ds_observacoes_importantes", length = 2000)
    private String observacoesImportantes;
    @Column(name = "ds_contato_emergencia", length = 300)
    private String contatoEmergencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_veterinario_preferencial")
    private Veterinario veterinarioPreferencial;
}
