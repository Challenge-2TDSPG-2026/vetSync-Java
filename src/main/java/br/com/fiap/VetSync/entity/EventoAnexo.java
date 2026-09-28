package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_EVENTO_ANEXO")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoAnexo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_anexo")
    private Long idAnexo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evento", nullable = false)
    private EventoSaude evento;

    @Column(name = "nm_arquivo", nullable = false, length = 255)
    private String nmArquivo;
    @Column(name = "ds_mime_type", nullable = false, length = 100)
    private String dsMimeType;
    @Column(name = "nr_tamanho", nullable = false)
    private Long nrTamanho;
    @Lob
    @Column(name = "ds_conteudo", nullable = false)
    private byte[] dsConteudo;
    @Column(name = "ds_ator", length = 150)
    private String dsAtor;
    @Column(name = "dt_criacao", nullable = false)
    private LocalDateTime dtCriacao;
}
