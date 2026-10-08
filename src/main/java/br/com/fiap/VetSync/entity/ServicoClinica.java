package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TB_SERVICO_CLINICA")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ServicoClinica {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servico_clinica")
    private Long idServicoClinica;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clinica", nullable = false)
    private Clinica clinica;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_evento", nullable = false)
    private TipoEvento tipoEvento;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servico_base")
    private ServicoEstetica baseEstetica;
    @Column(name = "nm_servico", nullable = false, length = 80)
    private String nmServico;
    @Column(name = "ds_categoria", nullable = false, length = 30)
    private String dsCategoria;
    @Column(name = "nr_duracao_minutos", nullable = false)
    private Integer nrDuracaoMinutos;
    @Builder.Default
    @Column(name = "fl_ativo", nullable = false)
    private Boolean ativo = true;
}
