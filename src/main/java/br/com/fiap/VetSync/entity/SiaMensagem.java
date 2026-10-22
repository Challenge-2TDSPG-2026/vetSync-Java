package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_SIA_MENSAGEM")
@Getter
@Setter
public class SiaMensagem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensagem")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_conversa")
    private SiaConversa conversa;

    @Column(name = "ds_autoria", nullable = false, length = 16)
    private String autoria;

    @Lob
    @Column(name = "ds_texto", nullable = false)
    private String texto;

    @Column(name = "dt_criacao", nullable = false)
    private LocalDateTime criadoEm;
}
