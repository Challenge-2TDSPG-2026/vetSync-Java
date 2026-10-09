package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Link temporário e somente leitura do prontuário, para o tutor apresentar a outro atendimento.
 * O token puro nunca é persistido: só o hash SHA-256 (hex).
 */
@Entity
@Table(name = "TB_PRONTUARIO_COMPARTILHADO")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProntuarioCompartilhado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compartilhamento")
    private Long idCompartilhamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pet", nullable = false)
    private Pet pet;

    @NotNull
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /** Seções liberadas, separadas por vírgula (ex.: "PERFIL_SAUDE,EXAMES"). */
    @NotNull
    @Column(name = "ds_secoes", nullable = false, length = 200)
    private String dsSecoes;

    /** Identificação livre de quem vai receber (ex.: "Clínica Pet Norte"), só para o tutor se organizar. */
    @Column(name = "ds_destinatario", length = 150)
    private String dsDestinatario;

    @Column(name = "dt_periodo_inicio")
    private LocalDate dtPeriodoInicio;

    @Column(name = "dt_periodo_fim")
    private LocalDate dtPeriodoFim;

    @NotNull
    @Column(name = "id_usuario_criador", nullable = false)
    private Long idUsuarioCriador;

    @NotNull
    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @NotNull
    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "revogada_em")
    private LocalDateTime revogadaEm;

    @Column(name = "ultimo_acesso_em")
    private LocalDateTime ultimoAcessoEm;

    @Builder.Default
    @Column(name = "nr_acessos", nullable = false)
    private Integer nrAcessos = 0;

    @Transient
    public boolean isAtivo() {
        return revogadaEm == null && expiraEm != null && expiraEm.isAfter(LocalDateTime.now());
    }

    @Transient
    public Set<SecaoProntuario> secoes() {
        if (dsSecoes == null || dsSecoes.isBlank()) {
            return EnumSet.noneOf(SecaoProntuario.class);
        }
        Set<SecaoProntuario> resultado = EnumSet.noneOf(SecaoProntuario.class);
        Arrays.stream(dsSecoes.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .forEach(s -> resultado.add(SecaoProntuario.valueOf(s)));
        return resultado;
    }

    public static String serializarSecoes(Set<SecaoProntuario> secoes) {
        return secoes.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }
}