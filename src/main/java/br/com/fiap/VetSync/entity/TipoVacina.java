package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Entity
@Table(name = "TB_TIPO_VACINA")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoVacina {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_vacina")
    private Long idTipoVacina;

    @NotBlank
    @Column(name = "nm_tipo_vacina", nullable = false, unique = true, length = 100)
    private String nmTipoVacina;

    @Positive
    @Column(name = "nr_periodicidade_dias", nullable = false)
    private Integer nrPeriodicidadeDias;

    @Builder.Default
    @Column(name = "fl_ativo", nullable = false)
    private Boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clinica")
    private Clinica clinica;
}