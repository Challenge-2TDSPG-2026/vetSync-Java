package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TB_PREFERENCIA_NOTIFICACAO")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciaNotificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_preferencia")
    private Long idPreferencia;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tutor", nullable = false, unique = true)
    private Tutor tutor;

    @Builder.Default
    @Column(name = "fl_push_ativo", nullable = false)
    private boolean pushAtivo = true;

    @Builder.Default
    @Column(name = "fl_lembrete_sete_dias", nullable = false)
    private boolean lembreteSeteDias = true;

    @Builder.Default
    @Column(name = "fl_lembrete_um_dia", nullable = false)
    private boolean lembreteUmDia = true;

    @Builder.Default
    @Column(name = "fl_lembrete_duas_horas", nullable = false)
    private boolean lembreteDuasHoras = false;

    @Builder.Default
    @Column(name = "fl_vacinas_vencendo", nullable = false)
    private boolean vacinasVencendo = true;

    @Builder.Default
    @Column(name = "fl_retornos_pendentes", nullable = false)
    private boolean retornosPendentes = true;

    @Builder.Default
    @Column(name = "fl_convites_acesso", nullable = false)
    private boolean convitesDeAcesso = true;

    @Builder.Default
    @Column(name = "fl_resgates", nullable = false)
    private boolean resgates = true;
}
