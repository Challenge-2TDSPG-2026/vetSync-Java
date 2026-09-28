package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_EVENTO_HISTORICO")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoHistorico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historico")
    private Long idHistorico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evento", nullable = false)
    private EventoSaude evento;

    @Column(name = "ds_acao", nullable = false, length = 40)
    private String dsAcao;
    @Column(name = "ds_status_anterior", length = 20)
    private String dsStatusAnterior;
    @Column(name = "ds_status_novo", length = 20)
    private String dsStatusNovo;
    @Column(name = "ds_observacao_anterior", length = 500)
    private String dsObservacaoAnterior;
    @Column(name = "ds_observacao_nova", length = 500)
    private String dsObservacaoNova;
    @Column(name = "vl_custo_anterior", precision = 10, scale = 2)
    private BigDecimal vlCustoAnterior;
    @Column(name = "vl_custo_novo", precision = 10, scale = 2)
    private BigDecimal vlCustoNovo;
    @Column(name = "dt_evento_anterior")
    private LocalDate dtEventoAnterior;
    @Column(name = "dt_evento_novo")
    private LocalDate dtEventoNovo;
    @Column(name = "hr_evento_anterior", length = 5)
    private String hrEventoAnterior;
    @Column(name = "hr_evento_novo", length = 5)
    private String hrEventoNovo;
    @Column(name = "ds_ator", length = 150)
    private String dsAtor;
    @Column(name = "dt_ocorrencia", nullable = false)
    private LocalDateTime dtOcorrencia;
}
