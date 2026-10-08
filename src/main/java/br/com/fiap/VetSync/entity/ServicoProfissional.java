package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TB_SERVICO_PROFISSIONAL")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ServicoProfissional {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servico_profissional")
    private Long idServicoProfissional;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servico_clinica", nullable = false)
    private ServicoClinica servico;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_veterinario")
    private Veterinario veterinario;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_profissional_estetica")
    private ProfissionalEstetica profissionalEstetica;
}
