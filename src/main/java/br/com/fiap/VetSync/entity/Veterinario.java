package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "TB_VETERINARIO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Veterinario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_veterinario")
    private Long idVeterinario;

    @NotBlank(message = "Nome do veterinário é obrigatório")
    @Column(name = "nm_veterinario", nullable = false, length = 100)
    private String nmVeterinario;

    @NotBlank(message = "CRMV é obrigatório")
    @Column(name = "nr_crmv", nullable = false, unique = true, length = 20)
    private String nrCrmv;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail deve ter formato válido")
    @Column(name = "ds_email", nullable = false, unique = true, length = 150)
    private String dsEmail;

    @Column(name = "ds_senha", nullable = false)
    private String dsSenha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clinica", nullable = false)
    private Clinica clinica;

    @Column(name = "ds_especialidade", length = 50)
    private String dsEspecialidade;
}