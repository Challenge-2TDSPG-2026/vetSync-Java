package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "TB_BLOQUEIO_AGENDA_ESTETICA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloqueioAgendaEstetica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bloqueio")
    private Long idBloqueio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_profissional_estetica", nullable = false)
    private ProfissionalEstetica profissionalEstetica;

    @Column(name = "dt_inicio", nullable = false)
    private LocalDate dtInicio;

    @Column(name = "dt_fim", nullable = false)
    private LocalDate dtFim;

    @Column(name = "ds_motivo", length = 200)
    private String dsMotivo;
}