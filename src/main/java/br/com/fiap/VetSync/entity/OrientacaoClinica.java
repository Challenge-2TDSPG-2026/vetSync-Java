package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Orientação passada ao tutor num atendimento (cuidados, retorno, alimentação, sinais de alerta...). */
@Entity
@Table(name = "TB_ORIENTACAO_CLINICA")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrientacaoClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orientacao")
    private Long idOrientacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evento", nullable = false)
    private EventoSaude evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_veterinario")
    private Veterinario veterinario;

    @Column(name = "ds_titulo", nullable = false, length = 120)
    private String dsTitulo;

    @Column(name = "ds_texto", nullable = false, length = 2000)
    private String dsTexto;

    @Column(name = "ds_ator", length = 150)
    private String dsAtor;

    @Column(name = "dt_criacao", nullable = false)
    private LocalDateTime dtCriacao;
}