package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TB_HORARIO_CLINICA")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HorarioClinica {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario")
    private Long idHorario;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clinica", nullable = false)
    private Clinica clinica;
    @Column(name = "nr_dia_semana", nullable = false)
    private Integer nrDiaSemana;
    @Column(name = "hr_inicio", nullable = false, length = 5)
    private String hrInicio;
    @Column(name = "hr_fim", nullable = false, length = 5)
    private String hrFim;
}
