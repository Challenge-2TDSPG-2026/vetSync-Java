ALTER TABLE TB_CLINICA ADD (
    st_contratante CHAR(1) DEFAULT 'A' NOT NULL
        CHECK (st_contratante IN ('A', 'I'))
);

CREATE TABLE TB_CODIGO_VINCULO_CLINICA (
    id_codigo_vinculo NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica NUMBER(10) NOT NULL,
    ds_codigo_hash CHAR(64) NOT NULL UNIQUE,
    st_ativo CHAR(1) DEFAULT 'A' NOT NULL CHECK (st_ativo IN ('A', 'I')),
    dt_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    dt_revogacao TIMESTAMP,
    CONSTRAINT fk_codigo_vinculo_clinica FOREIGN KEY (id_clinica)
        REFERENCES TB_CLINICA(id_clinica)
);

CREATE TABLE TB_SESSAO_VINCULO_CLINICA (
    id_sessao_vinculo NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica NUMBER(10) NOT NULL,
    ds_token_hash CHAR(64) NOT NULL UNIQUE,
    dt_expiracao TIMESTAMP NOT NULL,
    dt_utilizacao TIMESTAMP,
    dt_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_sessao_vinculo_clinica FOREIGN KEY (id_clinica)
        REFERENCES TB_CLINICA(id_clinica)
);

CREATE TABLE TB_VINCULO_TUTOR_CLINICA (
    id_vinculo_tutor_clinica NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_tutor NUMBER(10) NOT NULL,
    id_clinica NUMBER(10) NOT NULL,
    dt_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    dt_encerramento TIMESTAMP,
    CONSTRAINT fk_vinculo_tutor FOREIGN KEY (id_tutor) REFERENCES TB_TUTOR(id_tutor),
    CONSTRAINT fk_vinculo_clinica FOREIGN KEY (id_clinica) REFERENCES TB_CLINICA(id_clinica),
    CONSTRAINT ck_vinculo_datas CHECK (dt_encerramento IS NULL OR dt_encerramento >= dt_inicio)
);

CREATE INDEX idx_vinculo_tutor ON TB_VINCULO_TUTOR_CLINICA (id_tutor, dt_encerramento);
CREATE INDEX idx_vinculo_clinica ON TB_VINCULO_TUTOR_CLINICA (id_clinica, dt_encerramento);
CREATE UNIQUE INDEX uq_vinculo_tutor_ativo ON TB_VINCULO_TUTOR_CLINICA
    (CASE WHEN dt_encerramento IS NULL THEN id_tutor END);
CREATE INDEX idx_sessao_vinculo_token ON TB_SESSAO_VINCULO_CLINICA (ds_token_hash);
