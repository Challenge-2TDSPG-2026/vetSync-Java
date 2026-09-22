package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_PET_CONVITE")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetConvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_convite")
    private Long idConvite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pet", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tutor_origem", nullable = false)
    private Tutor tutorOrigem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tutor_destino")
    private Tutor tutorDestino;

    @NotNull
    @Column(name = "ds_email_destino", nullable = false, length = 150)
    private String dsEmailDestino;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "ds_relacao", nullable = false, length = 20)
    private RelacaoPet dsRelacao;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "ds_permissao", nullable = false, length = 10)
    private PermissaoPet dsPermissao;

    @NotNull
    @Column(name = "ds_token_hash", nullable = false, unique = true, length = 64)
    private String dsTokenHash;

    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "ds_status", nullable = false, length = 15)
    private StatusConvitePet dsStatus = StatusConvitePet.PENDENTE;

    @NotNull
    @Column(name = "dt_criacao", nullable = false)
    private LocalDateTime dtCriacao;

    @NotNull
    @Column(name = "dt_expiracao", nullable = false)
    private LocalDateTime dtExpiracao;

    @Column(name = "dt_aceite")
    private LocalDateTime dtAceite;

    @Transient
    public boolean isExpirado() {
        return dtExpiracao == null || !dtExpiracao.isAfter(LocalDateTime.now());
    }
}