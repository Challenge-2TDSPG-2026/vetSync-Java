package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_CARTEIRA_COMPARTILHADA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarteiraCompartilhada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carteira_compartilhada")
    private Long idCarteiraCompartilhada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pet", nullable = false)
    private Pet pet;


    @NotNull
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @NotNull
    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @NotNull
    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "revogada_em")
    private LocalDateTime revogadaEm;

    @NotNull
    @Column(name = "id_usuario_criador", nullable = false)
    private Long idUsuarioCriador;

    @Column(name = "ultimo_acesso_em")
    private LocalDateTime ultimoAcessoEm;

    @Transient
    public boolean isAtiva() {
        return revogadaEm == null && expiraEm != null && expiraEm.isAfter(LocalDateTime.now());
    }
}