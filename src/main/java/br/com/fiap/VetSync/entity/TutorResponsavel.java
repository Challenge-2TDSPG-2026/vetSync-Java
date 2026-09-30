package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_TUTOR_RESPONSAVEL")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorResponsavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_responsavel")
    private Long idResponsavel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tutor_proprietario", nullable = false)
    private Tutor tutorProprietario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tutor_responsavel", nullable = false)
    private Tutor tutorResponsavel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "ds_permissao", nullable = false, length = 10)
    private PermissaoPet dsPermissao;

    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "ds_status", nullable = false, length = 10)
    private StatusAcessoPet dsStatus = StatusAcessoPet.ATIVO;

    @NotNull
    @Column(name = "dt_concedido", nullable = false)
    private LocalDateTime dtConcedido;

    @Column(name = "dt_revogado")
    private LocalDateTime dtRevogado;
}
