CREATE TABLE TB_SERVICO_ESTETICA (
                                     id_servico  NUMBER(10)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                     nm_servico  VARCHAR2(100) NOT NULL UNIQUE,
                                     tp_servico  VARCHAR2(10)  NOT NULL,
                                     CONSTRAINT ck_serv_est_tipo CHECK (tp_servico IN ('BASE','EXTRA'))
);

CREATE TABLE TB_PROFISSIONAL_SERVICO (
                                         id_profissional_estetica NUMBER(10) NOT NULL,
                                         id_servico               NUMBER(10) NOT NULL,
                                         CONSTRAINT pk_prof_servico PRIMARY KEY (id_profissional_estetica, id_servico),
                                         CONSTRAINT fk_ps_prof FOREIGN KEY (id_profissional_estetica) REFERENCES TB_PROFISSIONAL_ESTETICA(id_profissional_estetica),
                                         CONSTRAINT fk_ps_serv FOREIGN KEY (id_servico) REFERENCES TB_SERVICO_ESTETICA(id_servico)
);

CREATE TABLE TB_EVENTO_SERVICO (
                                   id_evento  NUMBER(10) NOT NULL,
                                   id_servico NUMBER(10) NOT NULL,
                                   CONSTRAINT pk_evento_servico PRIMARY KEY (id_evento, id_servico),
                                   CONSTRAINT fk_es_evento FOREIGN KEY (id_evento) REFERENCES TB_EVENTO_SAUDE(id_evento),
                                   CONSTRAINT fk_es_serv   FOREIGN KEY (id_servico) REFERENCES TB_SERVICO_ESTETICA(id_servico)
);

INSERT INTO TB_SERVICO_ESTETICA (nm_servico, tp_servico) VALUES ('Banho', 'BASE');
INSERT INTO TB_SERVICO_ESTETICA (nm_servico, tp_servico) VALUES ('Banho e Tosa na Tesoura', 'BASE');
INSERT INTO TB_SERVICO_ESTETICA (nm_servico, tp_servico) VALUES ('Banho e Tosa na Máquina', 'BASE');

INSERT INTO TB_SERVICO_ESTETICA (nm_servico, tp_servico) VALUES ('Banho anti-pulgas', 'EXTRA');
INSERT INTO TB_SERVICO_ESTETICA (nm_servico, tp_servico) VALUES ('Desembaraçar pelos (1h)', 'EXTRA');
INSERT INTO TB_SERVICO_ESTETICA (nm_servico, tp_servico) VALUES ('Desembaraçar pelos (15 min)', 'EXTRA');
INSERT INTO TB_SERVICO_ESTETICA (nm_servico, tp_servico) VALUES ('Hidratação de pequeno porte', 'EXTRA');
INSERT INTO TB_SERVICO_ESTETICA (nm_servico, tp_servico) VALUES ('Hidratação porte médio', 'EXTRA');
INSERT INTO TB_SERVICO_ESTETICA (nm_servico, tp_servico) VALUES ('Hidratação porte grande', 'EXTRA');
INSERT INTO TB_SERVICO_ESTETICA (nm_servico, tp_servico) VALUES ('Higiene bucal', 'EXTRA');
INSERT INTO TB_SERVICO_ESTETICA (nm_servico, tp_servico) VALUES ('Corte de unhas e tosa higiênica', 'EXTRA');
INSERT INTO TB_SERVICO_ESTETICA (nm_servico, tp_servico) VALUES ('Pintura de unhas', 'EXTRA');
INSERT INTO TB_SERVICO_ESTETICA (nm_servico, tp_servico) VALUES ('Pintura de pelagem', 'EXTRA');