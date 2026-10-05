package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;


@Entity
@Table(name = "TB_LANCAMENTO_PONTOS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LancamentoPontos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lancamento")
    private Long idLancamento;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", unique = true)
    private EventoSaude evento;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plano_tratamento", unique = true)
    private PlanoTratamento planoTratamento;

    @Column(name = "nr_pontos", nullable = false)
    private Integer nrPontos;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "ds_status", nullable = false, length = 20)
    private StatusLancamentoPontos dsStatus = StatusLancamentoPontos.PENDENTE;

    @Builder.Default
    @Column(name = "dt_lancamento", nullable = false)
    private LocalDate dtLancamento = LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin_validador")
    private Admin adminValidador;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clinica")
    private Clinica clinica;
}