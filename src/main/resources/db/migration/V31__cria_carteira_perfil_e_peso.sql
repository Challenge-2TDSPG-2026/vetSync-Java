CREATE TABLE TB_TIPO_VACINA (
    id_tipo_vacina NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nm_tipo_vacina VARCHAR2(100) NOT NULL UNIQUE,
    nr_periodicidade_dias NUMBER(5) NOT NULL,
    fl_ativo NUMBER(1) DEFAULT 1 NOT NULL
);

CREATE TABLE TB_VACINA_PET (
    id_vacina NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pet NUMBER(10) NOT NULL,
    id_tipo_vacina NUMBER(10) NOT NULL,
    id_evento NUMBER(10),
    dt_aplicacao DATE NOT NULL,
    dt_proxima_dose DATE,
    ds_comprovante_url VARCHAR2(500),
    dt_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_vacina_pet FOREIGN KEY (id_pet) REFERENCES TB_PET(id_pet),
    CONSTRAINT fk_vacina_tipo FOREIGN KEY (id_tipo_vacina) REFERENCES TB_TIPO_VACINA(id_tipo_vacina),
    CONSTRAINT fk_vacina_evento FOREIGN KEY (id_evento) REFERENCES TB_EVENTO_SAUDE(id_evento)
);

CREATE TABLE TB_PERFIL_SAUDE (
    id_perfil NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pet NUMBER(10) NOT NULL UNIQUE,
    nr_peso_atual NUMBER(5,2),
    dt_peso_atualizado DATE,
    ds_alergias VARCHAR2(2000),
    ds_medicamentos_continuos VARCHAR2(2000),
    ds_restricoes_alimentares VARCHAR2(2000),
    ds_condicoes_pre_existentes VARCHAR2(2000),
    ds_observacoes_importantes VARCHAR2(2000),
    ds_contato_emergencia VARCHAR2(300),
    id_veterinario_preferencial NUMBER(10),
    CONSTRAINT fk_perfil_pet FOREIGN KEY (id_pet) REFERENCES TB_PET(id_pet),
    CONSTRAINT fk_perfil_veterinario FOREIGN KEY (id_veterinario_preferencial) REFERENCES TB_VETERINARIO(id_veterinario)
);

CREATE TABLE TB_HISTORICO_PESO (
    id_peso NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pet NUMBER(10) NOT NULL,
    nr_peso_kg NUMBER(5,2) NOT NULL,
    dt_medicao DATE NOT NULL,
    ds_observacao VARCHAR2(300),
    CONSTRAINT fk_peso_pet FOREIGN KEY (id_pet) REFERENCES TB_PET(id_pet)
);

CREATE INDEX ix_vacina_pet ON TB_VACINA_PET(id_pet, dt_proxima_dose);
CREATE INDEX ix_peso_pet ON TB_HISTORICO_PESO(id_pet, dt_medicao);

INSERT INTO TB_TIPO_VACINA (nm_tipo_vacina, nr_periodicidade_dias, fl_ativo)
VALUES ('Antirrábica', 365, 1);
INSERT INTO TB_TIPO_VACINA (nm_tipo_vacina, nr_periodicidade_dias, fl_ativo)
VALUES ('V8/V10', 365, 1);
