CREATE TABLE TB_PROFISSIONAL_ESTETICA (
                                          id_profissional_estetica NUMBER(10)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                          nm_profissional_estetica VARCHAR2(100) NOT NULL,
                                          nr_registro              VARCHAR2(20)  NOT NULL UNIQUE,
                                          ds_email                 VARCHAR2(150) NOT NULL UNIQUE,
                                          ds_senha                 VARCHAR2(255) NOT NULL,
                                          id_clinica               NUMBER(10)    NOT NULL,
                                          CONSTRAINT fk_prof_est_clinica FOREIGN KEY (id_clinica) REFERENCES TB_CLINICA(id_clinica)
);

ALTER TABLE TB_EVENTO_SAUDE ADD id_profissional_estetica NUMBER(10);
ALTER TABLE TB_EVENTO_SAUDE ADD CONSTRAINT fk_ev_prof_est
    FOREIGN KEY (id_profissional_estetica) REFERENCES TB_PROFISSIONAL_ESTETICA(id_profissional_estetica);

CREATE TABLE TB_DISPONIBILIDADE_ESTETICA (
                                             id_disponibilidade       NUMBER(10)  GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                             id_profissional_estetica NUMBER(10)  NOT NULL,
                                             nr_dia_semana            NUMBER(1)   NOT NULL,
                                             hr_inicio                VARCHAR2(5) NOT NULL,
                                             hr_fim                   VARCHAR2(5) NOT NULL,
                                             CONSTRAINT fk_disp_est_prof FOREIGN KEY (id_profissional_estetica) REFERENCES TB_PROFISSIONAL_ESTETICA(id_profissional_estetica),
                                             CONSTRAINT ck_disp_est_dia CHECK (nr_dia_semana BETWEEN 1 AND 7)
);

CREATE TABLE TB_BLOQUEIO_AGENDA_ESTETICA (
                                             id_bloqueio               NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                             id_profissional_estetica  NUMBER(10) NOT NULL,
                                             dt_inicio                 DATE       NOT NULL,
                                             dt_fim                    DATE       NOT NULL,
                                             ds_motivo                 VARCHAR2(200),
                                             CONSTRAINT fk_bloq_est_prof FOREIGN KEY (id_profissional_estetica) REFERENCES TB_PROFISSIONAL_ESTETICA(id_profissional_estetica)
);

CREATE TABLE TB_RELATORIO_ESTETICA (
                                       id_relatorio              NUMBER(10)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                       id_evento                 NUMBER(10)    NOT NULL,
                                       id_profissional_estetica  NUMBER(10)    NOT NULL,
                                       ds_problema                VARCHAR2(500) NOT NULL,
                                       ds_status                 VARCHAR2(20)  DEFAULT 'SOLICITADO' NOT NULL,
                                       id_admin_validador        NUMBER(10),
                                       dt_criacao                DATE          DEFAULT CURRENT_DATE NOT NULL,
                                       CONSTRAINT fk_rel_est_evento FOREIGN KEY (id_evento) REFERENCES TB_EVENTO_SAUDE(id_evento),
                                       CONSTRAINT fk_rel_est_prof   FOREIGN KEY (id_profissional_estetica) REFERENCES TB_PROFISSIONAL_ESTETICA(id_profissional_estetica),
                                       CONSTRAINT fk_rel_est_admin  FOREIGN KEY (id_admin_validador) REFERENCES TB_ADMIN(id_admin),
                                       CONSTRAINT ck_rel_est_status CHECK (ds_status IN ('SOLICITADO','LIBERADO','NEGADO'))
);

CREATE TABLE TB_LINK_AGENDAMENTO_VET (
                                         id_link            NUMBER(10)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                         ds_token           VARCHAR2(36)  NOT NULL UNIQUE,
                                         id_relatorio       NUMBER(10)    NOT NULL,
                                         ds_status          VARCHAR2(20)  DEFAULT 'PENDENTE' NOT NULL,
                                         dt_criacao         DATE          DEFAULT CURRENT_DATE NOT NULL,
                                         dt_expiracao       DATE          NOT NULL,
                                         id_evento_criado   NUMBER(10),
                                         CONSTRAINT fk_link_relatorio FOREIGN KEY (id_relatorio) REFERENCES TB_RELATORIO_ESTETICA(id_relatorio),
                                         CONSTRAINT fk_link_evento    FOREIGN KEY (id_evento_criado) REFERENCES TB_EVENTO_SAUDE(id_evento),
                                         CONSTRAINT ck_link_status CHECK (ds_status IN ('PENDENTE','UTILIZADO','EXPIRADO'))
);

INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Retorno veterinário', 'TERAPEUTICO', 0);