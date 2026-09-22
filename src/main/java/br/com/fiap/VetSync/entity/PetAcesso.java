package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_PET_ACESSO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_acesso")
    private Long idAcesso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pet", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tutor", nullable = false)
    private Tutor tutor;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "ds_relacao", nullable = false, length = 20)
    private RelacaoPet dsRelacao;

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

    @Transient
    public boolean isAtivo() {
        return dsStatus == StatusAcessoPet.ATIVO;
    }
}