package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "TB_PLANO_TRATAMENTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanoTratamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plano")
    private Long idPlano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pet", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_veterinario", nullable = false)
    private Veterinario veterinario;

    @Builder.Default
    @Column(name = "nr_pontos_bonus", nullable = false)
    private Integer nrPontosBonus = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "ds_status", nullable = false, length = 20)
    private StatusPlanoTratamento dsStatus = StatusPlanoTratamento.EM_ANDAMENTO;

    @Builder.Default
    @Column(name = "dt_criacao", nullable = false)
    private LocalDate dtCriacao = LocalDate.now();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clinica")
    private Clinica clinica;
}