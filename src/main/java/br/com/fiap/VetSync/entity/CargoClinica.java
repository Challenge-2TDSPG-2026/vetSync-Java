package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "TB_CARGO_CLINICA")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CargoClinica {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cargo")
    private Long idCargo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clinica", nullable = false)
    private Clinica clinica;

    @Column(name = "nm_cargo", nullable = false, length = 80)
    private String nmCargo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "TB_CARGO_PERMISSAO", joinColumns = @JoinColumn(name = "id_cargo"))
    @Column(name = "ds_permissao", nullable = false, length = 40)
    @Builder.Default
    private Set<String> permissoes = new HashSet<>();
}
