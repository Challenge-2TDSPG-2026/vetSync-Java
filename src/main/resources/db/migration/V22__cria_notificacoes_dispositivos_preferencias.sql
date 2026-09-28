CREATE TABLE TB_DISPOSITIVO_PUSH (
    id_dispositivo NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_tutor NUMBER(10) NOT NULL,
    ds_token VARCHAR2(255) NOT NULL,
    ds_plataforma VARCHAR2(20) NOT NULL,
    nm_dispositivo VARCHAR2(100),
    ds_fuso_horario VARCHAR2(80),
    fl_ativo NUMBER(1) DEFAULT 1 NOT NULL,
    dt_ultimo_uso TIMESTAMP,
    dt_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_dispositivo_tutor FOREIGN KEY (id_tutor) REFERENCES TB_TUTOR(id_tutor),
    CONSTRAINT uk_dispositivo_usuario_token_plat UNIQUE (id_tutor, ds_token, ds_plataforma)
);

CREATE TABLE TB_NOTIFICACAO (
    id_notificacao NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_tutor NUMBER(10) NOT NULL,
    ds_tipo VARCHAR2(40) NOT NULL,
    ds_titulo VARCHAR2(150) NOT NULL,
    ds_mensagem VARCHAR2(500) NOT NULL,
    ds_referencia_tipo VARCHAR2(50),
    id_referencia NUMBER(19),
    fl_lida NUMBER(1) DEFAULT 0 NOT NULL,
    dt_enviada TIMESTAMP,
    dt_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_notificacao_tutor FOREIGN KEY (id_tutor) REFERENCES TB_TUTOR(id_tutor)
);

CREATE INDEX ix_notificacao_tutor_lida ON TB_NOTIFICACAO (id_tutor, fl_lida);

CREATE TABLE TB_PREFERENCIA_NOTIFICACAO (
    id_preferencia NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_tutor NUMBER(10) NOT NULL UNIQUE,
    fl_push_ativo NUMBER(1) DEFAULT 1 NOT NULL,
    fl_lembrete_sete_dias NUMBER(1) DEFAULT 1 NOT NULL,
    fl_lembrete_um_dia NUMBER(1) DEFAULT 1 NOT NULL,
    fl_lembrete_duas_horas NUMBER(1) DEFAULT 0 NOT NULL,
    fl_vacinas_vencendo NUMBER(1) DEFAULT 1 NOT NULL,
    fl_retornos_pendentes NUMBER(1) DEFAULT 1 NOT NULL,
    fl_convites_acesso NUMBER(1) DEFAULT 1 NOT NULL,
    fl_resgates NUMBER(1) DEFAULT 1 NOT NULL,
    CONSTRAINT fk_preferencia_tutor FOREIGN KEY (id_tutor) REFERENCES TB_TUTOR(id_tutor)
);
