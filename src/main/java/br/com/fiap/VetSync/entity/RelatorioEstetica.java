package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "TB_RELATORIO_ESTETICA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioEstetica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_relatorio")
    private Long idRelatorio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    private EventoSaude evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_profissional_estetica", nullable = false)
    private ProfissionalEstetica profissionalEstetica;

    @NotBlank(message = "Descrição do problema é obrigatória")
    @Column(name = "ds_problema", nullable = false, length = 500)
    private String dsProblema;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "ds_status", nullable = false, length = 20)
    private StatusRelatorioEstetica dsStatus = StatusRelatorioEstetica.SOLICITADO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin_validador")
    private Admin adminValidador;

    @Builder.Default
    @Column(name = "dt_criacao", nullable = false)
    private LocalDate dtCriacao = LocalDate.now();
}