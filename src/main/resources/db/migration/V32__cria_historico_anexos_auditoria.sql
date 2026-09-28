CREATE TABLE TB_EVENTO_HISTORICO (
    id_historico NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_evento NUMBER(19) NOT NULL,
    ds_acao VARCHAR2(40) NOT NULL,
    ds_status_anterior VARCHAR2(20), ds_status_novo VARCHAR2(20),
    ds_observacao_anterior VARCHAR2(500), ds_observacao_nova VARCHAR2(500),
    vl_custo_anterior NUMBER(10,2), vl_custo_novo NUMBER(10,2),
    dt_evento_anterior DATE, dt_evento_novo DATE,
    hr_evento_anterior VARCHAR2(5), hr_evento_novo VARCHAR2(5),
    ds_ator VARCHAR2(150), dt_ocorrencia TIMESTAMP NOT NULL,
    CONSTRAINT fk_evento_historico_evento FOREIGN KEY (id_evento) REFERENCES TB_EVENTO_SAUDE(id_evento)
);
CREATE INDEX ix_evento_historico_evento ON TB_EVENTO_HISTORICO(id_evento, dt_ocorrencia);

CREATE TABLE TB_EVENTO_ANEXO (
    id_anexo NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_evento NUMBER(19) NOT NULL,
    nm_arquivo VARCHAR2(255) NOT NULL, ds_mime_type VARCHAR2(100) NOT NULL,
    nr_tamanho NUMBER(19) NOT NULL, ds_conteudo BLOB NOT NULL,
    ds_ator VARCHAR2(150), dt_criacao TIMESTAMP NOT NULL,
    CONSTRAINT fk_evento_anexo_evento FOREIGN KEY (id_evento) REFERENCES TB_EVENTO_SAUDE(id_evento)
);
CREATE INDEX ix_evento_anexo_evento ON TB_EVENTO_ANEXO(id_evento);

CREATE TABLE TB_AUDITORIA (
    id_auditoria NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ds_entidade VARCHAR2(40) NOT NULL, id_entidade NUMBER(19) NOT NULL,
    ds_acao VARCHAR2(60) NOT NULL, ds_ator VARCHAR2(150), ds_perfil VARCHAR2(40),
    ds_valor_anterior CLOB, ds_valor_novo CLOB, ds_ip VARCHAR2(64),
    dt_ocorrencia TIMESTAMP NOT NULL
);
CREATE INDEX ix_auditoria_entidade ON TB_AUDITORIA(ds_entidade, id_entidade, dt_ocorrencia);
CREATE INDEX ix_auditoria_data ON TB_AUDITORIA(dt_ocorrencia);
