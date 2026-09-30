ALTER TABLE TB_PET_CONVITE ADD (
    fl_todos_pets NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT ck_convite_todos_pets CHECK (fl_todos_pets IN (0, 1))
);

CREATE TABLE TB_TUTOR_RESPONSAVEL (
    id_responsavel       NUMBER(19)   GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_tutor_proprietario NUMBER(10)  NOT NULL,
    id_tutor_responsavel NUMBER(10)   NOT NULL,
    ds_permissao         VARCHAR2(10) NOT NULL,
    ds_status            VARCHAR2(10) NOT NULL,
    dt_concedido         TIMESTAMP    NOT NULL,
    dt_revogado          TIMESTAMP    NULL,
    CONSTRAINT fk_resp_tutor_proprietario FOREIGN KEY (id_tutor_proprietario) REFERENCES TB_TUTOR(id_tutor),
    CONSTRAINT fk_resp_tutor_responsavel FOREIGN KEY (id_tutor_responsavel) REFERENCES TB_TUTOR(id_tutor),
    CONSTRAINT uq_resp_tutores UNIQUE (id_tutor_proprietario, id_tutor_responsavel),
    CONSTRAINT ck_resp_tutores_distintos CHECK (id_tutor_proprietario <> id_tutor_responsavel),
    CONSTRAINT ck_resp_permissao CHECK (ds_permissao IN ('LEITURA', 'EDICAO')),
    CONSTRAINT ck_resp_status CHECK (ds_status IN ('ATIVO', 'REVOGADO'))
);

CREATE INDEX ix_resp_proprietario ON TB_TUTOR_RESPONSAVEL (id_tutor_proprietario);
CREATE INDEX ix_resp_responsavel ON TB_TUTOR_RESPONSAVEL (id_tutor_responsavel);
