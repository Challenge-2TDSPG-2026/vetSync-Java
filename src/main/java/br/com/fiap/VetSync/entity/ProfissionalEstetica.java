package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "TB_PROFISSIONAL_ESTETICA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfissionalEstetica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_profissional_estetica")
    private Long idProfissionalEstetica;

    @NotBlank(message = "Nome do profissional de estética é obrigatório")
    @Column(name = "nm_profissional_estetica", nullable = false, length = 100)
    private String nmProfissionalEstetica;

    @NotBlank(message = "Registro profissional é obrigatório")
    @Column(name = "nr_registro", nullable = false, unique = true, length = 20)
    private String nrRegistro;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail deve ter formato válido")
    @Column(name = "ds_email", nullable = false, unique = true, length = 150)
    private String dsEmail;

    @Column(name = "ds_senha", nullable = false)
    private String dsSenha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clinica", nullable = false)
    private Clinica clinica;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "TB_PROFISSIONAL_SERVICO",
            joinColumns = @JoinColumn(name = "id_profissional_estetica"),
            inverseJoinColumns = @JoinColumn(name = "id_servico")
    )
    @Builder.Default
    private Set<ServicoEstetica> servicos = new HashSet<>();
}