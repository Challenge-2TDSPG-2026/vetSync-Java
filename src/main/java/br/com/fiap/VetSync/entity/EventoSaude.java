package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "TB_EVENTO_SAUDE")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoSaude {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Long idEvento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pet", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_evento", nullable = false)
    private TipoEvento tipoEvento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_veterinario")
    private Veterinario veterinario;

    @NotNull(message = "Data do evento é obrigatória")
    @Column(name = "dt_evento", nullable = false)
    private LocalDate dtEvento;

    @Column(name = "hr_evento", length = 5)
    private String hrEvento;

    @Column(name = "ds_observacao", length = 500)
    private String dsObservacao;

    @Builder.Default
    @Column(name = "vl_custo", precision = 10, scale = 2)
    private BigDecimal vlCusto = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "ds_status", nullable = false, length = 20)
    private StatusEvento dsStatus = StatusEvento.AGENDADO;

    @Column(name = "ds_motivo_cancelamento", length = 300)
    private String dsMotivoCancelamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_profissional_estetica")
    private ProfissionalEstetica profissionalEstetica;
}
