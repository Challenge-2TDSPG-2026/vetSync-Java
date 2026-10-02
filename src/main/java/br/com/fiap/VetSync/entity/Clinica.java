package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Entity
@Table(name = "TB_CLINICA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Clinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_clinica")
    private Long idClinica;

    @NotBlank(message = "Nome da clínica é obrigatório")
    @Column(name = "nm_clinica", nullable = false, length = 150)
    private String nmClinica;

    @NotBlank(message = "CNPJ é obrigatório")
    @Pattern(regexp = "^\\d{14}$", message = "CNPJ deve conter 14 dígitos numéricos")
    @Column(name = "ds_cnpj", nullable = false, unique = true, length = 14)
    private String dsCnpj;

    @Column(name = "ds_cidade", length = 80)
    private String dsCidade;

    @Column(name = "ds_uf", length = 2)
    private String dsUf;

    @Builder.Default
    @Column(name = "st_contratante", nullable = false, length = 1)
    private String stContratante = "A";

    public boolean estaContratanteAtiva() {
        return "A".equals(stContratante);
    }
}
