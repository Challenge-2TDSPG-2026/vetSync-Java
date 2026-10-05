package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "TB_MEDICAMENTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_medicamento")
    private Long idMedicamento;

    @NotBlank(message = "Nome do medicamento é obrigatório")
    @Column(name = "nm_medicamento", nullable = false, length = 100)
    private String nmMedicamento;

    @Column(name = "ds_principio", length = 100)
    private String dsPrincipio;

    @Column(name = "vl_preco_ref", precision = 10, scale = 2)
    private BigDecimal vlPrecoRef;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clinica")
    private Clinica clinica;
}