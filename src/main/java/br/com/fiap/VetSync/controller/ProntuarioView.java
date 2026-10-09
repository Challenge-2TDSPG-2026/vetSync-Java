package br.com.fiap.VetSync.controller;

import br.com.fiap.VetSync.entity.SecaoProntuario;
import br.com.fiap.VetSync.service.ProntuarioService;

import java.util.HashMap;
import java.util.Map;

/** Variáveis do template {@code prontuario.html}, usadas pela página pública e pela exportação em HTML. */
final class ProntuarioView {
    private ProntuarioView() {}

    /**
     * @param arquivoBaseUrl prefixo para os links de download dos arquivos de exame (termina em "/exames/");
     *                       {@code null} quando não há como baixá-los (ex.: arquivo HTML exportado).
     */
    static Map<String, Object> variaveis(ProntuarioService.Prontuario prontuario, String arquivoBaseUrl) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("prontuario", prontuario);
        vars.put("arquivoBaseUrl", arquivoBaseUrl);
        vars.put("mostrarPerfil", prontuario.secoes().contains(SecaoProntuario.PERFIL_SAUDE));
        vars.put("mostrarAtendimentos", prontuario.secoes().contains(SecaoProntuario.ATENDIMENTOS));
        vars.put("mostrarOrientacoes", prontuario.secoes().contains(SecaoProntuario.ORIENTACOES));
        vars.put("mostrarReceitas", prontuario.secoes().contains(SecaoProntuario.RECEITAS));
        vars.put("mostrarExames", prontuario.secoes().contains(SecaoProntuario.EXAMES));
        return vars;
    }
}