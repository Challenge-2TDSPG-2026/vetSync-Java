package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "TB_TIPO_EVENTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_evento")
    private Long idTipoEvento;

    @NotBlank(message = "Nome do tipo de evento é obrigatório")
    @Column(name = "nm_tipo_evento", nullable = false, length = 80)
    private String nmTipoEvento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clinica")
    private Clinica clinica;

    @Column(name = "ds_categoria", length = 30)
    private String dsCategoria;

    /**
     * Identifica fluxos de agendamento que não podem depender do nome exibido
     * do tipo de evento. Ex.: CLINICO_GERAL.
     */
    @Column(name = "ds_modalidade_agendamento", length = 30)
    private String dsModalidadeAgendamento;

    @Builder.Default
    @Column(name = "nr_duracao_minutos", nullable = false)
    private Integer nrDuracaoMinutos = 30;

    @Builder.Default
    @Column(name = "nr_pontos", nullable = false)
    private Integer nrPontos = 0;
}
