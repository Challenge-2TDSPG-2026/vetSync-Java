package br.com.fiap.VetSync.service;

import java.util.List;

/** Tipos auditáveis (entidade) e ações registradas. Servem também para montar os filtros da tela. */
public final class AuditoriaTipos {
    private AuditoriaTipos() {}

    // entidades
    public static final String EVENTO = "EVENTO";
    public static final String ACESSO_PET = "ACESSO_PET";
    public static final String VINCULO = "VINCULO";
    public static final String CONTRATO = "CONTRATO";
    public static final String CODIGO_VINCULO = "CODIGO_VINCULO";
    public static final String PONTOS = "PONTOS";
    public static final String CATALOGO = "CATALOGO";
    public static final String RESGATE = "RESGATE";
    /** Prontuário clínico; {@code idEntidade} é o id do pet. */
    public static final String PRONTUARIO = "PRONTUARIO";

    public static final List<String> ENTIDADES = List.of(EVENTO, ACESSO_PET, VINCULO, CONTRATO,
            CODIGO_VINCULO, PONTOS, CATALOGO, RESGATE, PRONTUARIO);

    /** Entidades que o fluxo legado (qualquer usuário autenticado, por entidade+id) continua podendo consultar. */
    public static final List<String> ENTIDADES_LEGADO = List.of(EVENTO, ACESSO_PET);

    public static final List<String> ACOES = List.of(
            "CRIADO", "CONCLUIDO", "CANCELADO", "ACESSO_REVOGADO", "ACESSO_ATUALIZADO",
            "VINCULO_CRIADO", "VINCULO_TROCADO", "VINCULO_ENCERRADO",
            "CONTRATO_ATIVADO", "CONTRATO_DESATIVADO",
            "CODIGO_EMITIDO", "CODIGO_REVOGADO",
            "PONTOS_LANCADOS", "PONTOS_LIBERADOS", "PONTOS_BLOQUEADOS", "PONTOS_DESBLOQUEADOS",
            "CATALOGO_ITEM_CRIADO", "CATALOGO_ITEM_ATUALIZADO", "CATALOGO_ITEM_INATIVADO", "CATALOGO_ITEM_EXCLUIDO",
            "RESGATE_SOLICITADO", "RESGATE_VALIDADO", "RESGATE_NEGADO",
            "ORIENTACAO_CRIADA", "ORIENTACAO_REMOVIDA", "EXAME_REGISTRADO", "EXAME_ARQUIVO_ANEXADO", "EXAME_REMOVIDO",
            "PRONTUARIO_COMPARTILHADO", "PRONTUARIO_COMPARTILHAMENTO_REVOGADO", "PRONTUARIO_ACESSADO_POR_LINK",
            "PRONTUARIO_ARQUIVO_ACESSADO_POR_LINK", "PRONTUARIO_EXPORTADO");
}