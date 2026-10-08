package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_MENSAGEM_CLINICA")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MensagemClinica {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensagem")
    private Long idMensagem;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_conversa", nullable = false)
    private ConversaClinica conversa;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin")
    private Admin admin;
    @Column(name = "ds_remetente", nullable = false, length = 20)
    private String dsRemetente;
    @Column(name = "ds_texto", nullable = false, length = 2000)
    private String dsTexto;
    @Builder.Default
    @Column(name = "dt_envio", nullable = false)
    private LocalDateTime dtEnvio = LocalDateTime.now();
}
