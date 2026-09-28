package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_VACINA_PET")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacinaPet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vacina")
    private Long idVacina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pet", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_vacina", nullable = false)
    private TipoVacina tipoVacina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento")
    private EventoSaude evento;

    @NotNull
    @Column(name = "dt_aplicacao", nullable = false)
    private LocalDate dtAplicacao;

    @Column(name = "dt_proxima_dose")
    private LocalDate dtProximaDose;

    @Column(name = "ds_comprovante_url", length = 500)
    private String comprovanteUrl;

    @Column(name = "dt_criacao", nullable = false)
    private LocalDateTime criadoEm;
}
