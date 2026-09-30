package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_VINCULO_TUTOR_CLINICA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VinculoTutorClinica {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vinculo_tutor_clinica")
    private Long idVinculoTutorClinica;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_tutor", nullable = false)
    private Tutor tutor;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_clinica", nullable = false)
    private Clinica clinica;

    @Column(name = "dt_inicio", nullable = false) @Builder.Default
    private LocalDateTime dtInicio = LocalDateTime.now();

    @Column(name = "dt_encerramento")
    private LocalDateTime dtEncerramento;

    public boolean estaAtivo() { return dtEncerramento == null; }
}
