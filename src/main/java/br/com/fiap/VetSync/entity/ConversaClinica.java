package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_CONVERSA_CLINICA")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ConversaClinica {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conversa")
    private Long idConversa;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clinica", nullable = false)
    private Clinica clinica;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tutor", nullable = false)
    private Tutor tutor;
    @Builder.Default
    @Column(name = "dt_criacao", nullable = false)
    private LocalDateTime dtCriacao = LocalDateTime.now();
}
