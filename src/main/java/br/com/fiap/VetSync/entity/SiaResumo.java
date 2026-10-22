package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_SIA_RESUMO")
@Getter
@Setter
public class SiaResumo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resumo")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pet")
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento")
    private EventoSaude evento;

    @Column(name = "ds_tutor_email", nullable = false, length = 150)
    private String tutorEmail;

    @Lob
    @Column(name = "ds_resumo", nullable = false)
    private String resumo;

    @Lob
    @Column(name = "ds_perguntas")
    private String perguntas;

    @Column(name = "ds_origem", nullable = false, length = 30)
    private String origem = "IA_RASCUNHO";

    @Column(name = "dt_criacao", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "dt_compartilhamento")
    private LocalDateTime compartilhadoEm;

    @Column(name = "dt_revogacao")
    private LocalDateTime revogadoEm;
}
