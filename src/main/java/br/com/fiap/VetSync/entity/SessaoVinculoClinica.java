package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_SESSAO_VINCULO_CLINICA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessaoVinculoClinica {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sessao_vinculo")
    private Long idSessaoVinculo;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_clinica", nullable = false)
    private Clinica clinica;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_codigo_vinculo")
    private CodigoVinculoClinica codigoVinculo;

    @Column(name = "ds_token_hash", nullable = false, unique = true, length = 64)
    private String dsTokenHash;

    @Column(name = "dt_expiracao", nullable = false)
    private LocalDateTime dtExpiracao;

    @Column(name = "dt_utilizacao")
    private LocalDateTime dtUtilizacao;

    @Column(name = "dt_criacao", nullable = false) @Builder.Default
    private LocalDateTime dtCriacao = LocalDateTime.now();

    public boolean estaDisponivel() {
        return dtUtilizacao == null && dtExpiracao.isAfter(LocalDateTime.now());
    }
}