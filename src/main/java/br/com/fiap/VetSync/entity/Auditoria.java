package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_AUDITORIA", indexes = {
        @Index(name = "ix_auditoria_entidade", columnList = "ds_entidade,id_entidade,dt_ocorrencia"),
        @Index(name = "ix_auditoria_data", columnList = "dt_ocorrencia")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Auditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;
    @Column(name = "ds_entidade", nullable = false, length = 40)
    private String dsEntidade;
    @Column(name = "id_entidade", nullable = false)
    private Long idEntidade;
    @Column(name = "ds_acao", nullable = false, length = 60)
    private String dsAcao;
    @Column(name = "ds_ator", length = 150)
    private String dsAtor;
    @Column(name = "ds_perfil", length = 40)
    private String dsPerfil;
    @Lob
    @Column(name = "ds_valor_anterior")
    private String dsValorAnterior;
    @Lob
    @Column(name = "ds_valor_novo")
    private String dsValorNovo;
    @Column(name = "ds_ip", length = 64)
    private String dsIp;
    @Column(name = "dt_ocorrencia", nullable = false)
    private LocalDateTime dtOcorrencia;
}
