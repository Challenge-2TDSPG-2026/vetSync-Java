package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_SIA_SESSAO_AGENDAMENTO")
@Getter
@Setter
public class SiaSessaoAgendamento {
    @Id
    @Column(name = "id_sessao", length = 36)
    private String id;

    @Column(name = "ds_tutor_email", nullable = false, length = 150)
    private String tutorEmail;

    @Lob
    @Column(name = "ds_dados", nullable = false)
    private String dados;

    @Column(name = "dt_expiracao", nullable = false)
    private LocalDateTime expiraEm;
}
