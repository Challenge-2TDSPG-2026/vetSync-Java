package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_ACAO_IA")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcaoIa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_acao")
    private Long idAcao;

    @Column(name = "ds_acao", nullable = false, length = 40)
    private String acao;

    @Column(name = "ds_resumo", nullable = false, length = 500)
    private String resumo;

    @Lob
    @Column(name = "ds_dados", nullable = false)
    private String dados;

    @Enumerated(EnumType.STRING)
    @Column(name = "ds_status", nullable = false, length = 20)
    private StatusAcaoIa status;

    @Column(name = "ds_email_usuario", nullable = false, length = 150)
    private String emailUsuario;

    @Column(name = "dt_expiracao", nullable = false)
    private LocalDateTime expiracao;

    @Column(name = "dt_criacao", nullable = false)
    private LocalDateTime criacao;
}
