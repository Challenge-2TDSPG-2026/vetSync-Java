package br.com.fiap.VetSync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_LISTA_ESPERA")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListaEspera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_espera")
    private Long idEspera;

    /** Quem entrou na fila e será avisado (pode ser um cuidador com acesso ao pet). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tutor", nullable = false)
    private Tutor tutor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pet", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_servico_clinica", nullable = false)
    private ServicoClinica servicoClinica;

    /** Preferência por um profissional; null em ambos = qualquer profissional do serviço. */
    @Column(name = "id_veterinario")
    private Long idVeterinario;

    @Column(name = "id_profissional_estetica")
    private Long idProfissionalEstetica;

    @Column(name = "dt_desejada_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "dt_desejada_fim", nullable = false)
    private LocalDate dataFim;

    /** Faixa de horário aceita (HH:mm). Null = qualquer horário. */
    @Column(name = "hr_min", length = 5)
    private String horaMin;

    @Column(name = "hr_max", length = 5)
    private String horaMax;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "ds_status", nullable = false, length = 20)
    private StatusListaEspera status = StatusListaEspera.AGUARDANDO;

    /** Preenchidos quando uma vaga compatível surge (status NOTIFICADO). */
    @Column(name = "dt_vaga")
    private LocalDate dataVaga;

    @Column(name = "hr_vaga", length = 5)
    private String horaVaga;

    /** VETERINARIO ou ESTETICA. */
    @Column(name = "ds_tipo_profissional_vaga", length = 20)
    private String tipoProfissionalVaga;

    @Column(name = "id_profissional_vaga")
    private Long idProfissionalVaga;

    @Column(name = "dt_notificacao")
    private LocalDateTime notificadaEm;

    @Column(name = "dt_criacao", nullable = false)
    private LocalDateTime criadaEm;

    @PrePersist
    void prePersist() {
        if (criadaEm == null) criadaEm = LocalDateTime.now();
    }

    public void limparVaga() {
        this.dataVaga = null;
        this.horaVaga = null;
        this.tipoProfissionalVaga = null;
        this.idProfissionalVaga = null;
        this.notificadaEm = null;
    }
}