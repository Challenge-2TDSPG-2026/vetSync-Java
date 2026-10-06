package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_RESGATE")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resgate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resgate")
    private Long idResgate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tutor", nullable = false)
    private Tutor tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_recompensa", nullable = false)
    private Recompensa recompensa;

    // ---- Cópia da recompensa no momento do resgate (não muda se o catálogo for editado depois) ----

    @Column(name = "nm_recompensa", nullable = false, length = 150)
    private String nmRecompensa;

    @Column(name = "ds_descricao_recompensa", length = 500)
    private String dsDescricaoRecompensa;

    @Enumerated(EnumType.STRING)
    @Column(name = "ds_tipo_recompensa", length = 20)
    private TipoRecompensa dsTipoRecompensa;

    /** Custo em pontos aplicado no momento do resgate. É este valor que entra no cálculo do saldo. */
    @Column(name = "nr_custo_pontos", nullable = false)
    private Integer nrCustoPontos;

    /** Clínica da recompensa no momento do resgate: é nela que os pontos foram gastos. */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clinica")
    private Clinica clinica;

    @Builder.Default
    @Column(name = "dt_resgate", nullable = false)
    private LocalDateTime dtResgate = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_veterinario_validador")
    private Veterinario veterinarioValidador;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "ds_status", nullable = false, length = 20)
    private StatusResgate dsStatus = StatusResgate.PENDENTE;

    /** Rede de segurança: se a cópia não foi preenchida, congela os dados atuais da recompensa ao gravar. */
    @PrePersist
    void congelarDadosDaRecompensa() {
        if (recompensa == null) return;
        if (nmRecompensa == null) nmRecompensa = recompensa.getNmRecompensa();
        if (dsDescricaoRecompensa == null) dsDescricaoRecompensa = recompensa.getDsDescricao();
        if (dsTipoRecompensa == null) dsTipoRecompensa = recompensa.getDsTipo();
        if (nrCustoPontos == null) nrCustoPontos = recompensa.getNrCustoPontos();
        if (clinica == null) clinica = recompensa.getClinica();
    }

    // Leituras com fallback para a recompensa (dados legados ou objetos ainda não gravados).

    public int custoAplicado() {
        if (nrCustoPontos != null) return nrCustoPontos;
        return recompensa != null && recompensa.getNrCustoPontos() != null ? recompensa.getNrCustoPontos() : 0;
    }

    public String nomeRecompensa() {
        return nmRecompensa != null ? nmRecompensa : (recompensa != null ? recompensa.getNmRecompensa() : null);
    }

    public String descricaoRecompensa() {
        return dsDescricaoRecompensa != null ? dsDescricaoRecompensa : (recompensa != null ? recompensa.getDsDescricao() : null);
    }

    public TipoRecompensa tipoRecompensa() {
        return dsTipoRecompensa != null ? dsTipoRecompensa : (recompensa != null ? recompensa.getDsTipo() : null);
    }

    public Clinica clinicaOrigem() {
        return clinica != null ? clinica : (recompensa != null ? recompensa.getClinica() : null);
    }
}