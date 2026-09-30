package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clinica")
    private Clinica clinica;

    @NotNull(message = "Data do evento é obrigatória")
    @Column(name = "dt_evento", nullable = false)
    private LocalDate dtEvento;

    @Column(name = "hr_evento", length = 5)
    private String hrEvento;

    @Column(name = "ds_observacao", length = 500)
    private String dsObservacao;

    @Column(name = "ds_observacao_tutor", length = 500)
    private String dsObservacaoTutor;

    @Column(name = "ds_observacao_clinica", length = 1000)
    private String dsObservacaoClinica;

    @Column(name = "ds_diagnostico", length = 500)
    private String dsDiagnostico;

    @Column(name = "ds_conduta", length = 1000)
    private String dsConduta;

    @Column(name = "dt_criacao", nullable = false)
    @Builder.Default
    private LocalDateTime dtCriacao = LocalDateTime.now();

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "TB_EVENTO_SERVICO",
            joinColumns = @JoinColumn(name = "id_evento"),
            inverseJoinColumns = @JoinColumn(name = "id_servico")
    )
    @Builder.Default
    private Set<ServicoEstetica> servicos = new HashSet<>();
}