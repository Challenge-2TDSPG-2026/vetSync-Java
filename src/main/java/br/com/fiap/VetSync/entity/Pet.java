package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "TB_PET")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pet")
    private Long idPet;

    @NotBlank(message = "Nome do pet é obrigatório")
    @Column(name = "nm_pet", nullable = false, length = 80)
    private String nmPet;

    @NotNull(message = "Data de nascimento é obrigatória")
    @Column(name = "dt_nascimento", nullable = false)
    private LocalDate dtNascimento;

    @Pattern(regexp = "^[MF]$", message = "Sexo deve ser M ou F")
    @Column(name = "ds_sexo", length = 1)
    private String dsSexo;

    @Column(name = "nr_peso_kg", precision = 5, scale = 2)
    private BigDecimal nrPesoKg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tutor", nullable = false)
    private Tutor tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_raca", nullable = false)
    private Raca raca;

    /** Número do pet exibido na carteirinha e usado na busca do admin: o próprio id, com 4 dígitos (ex.: 0042). */
    public String numeroFormatado() {
        return idPet == null ? null : String.format("%04d", idPet);
    }

    // NOVO: foto do pet (perfil e carteirinha). Fora de toString/equals/hashCode para não expor o BLOB.
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Lob
    @Column(name = "ds_foto")
    private byte[] dsFoto;

    @Column(name = "ds_foto_tipo", length = 100)
    private String dsFotoTipo;
}