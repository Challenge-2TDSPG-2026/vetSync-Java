package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/** Registro de auditoria. Append-only: {@link Immutable} faz o Hibernate recusar UPDATE e DELETE. */
@Entity
@Immutable
@Table(name = "TB_AUDITORIA", indexes = {
        @Index(name = "ix_auditoria_entidade", columnList = "ds_entidade,id_entidade,dt_ocorrencia"),
        @Index(name = "ix_auditoria_data", columnList = "dt_ocorrencia"),
        @Index(name = "ix_auditoria_clinica", columnList = "id_clinica,dt_ocorrencia"),
        @Index(name = "ix_auditoria_acao", columnList = "ds_acao,dt_ocorrencia")
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
    /** Clínica em cujo contexto a ação ocorreu (nula para ações sem clínica). */
    @Column(name = "id_clinica")
    private Long idClinica;
    /** Nome da clínica na época da ação (cópia: renomear a clínica não reescreve o histórico). */
    @Column(name = "nm_clinica", length = 150)
    private String nmClinica;
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