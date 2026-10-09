package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Resultado de exame do pet: laudo em texto e, opcionalmente, o arquivo (PDF/imagem) do laboratório. */
@Entity
@Table(name = "TB_RESULTADO_EXAME")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoExame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resultado")
    private Long idResultado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pet", nullable = false)
    private Pet pet;

    /** Atendimento em que o exame foi solicitado/avaliado (opcional: pode vir de laboratório externo). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento")
    private EventoSaude evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_veterinario")
    private Veterinario veterinario;

    @Column(name = "nm_exame", nullable = false, length = 120)
    private String nmExame;

    @Column(name = "nm_laboratorio", length = 120)
    private String nmLaboratorio;

    @Column(name = "dt_coleta")
    private LocalDate dtColeta;

    @Column(name = "dt_resultado", nullable = false)
    private LocalDate dtResultado;

    @Column(name = "ds_resultado", length = 2000)
    private String dsResultado;

    @Column(name = "ds_interpretacao", length = 1000)
    private String dsInterpretacao;

    @Column(name = "nm_arquivo", length = 255)
    private String nmArquivo;

    @Column(name = "ds_mime_type", length = 100)
    private String dsMimeType;

    @Column(name = "nr_tamanho")
    private Long nrTamanho;

    /** BLOB fora de toString/equals/hashCode para não expor nem carregar o arquivo à toa. */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Lob
    @Column(name = "ds_conteudo")
    private byte[] dsConteudo;

    @Column(name = "ds_ator", length = 150)
    private String dsAtor;

    @Column(name = "dt_criacao", nullable = false)
    private LocalDateTime dtCriacao;

    @Transient
    public boolean temArquivo() {
        return nmArquivo != null;
    }
}