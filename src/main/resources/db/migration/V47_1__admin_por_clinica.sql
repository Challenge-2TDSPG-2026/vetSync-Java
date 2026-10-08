-- Contas existentes continuam administradores globais (id_clinica nulo).
ALTER TABLE TB_CLINICA ADD (
    ds_endereco VARCHAR2(240),
    ds_telefone VARCHAR2(20),
    ds_logo_mime VARCHAR2(80),
    ds_logo BLOB
);

CREATE TABLE TB_CARGO_CLINICA (
    id_cargo NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica NUMBER(10) NOT NULL,
    nm_cargo VARCHAR2(80) NOT NULL,
    CONSTRAINT fk_cargo_clinica FOREIGN KEY (id_clinica) REFERENCES TB_CLINICA(id_clinica),
    CONSTRAINT uq_cargo_clinica_nome UNIQUE (id_clinica, nm_cargo)
);

CREATE TABLE TB_CARGO_PERMISSAO (
    id_cargo NUMBER(10) NOT NULL,
    ds_permissao VARCHAR2(40) NOT NULL,
    CONSTRAINT pk_cargo_permissao PRIMARY KEY (id_cargo, ds_permissao),
    CONSTRAINT fk_permissao_cargo FOREIGN KEY (id_cargo) REFERENCES TB_CARGO_CLINICA(id_cargo) ON DELETE CASCADE
);

ALTER TABLE TB_ADMIN ADD (
    id_clinica NUMBER(10),
    id_cargo NUMBER(10),
    fl_dono NUMBER(1) DEFAULT 0 NOT NULL,
    fl_ativo NUMBER(1) DEFAULT 1 NOT NULL,
    fl_troca_senha NUMBER(1) DEFAULT 0 NOT NULL
);
ALTER TABLE TB_ADMIN ADD CONSTRAINT fk_admin_clinica FOREIGN KEY (id_clinica) REFERENCES TB_CLINICA(id_clinica);
ALTER TABLE TB_ADMIN ADD CONSTRAINT fk_admin_cargo FOREIGN KEY (id_cargo) REFERENCES TB_CARGO_CLINICA(id_cargo);
ALTER TABLE TB_ADMIN ADD CONSTRAINT ck_admin_escopo CHECK (
    (id_clinica IS NULL AND id_cargo IS NULL AND fl_dono = 0) OR
    (id_clinica IS NOT NULL AND ((fl_dono = 1 AND id_cargo IS NULL) OR (fl_dono = 0 AND id_cargo IS NOT NULL)))
);
CREATE UNIQUE INDEX uq_dono_clinica ON TB_ADMIN (CASE WHEN fl_dono = 1 THEN id_clinica END);
CREATE INDEX ix_admin_clinica ON TB_ADMIN (id_clinica, fl_ativo);

CREATE TABLE TB_HORARIO_CLINICA (
    id_horario NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica NUMBER(10) NOT NULL,
    nr_dia_semana NUMBER(1) NOT NULL,
    hr_inicio VARCHAR2(5) NOT NULL,
    hr_fim VARCHAR2(5) NOT NULL,
    CONSTRAINT fk_horario_clinica FOREIGN KEY (id_clinica) REFERENCES TB_CLINICA(id_clinica) ON DELETE CASCADE,
    CONSTRAINT ck_horario_dia CHECK (nr_dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_horario_intervalo CHECK (hr_inicio < hr_fim)
);

ALTER TABLE TB_TIPO_EVENTO ADD (id_clinica NUMBER(10));
ALTER TABLE TB_TIPO_EVENTO ADD CONSTRAINT fk_tipo_evento_clinica FOREIGN KEY (id_clinica) REFERENCES TB_CLINICA(id_clinica);
ALTER TABLE TB_TIPO_EVENTO DROP CONSTRAINT ck_tipo_evento_modalidade_agendamento;
ALTER TABLE TB_TIPO_EVENTO ADD CONSTRAINT ck_tipo_evento_modalidade_agendamento
    CHECK (ds_modalidade_agendamento IS NULL OR ds_modalidade_agendamento IN ('CLINICO_GERAL', 'SERVICO_CLINICA'));

CREATE TABLE TB_SERVICO_CLINICA (
    id_servico_clinica NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica NUMBER(10) NOT NULL,
    id_tipo_evento NUMBER(5) NOT NULL,
    id_servico_base NUMBER(10),
    nm_servico VARCHAR2(80) NOT NULL,
    ds_categoria VARCHAR2(30) NOT NULL,
    nr_duracao_minutos NUMBER(3) NOT NULL,
    fl_ativo NUMBER(1) DEFAULT 1 NOT NULL,
    CONSTRAINT fk_servico_clinica FOREIGN KEY (id_clinica) REFERENCES TB_CLINICA(id_clinica),
    CONSTRAINT fk_servico_tipo FOREIGN KEY (id_tipo_evento) REFERENCES TB_TIPO_EVENTO(id_tipo_evento),
    CONSTRAINT fk_servico_base FOREIGN KEY (id_servico_base) REFERENCES TB_SERVICO_ESTETICA(id_servico),
    CONSTRAINT uq_servico_clinica_nome UNIQUE (id_clinica, nm_servico),
    CONSTRAINT ck_servico_duracao CHECK (nr_duracao_minutos BETWEEN 5 AND 480)
);

ALTER TABLE TB_EVENTO_SAUDE ADD (id_servico_clinica NUMBER(10));
ALTER TABLE TB_EVENTO_SAUDE ADD CONSTRAINT fk_evento_servico_clinica
    FOREIGN KEY (id_servico_clinica) REFERENCES TB_SERVICO_CLINICA(id_servico_clinica);
CREATE INDEX ix_evento_servico_clinica ON TB_EVENTO_SAUDE(id_servico_clinica);

CREATE TABLE TB_SERVICO_PROFISSIONAL (
    id_servico_profissional NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_servico_clinica NUMBER(10) NOT NULL,
    id_veterinario NUMBER(10),
    id_profissional_estetica NUMBER(10),
    CONSTRAINT fk_sp_servico FOREIGN KEY (id_servico_clinica) REFERENCES TB_SERVICO_CLINICA(id_servico_clinica) ON DELETE CASCADE,
    CONSTRAINT fk_sp_vet FOREIGN KEY (id_veterinario) REFERENCES TB_VETERINARIO(id_veterinario),
    CONSTRAINT fk_sp_estetica FOREIGN KEY (id_profissional_estetica) REFERENCES TB_PROFISSIONAL_ESTETICA(id_profissional_estetica),
    CONSTRAINT ck_sp_profissional CHECK ((id_veterinario IS NOT NULL AND id_profissional_estetica IS NULL) OR
                                       (id_veterinario IS NULL AND id_profissional_estetica IS NOT NULL))
);
CREATE UNIQUE INDEX uq_sp_vet ON TB_SERVICO_PROFISSIONAL(id_servico_clinica, id_veterinario);
CREATE UNIQUE INDEX uq_sp_estetica ON TB_SERVICO_PROFISSIONAL(id_servico_clinica, id_profissional_estetica);

CREATE TABLE TB_CONVERSA_CLINICA (
    id_conversa NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica NUMBER(10) NOT NULL,
    id_tutor NUMBER(10) NOT NULL,
    dt_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_conversa_clinica FOREIGN KEY (id_clinica) REFERENCES TB_CLINICA(id_clinica),
    CONSTRAINT fk_conversa_tutor FOREIGN KEY (id_tutor) REFERENCES TB_TUTOR(id_tutor),
    CONSTRAINT uq_conversa_clinica_tutor UNIQUE (id_clinica, id_tutor)
);

CREATE TABLE TB_MENSAGEM_CLINICA (
    id_mensagem NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_conversa NUMBER(10) NOT NULL,
    id_admin NUMBER(10),
    ds_remetente VARCHAR2(20) NOT NULL,
    ds_texto VARCHAR2(2000) NOT NULL,
    dt_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_mensagem_conversa FOREIGN KEY (id_conversa) REFERENCES TB_CONVERSA_CLINICA(id_conversa),
    CONSTRAINT fk_mensagem_admin FOREIGN KEY (id_admin) REFERENCES TB_ADMIN(id_admin),
    CONSTRAINT ck_mensagem_remetente CHECK (ds_remetente IN ('TUTOR', 'CLINICA'))
);
CREATE INDEX ix_mensagem_conversa ON TB_MENSAGEM_CLINICA(id_conversa, dt_envio);
