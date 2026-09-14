package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "TB_LINK_AGENDAMENTO_VET")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkAgendamentoVet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_link")
    private Long idLink;

    @Column(name = "ds_token", nullable = false, unique = true, length = 36)
    private String dsToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_relatorio", nullable = false)
    private RelatorioEstetica relatorioEstetica;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "ds_status", nullable = false, length = 20)
    private StatusLinkAgendamento dsStatus = StatusLinkAgendamento.PENDENTE;

    @Builder.Default
    @Column(name = "dt_criacao", nullable = false)
    private LocalDate dtCriacao = LocalDate.now();

    @Column(name = "dt_expiracao", nullable = false)
    private LocalDate dtExpiracao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento_criado")
    private EventoSaude eventoCriado;
}