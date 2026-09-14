package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TB_DISPONIBILIDADE_ESTETICA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadeEstetica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_disponibilidade")
    private Long idDisponibilidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_profissional_estetica", nullable = false)
    private ProfissionalEstetica profissionalEstetica;

    @Column(name = "nr_dia_semana", nullable = false)
    private Integer nrDiaSemana;

    @Column(name = "hr_inicio", nullable = false, length = 5)
    private String hrInicio;

    @Column(name = "hr_fim", nullable = false, length = 5)
    private String hrFim;
}