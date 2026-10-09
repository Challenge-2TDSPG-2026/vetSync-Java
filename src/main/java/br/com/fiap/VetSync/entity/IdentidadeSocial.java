package br.com.fiap.VetSync.entity;

import br.com.fiap.VetSync.social.SocialProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_IDENTIDADE_SOCIAL", uniqueConstraints = {
        @UniqueConstraint(name = "uk_identidade_provedor_subject", columnNames = {"ds_provedor", "ds_subject"}),
        @UniqueConstraint(name = "uk_identidade_tutor_provedor", columnNames = {"id_tutor", "ds_provedor"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentidadeSocial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_identidade_social")
    private Long idIdentidadeSocial;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tutor", nullable = false)
    private Tutor tutor;

    @Enumerated(EnumType.STRING)
    @Column(name = "ds_provedor", nullable = false, length = 20)
    private SocialProvider provedor;

    @Column(name = "ds_subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "ds_email_provedor", length = 150)
    private String dsEmailProvedor;

    @Builder.Default
    @Column(name = "dt_vinculo", nullable = false)
    private LocalDateTime dtVinculo = LocalDateTime.now();
}