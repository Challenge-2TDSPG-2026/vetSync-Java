package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_DISPOSITIVO_PUSH",
        uniqueConstraints = @UniqueConstraint(name = "uk_dispositivo_usuario_token_plat",
                columnNames = {"id_tutor", "ds_token", "ds_plataforma"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispositivoPush {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dispositivo")
    private Long idDispositivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tutor", nullable = false)
    private Tutor tutor;

    @NotBlank
    @Column(name = "ds_token", nullable = false, length = 255)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "ds_plataforma", nullable = false, length = 20)
    private PlataformaPush plataforma;

    @Column(name = "nm_dispositivo", length = 100)
    private String nomeDispositivo;

    @Column(name = "ds_fuso_horario", length = 80)
    private String fusoHorario;

    @Builder.Default
    @Column(name = "fl_ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "dt_ultimo_uso")
    private LocalDateTime ultimoUsoEm;

    @Column(name = "dt_criacao", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
        if (ultimoUsoEm == null) ultimoUsoEm = criadoEm;
    }
}
