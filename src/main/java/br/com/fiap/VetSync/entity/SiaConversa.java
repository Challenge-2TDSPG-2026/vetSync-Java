package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_SIA_CONVERSA")
@Getter
@Setter
public class SiaConversa {
    @Id
    @Column(name = "id_conversa", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pet")
    private Pet pet;

    @Column(name = "ds_tutor_email", nullable = false, length = 150)
    private String tutorEmail;

    @Column(name = "dt_criacao", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "dt_atualizacao", nullable = false)
    private LocalDateTime atualizadoEm;
}
