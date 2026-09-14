package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "TB_SERVICO_ESTETICA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicoEstetica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servico")
    private Long idServico;

    @NotBlank(message = "Nome do serviço é obrigatório")
    @Column(name = "nm_servico", nullable = false, unique = true, length = 100)
    private String nmServico;

    @NotNull(message = "Tipo do serviço é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "tp_servico", nullable = false, length = 10)
    private TipoServicoEstetica tpServico;
}