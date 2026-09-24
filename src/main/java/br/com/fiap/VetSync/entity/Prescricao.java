// Prescricao.java (entity)
package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "TB_PRESCRICAO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prescricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prescricao")
    private Long idPrescricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    private EventoSaude evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_medicamento", nullable = false)
    private Medicamento medicamento;

    @NotBlank(message = "Posologia é obrigatória")
    @Column(name = "ds_posologia", nullable = false, length = 200)
    private String dsPosologia;

    @NotNull(message = "Data de início é obrigatória")
    @Column(name = "dt_inicio", nullable = false)
    private LocalDate dtInicio;

    @Column(name = "dt_fim")
    private LocalDate dtFim;

    @Column(name = "qt_doses_dia")
    private Integer qtDosesDia;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "ds_status", nullable = false, length = 20)
    private StatusPrescricao dsStatus = StatusPrescricao.SOLICITADO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin_validador")
    private Admin adminValidador;

    @Lob
    @Column(name = "ds_anexo_pdf")
    private byte[] dsAnexoPdf;

    @Column(name = "ds_anexo_pdf_nome", length = 255)
    private String dsAnexoPdfNome;

    @Column(name = "ds_anexo_pdf_tipo", length = 100)
    private String dsAnexoPdfTipo;
}