package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Entity
@Table(name = "TB_RECOMPENSA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recompensa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recompensa")
    private Long idRecompensa;

    @NotBlank(message = "Nome da recompensa é obrigatório")
    @Column(name = "nm_recompensa", nullable = false, length = 150)
    private String nmRecompensa;

    @Column(name = "ds_descricao", length = 500)
    private String dsDescricao;

    @Positive(message = "Custo em pontos deve ser maior que zero")
    @Column(name = "nr_custo_pontos", nullable = false)
    private Integer nrCustoPontos;

    @Enumerated(EnumType.STRING)
    @Column(name = "ds_tipo", nullable = false, length = 20)
    private TipoRecompensa dsTipo;

    @Builder.Default
    @Column(name = "fl_ativo", nullable = false)
    private Boolean flAtivo = true;

    // ALTERADO: excluídos de toString/equals/hashCode para não imprimir/comparar o BLOB inteiro
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Lob
    @Column(name = "ds_imagem")
    private byte[] dsImagem;

    @Column(name = "ds_imagem_tipo", length = 100)
    private String dsImagemTipo;
}