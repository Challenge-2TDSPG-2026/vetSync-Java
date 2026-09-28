package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_NOTIFICACAO")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacao")
    private Long idNotificacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tutor", nullable = false)
    private Tutor tutor;

    @Enumerated(EnumType.STRING)
    @Column(name = "ds_tipo", nullable = false, length = 40)
    private TipoNotificacao tipo;

    @Column(name = "ds_titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "ds_mensagem", nullable = false, length = 500)
    private String mensagem;

    @Column(name = "ds_referencia_tipo", length = 50)
    private String referenciaTipo;

    @Column(name = "id_referencia")
    private Long referenciaId;

    @Builder.Default
    @Column(name = "fl_lida", nullable = false)
    private boolean lida = false;

    @Column(name = "dt_enviada")
    private LocalDateTime enviadaEm;

    @Column(name = "dt_criacao", nullable = false)
    private LocalDateTime criadaEm;

    @PrePersist
    void prePersist() {
        if (criadaEm == null) criadaEm = LocalDateTime.now();
    }
}
