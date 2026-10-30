package br.com.fiap.VetSync.service;

import br.com.fiap.VetSync.entity.StatusConfirmacao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Regra de negócio: um agendamento novo nasce PENDENTE (clínica confirma depois) ou já CONFIRMADO. */
@Component
public class PoliticaConfirmacaoAgenda {

    private final boolean exigeConfirmacao;

    public PoliticaConfirmacaoAgenda(@Value("${app.agenda.exige-confirmacao:false}") boolean exigeConfirmacao) {
        this.exigeConfirmacao = exigeConfirmacao;
    }

    /** PENDENTE se a clínica exige confirmação; senão, já CONFIRMADO. */
    public StatusConfirmacao statusInicial() {
        return exigeConfirmacao ? StatusConfirmacao.PENDENTE : StatusConfirmacao.CONFIRMADO;
    }
}
