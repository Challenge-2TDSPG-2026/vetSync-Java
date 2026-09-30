package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_TUTOR")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tutor")
    private Long idTutor;

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    @Column(name = "nm_tutor", nullable = false, length = 100)
    private String nmTutor;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail deve ter formato válido")
    @Column(name = "ds_email", nullable = false, unique = true, length = 150)
    private String dsEmail;

    @Pattern(regexp = "^\\d{10,11}$", message = "Telefone deve conter 10 ou 11 dígitos")
    @Column(name = "nr_telefone", length = 20)
    private String nrTelefone;

    @Pattern(regexp = "^\\d{8}$", message = "CEP deve conter 8 dígitos numéricos")
    @Column(name = "nr_cep", length = 8)
    private String nrCep;

    @Size(max = 150, message = "Endereço deve ter no máximo 150 caracteres")
    @Column(name = "ds_logradouro", length = 150)
    private String dsLogradouro;

    @Size(max = 20, message = "Número deve ter no máximo 20 caracteres")
    @Column(name = "nr_endereco", length = 20)
    private String nrEndereco;

    @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres")
    @Column(name = "ds_complemento", length = 100)
    private String dsComplemento;

    @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres")
    @Column(name = "ds_bairro", length = 100)
    private String dsBairro;

    @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
    @Column(name = "nm_cidade", length = 100)
    private String nmCidade;

    @Pattern(regexp = "^[A-Z]{2}$", message = "UF deve conter duas letras maiúsculas")
    @Column(name = "sg_uf", length = 2)
    private String sgUf;

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "^\\d{11}$", message = "CPF deve conter 11 dígitos numéricos")
    @Column(name = "ds_cpf", nullable = false, unique = true, length = 11)
    private String dsCpf;

    @Column(name = "ds_senha", nullable = false)
    private String dsSenha;

    @Column(name = "dt_senha_alterada_em")
    private LocalDateTime dtSenhaAlteradaEm;

    @Builder.Default
    @Column(name = "dt_cadastro", nullable = false)
    private LocalDate dtCadastro = LocalDate.now();
}
