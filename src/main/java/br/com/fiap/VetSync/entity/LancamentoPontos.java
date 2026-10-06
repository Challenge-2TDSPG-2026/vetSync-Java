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

    /** Dia em que o admin liberou os pontos. */
    @Column(name = "dt_liberacao")
    private LocalDate dtLiberacao;

    /** Último dia em que os pontos ainda valem (inclusive). Nulo enquanto não liberado. */
    @Column(name = "dt_validade")
    private LocalDate dtValidade;

    @Column(name = "dt_bloqueio")
    private LocalDate dtBloqueio;

    @Column(name = "ds_motivo_bloqueio", length = 300)
    private String dsMotivoBloqueio;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin_bloqueio")
    private Admin adminBloqueio;

    /** Status considerando a validade: um LIBERADO vencido é EXPIRADO. */
    public StatusLancamentoPontos statusEfetivo(LocalDate hoje) {
        if (dsStatus == StatusLancamentoPontos.LIBERADO && venceu(hoje)) {
            return StatusLancamentoPontos.EXPIRADO;
        }
        return dsStatus;
    }

    /** Só é resgatável se estiver LIBERADO, dentro da validade e pertencer a uma clínica. */
    public boolean resgatavel(LocalDate hoje) {
        return clinica != null && statusEfetivo(hoje) == StatusLancamentoPontos.LIBERADO;
    }

    public boolean venceu(LocalDate hoje) {
        return dtValidade != null && dtValidade.isBefore(hoje);
    }

    public Pet petOrigem() {
        if (evento != null) return evento.getPet();
        return planoTratamento != null ? planoTratamento.getPet() : null;
    }

    public Tutor tutorOrigem() {
        Pet pet = petOrigem();
        return pet != null ? pet.getTutor() : null;
    }
}