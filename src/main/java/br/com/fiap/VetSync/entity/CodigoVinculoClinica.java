package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_CODIGO_VINCULO_CLINICA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodigoVinculoClinica {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_codigo_vinculo")
    private Long idCodigoVinculo;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_clinica", nullable = false)
    private Clinica clinica;

    @Column(name = "ds_codigo_hash", nullable = false, unique = true, length = 64)
    private String dsCodigoHash;

    @Builder.Default @Column(name = "st_ativo", nullable = false, length = 1)
    private String stAtivo = "A";

    @Column(name = "dt_criacao", nullable = false) @Builder.Default
    private LocalDateTime dtCriacao = LocalDateTime.now();

    @Column(name = "dt_revogacao")
    private LocalDateTime dtRevogacao;

    public boolean estaAtivo() { return "A".equals(stAtivo) && dtRevogacao == null; }
}
