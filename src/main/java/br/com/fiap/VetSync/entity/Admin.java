package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_ADMIN")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_admin")
    private Long idAdmin;

    @NotBlank(message = "Nome é obrigatório")
    @Column(name = "nm_admin", nullable = false, length = 100)
    private String nmAdmin;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail deve ter formato válido")
    @Column(name = "ds_email", nullable = false, unique = true, length = 150)
    private String dsEmail;

    @Column(name = "ds_senha", nullable = false)
    private String dsSenha;

    @Column(name = "dt_senha_alterada_em")
    private LocalDateTime dtSenhaAlteradaEm;

    @Builder.Default
    @Column(name = "dt_cadastro", nullable = false)
    private LocalDate dtCadastro = LocalDate.now();
}