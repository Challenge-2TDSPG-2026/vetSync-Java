-- VetSync - Oracle DDL and schema evolution script
-- Generated from the versioned Flyway migrations in vetSync-java.
-- Intended for academic evidence or manual execution on an EMPTY Oracle schema.
-- Normal application deployment must let Flyway execute the original migration files.
-- Do not run this file before Flyway in the same schema.


-- ============================================================================
-- V1__create_tables.sql
-- ============================================================================

-- V1: schema completo do dominio VetSync (tutor, pet, especie, raca,
-- clinica, veterinario, tipo de evento, evento de saude, medicamento,
-- prescricao e log de erros).
--
-- Diferenca em relacao ao script original: TB_TUTOR ganhou ds_senha, e
-- TB_VETERINARIO ganhou ds_email e ds_senha, para que os dois perfis
-- consigam ter login proprio no sistema.

CREATE TABLE TB_LOG_ERROS (
                              id_log         NUMBER(10)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              nm_procedure   VARCHAR2(100),
                              nm_usuario     VARCHAR2(100) DEFAULT CURRENT_USER,
                              dt_ocorrencia  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
                              nr_codigo_erro NUMBER(10),
                              ds_mensagem    VARCHAR2(500)
);

CREATE TABLE TB_TUTOR (
                          id_tutor    NUMBER(10)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          nm_tutor    VARCHAR2(100) NOT NULL,
                          ds_email    VARCHAR2(150) NOT NULL UNIQUE,
                          nr_telefone VARCHAR2(20),
                          ds_cpf      CHAR(11)      NOT NULL UNIQUE,
                          ds_senha    VARCHAR2(255) NOT NULL,
                          dt_cadastro DATE          DEFAULT CURRENT_DATE NOT NULL
);

CREATE TABLE TB_ESPECIE (
                            id_especie NUMBER(5)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            nm_especie VARCHAR2(50) NOT NULL UNIQUE
);

CREATE TABLE TB_RACA (
                         id_raca    NUMBER(5)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         nm_raca    VARCHAR2(80) NOT NULL,
                         id_especie NUMBER(5)    NOT NULL,
                         CONSTRAINT fk_raca_especie FOREIGN KEY (id_especie) REFERENCES TB_ESPECIE(id_especie)
);

CREATE TABLE TB_PET (
                        id_pet        NUMBER(10)   GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        nm_pet        VARCHAR2(80) NOT NULL,
                        dt_nascimento DATE         NOT NULL,
                        ds_sexo       CHAR(1)      CHECK (ds_sexo IN ('M','F')),
                        nr_peso_kg    NUMBER(5,2),
                        id_tutor      NUMBER(10)   NOT NULL,
                        id_raca       NUMBER(5)    NOT NULL,
                        CONSTRAINT fk_pet_tutor FOREIGN KEY (id_tutor) REFERENCES TB_TUTOR(id_tutor),
                        CONSTRAINT fk_pet_raca  FOREIGN KEY (id_raca)  REFERENCES TB_RACA(id_raca)
);

CREATE TABLE TB_CLINICA (
                            id_clinica NUMBER(10)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            nm_clinica VARCHAR2(150) NOT NULL,
                            ds_cnpj    CHAR(14)      NOT NULL UNIQUE,
                            ds_cidade  VARCHAR2(80),
                            ds_uf      CHAR(2)
);

CREATE TABLE TB_VETERINARIO (
                                id_veterinario NUMBER(10)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                nm_veterinario VARCHAR2(100) NOT NULL,
                                nr_crmv        VARCHAR2(20)  NOT NULL UNIQUE,
                                ds_email       VARCHAR2(150) NOT NULL UNIQUE,
                                ds_senha       VARCHAR2(255) NOT NULL,
                                id_clinica     NUMBER(10)    NOT NULL,
                                CONSTRAINT fk_vet_clinica FOREIGN KEY (id_clinica) REFERENCES TB_CLINICA(id_clinica)
);

CREATE TABLE TB_TIPO_EVENTO (
                                id_tipo_evento NUMBER(5)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                nm_tipo_evento VARCHAR2(80) NOT NULL,
                                ds_categoria   VARCHAR2(30) CHECK (ds_categoria IN ('PREVENTIVO','TERAPEUTICO','BEM_ESTAR','EMERGENCIA'))
);

CREATE TABLE TB_EVENTO_SAUDE (
                                 id_evento      NUMBER(10)   GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 id_pet         NUMBER(10)   NOT NULL,
                                 id_tipo_evento NUMBER(5)    NOT NULL,
                                 id_veterinario NUMBER(10),
                                 dt_evento      DATE         NOT NULL,
                                 ds_observacao  VARCHAR2(500),
                                 vl_custo       NUMBER(10,2) DEFAULT 0,
                                 CONSTRAINT fk_ev_pet  FOREIGN KEY (id_pet)         REFERENCES TB_PET(id_pet),
                                 CONSTRAINT fk_ev_tipo FOREIGN KEY (id_tipo_evento) REFERENCES TB_TIPO_EVENTO(id_tipo_evento),
                                 CONSTRAINT fk_ev_vet  FOREIGN KEY (id_veterinario) REFERENCES TB_VETERINARIO(id_veterinario)
);

CREATE TABLE TB_MEDICAMENTO (
                                id_medicamento NUMBER(10)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                nm_medicamento VARCHAR2(100) NOT NULL,
                                ds_principio   VARCHAR2(100),
                                vl_preco_ref   NUMBER(10,2)
);

CREATE TABLE TB_PRESCRICAO (
                               id_prescricao  NUMBER(10)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               id_evento      NUMBER(10)    NOT NULL,
                               id_medicamento NUMBER(10)    NOT NULL,
                               ds_posologia   VARCHAR2(200) NOT NULL,
                               dt_inicio      DATE          NOT NULL,
                               dt_fim         DATE,
                               qt_doses_dia   NUMBER(3),
                               CONSTRAINT fk_presc_evento FOREIGN KEY (id_evento)      REFERENCES TB_EVENTO_SAUDE(id_evento),
                               CONSTRAINT fk_presc_med    FOREIGN KEY (id_medicamento) REFERENCES TB_MEDICAMENTO(id_medicamento)
);

-- ============================================================================
-- V2__add_status_evento.sql
-- ============================================================================


ALTER TABLE TB_EVENTO_SAUDE ADD ds_status VARCHAR2(20) DEFAULT 'SOLICITADO' NOT NULL;
ALTER TABLE TB_EVENTO_SAUDE ADD ds_motivo_cancelamento VARCHAR2(300);

ALTER TABLE TB_EVENTO_SAUDE ADD CONSTRAINT ck_evento_status
    CHECK (ds_status IN ('SOLICITADO','CONFIRMADO','CONCLUIDO','CANCELADO'));

-- ============================================================================
-- V3__create_agenda_veterinario.sql
-- ============================================================================

CREATE TABLE TB_DISPONIBILIDADE (
                                    id_disponibilidade NUMBER(10)  GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                    id_veterinario     NUMBER(10)  NOT NULL,
                                    nr_dia_semana      NUMBER(1)   NOT NULL,
                                    hr_inicio          VARCHAR2(5) NOT NULL,
                                    hr_fim             VARCHAR2(5) NOT NULL,
                                    CONSTRAINT fk_disp_vet FOREIGN KEY (id_veterinario) REFERENCES TB_VETERINARIO(id_veterinario),
                                    CONSTRAINT ck_disp_dia CHECK (nr_dia_semana BETWEEN 1 AND 7)
);

CREATE TABLE TB_BLOQUEIO_AGENDA (
                                    id_bloqueio     NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                    id_veterinario  NUMBER(10) NOT NULL,
                                    dt_inicio       DATE       NOT NULL,
                                    dt_fim          DATE       NOT NULL,
                                    ds_motivo       VARCHAR2(200),
                                    CONSTRAINT fk_bloq_vet FOREIGN KEY (id_veterinario) REFERENCES TB_VETERINARIO(id_veterinario)
);

-- ============================================================================
-- V4__create_recompensas.sql
-- ============================================================================

ALTER TABLE TB_TIPO_EVENTO ADD nr_pontos NUMBER(5) DEFAULT 0 NOT NULL;

CREATE TABLE TB_RECOMPENSA (
                               id_recompensa   NUMBER(10)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               nm_recompensa   VARCHAR2(150) NOT NULL,
                               ds_descricao    VARCHAR2(500),
                               nr_custo_pontos NUMBER(10)    NOT NULL,
                               ds_tipo         VARCHAR2(20)  NOT NULL,
                               fl_ativo        NUMBER(1)     DEFAULT 1 NOT NULL,
                               CONSTRAINT ck_recompensa_tipo CHECK (ds_tipo IN ('PRODUTO','CUPOM_DESCONTO')),
                               CONSTRAINT ck_recompensa_custo CHECK (nr_custo_pontos > 0)
);

CREATE TABLE TB_RESGATE (
                            id_resgate                NUMBER(10)  GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            id_tutor                  NUMBER(10)  NOT NULL,
                            id_recompensa              NUMBER(10)  NOT NULL,
                            dt_resgate                 TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
                            id_veterinario_validador   NUMBER(10),
                            ds_status                  VARCHAR2(20) DEFAULT 'PENDENTE' NOT NULL,
                            CONSTRAINT fk_resgate_tutor FOREIGN KEY (id_tutor) REFERENCES TB_TUTOR(id_tutor),
                            CONSTRAINT fk_resgate_recompensa FOREIGN KEY (id_recompensa) REFERENCES TB_RECOMPENSA(id_recompensa),
                            CONSTRAINT fk_resgate_vet FOREIGN KEY (id_veterinario_validador) REFERENCES TB_VETERINARIO(id_veterinario),
                            CONSTRAINT ck_resgate_status CHECK (ds_status IN ('PENDENTE','VALIDADO','NEGADO'))
);

-- ============================================================================
-- V5__create_admin.sql
-- ============================================================================

CREATE TABLE TB_ADMIN (
                          id_admin    NUMBER(10)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          nm_admin    VARCHAR2(100) NOT NULL,
                          ds_email    VARCHAR2(150) NOT NULL UNIQUE,
                          ds_senha    VARCHAR2(255) NOT NULL,
                          dt_cadastro DATE          DEFAULT CURRENT_DATE NOT NULL
);

-- ============================================================================
-- V6__redesenhar_status_evento.sql
-- ============================================================================


UPDATE TB_EVENTO_SAUDE
SET ds_status = 'AGENDADO'
WHERE ds_status IN ('SOLICITADO', 'CONFIRMADO');


ALTER TABLE TB_EVENTO_SAUDE DROP CONSTRAINT ck_evento_status;

ALTER TABLE TB_EVENTO_SAUDE ADD CONSTRAINT ck_evento_status
    CHECK (ds_status IN ('AGENDADO', 'CONCLUIDO', 'CANCELADO'));


ALTER TABLE TB_EVENTO_SAUDE MODIFY (ds_status DEFAULT 'AGENDADO');

-- ============================================================================
-- V7__adiciona_status_prescricao.sql
-- ============================================================================


ALTER TABLE TB_PRESCRICAO ADD ds_status VARCHAR2(20) DEFAULT 'SOLICITADO' NOT NULL;

ALTER TABLE TB_PRESCRICAO ADD CONSTRAINT ck_prescricao_status
    CHECK (ds_status IN ('SOLICITADO', 'LIBERADO', 'NEGADO'));

ALTER TABLE TB_PRESCRICAO ADD id_admin_validador NUMBER;

ALTER TABLE TB_PRESCRICAO ADD CONSTRAINT fk_prescricao_admin_validador
    FOREIGN KEY (id_admin_validador) REFERENCES TB_ADMIN (id_admin);

-- ============================================================================
-- V8__cria_lancamento_pontos.sql
-- ============================================================================



CREATE TABLE TB_LANCAMENTO_PONTOS (
                                      id_lancamento      NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                      id_evento          NUMBER(10)    NOT NULL UNIQUE,
                                      nr_pontos          NUMBER(10)    NOT NULL,
                                      ds_status          VARCHAR2(20)  DEFAULT 'PENDENTE' NOT NULL,
                                      dt_lancamento      DATE          DEFAULT CURRENT_DATE NOT NULL,
                                      id_admin_validador NUMBER(10),
                                      CONSTRAINT fk_lancamento_evento FOREIGN KEY (id_evento) REFERENCES TB_EVENTO_SAUDE(id_evento),
                                      CONSTRAINT fk_lancamento_admin FOREIGN KEY (id_admin_validador) REFERENCES TB_ADMIN(id_admin),
                                      CONSTRAINT ck_lancamento_status CHECK (ds_status IN ('PENDENTE', 'LIBERADO'))
);

-- Backfill: eventos ja concluidos antes desta migration entram como LIBERADO.
INSERT INTO TB_LANCAMENTO_PONTOS (id_evento, nr_pontos, ds_status, dt_lancamento)
SELECT e.id_evento, NVL(t.nr_pontos, 0), 'LIBERADO', CURRENT_DATE
FROM TB_EVENTO_SAUDE e
         JOIN TB_TIPO_EVENTO t ON t.id_tipo_evento = e.id_tipo_evento
WHERE e.ds_status = 'CONCLUIDO';

-- ============================================================================
-- V9__cria_plano_tratamento.sql
-- ============================================================================



CREATE TABLE TB_PLANO_TRATAMENTO (
                                     id_plano        NUMBER(10)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                     id_pet          NUMBER(10)    NOT NULL,
                                     id_veterinario  NUMBER(10)    NOT NULL,
                                     nr_pontos_bonus NUMBER(10)    DEFAULT 0 NOT NULL,
                                     ds_status       VARCHAR2(20)  DEFAULT 'EM_ANDAMENTO' NOT NULL,
                                     dt_criacao      DATE          DEFAULT CURRENT_DATE NOT NULL,
                                     CONSTRAINT fk_plano_pet FOREIGN KEY (id_pet) REFERENCES TB_PET(id_pet),
                                     CONSTRAINT fk_plano_veterinario FOREIGN KEY (id_veterinario) REFERENCES TB_VETERINARIO(id_veterinario),
                                     CONSTRAINT ck_plano_status CHECK (ds_status IN ('EM_ANDAMENTO', 'CONCLUIDO', 'QUEBRADO'))
);

CREATE TABLE TB_PLANO_ITEM (
                               id_item        NUMBER(10)   GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               id_plano       NUMBER(10)   NOT NULL,
                               nr_ordem       NUMBER(3)    NOT NULL,
                               id_tipo_evento NUMBER(10)   NOT NULL,
                               id_evento      NUMBER(10),
                               ds_status      VARCHAR2(20) DEFAULT 'PENDENTE' NOT NULL,
                               CONSTRAINT fk_item_plano FOREIGN KEY (id_plano) REFERENCES TB_PLANO_TRATAMENTO(id_plano),
                               CONSTRAINT fk_item_tipo_evento FOREIGN KEY (id_tipo_evento) REFERENCES TB_TIPO_EVENTO(id_tipo_evento),
                               CONSTRAINT fk_item_evento FOREIGN KEY (id_evento) REFERENCES TB_EVENTO_SAUDE(id_evento),
                               CONSTRAINT uk_item_plano_ordem UNIQUE (id_plano, nr_ordem),
                               CONSTRAINT uk_item_evento UNIQUE (id_evento),
                               CONSTRAINT ck_item_status CHECK (ds_status IN ('PENDENTE', 'AGENDADO', 'CONCLUIDO', 'QUEBRADO'))
);


ALTER TABLE TB_LANCAMENTO_PONTOS MODIFY (id_evento NULL);

ALTER TABLE TB_LANCAMENTO_PONTOS ADD id_plano_tratamento NUMBER(10);

ALTER TABLE TB_LANCAMENTO_PONTOS ADD CONSTRAINT fk_lancamento_plano
    FOREIGN KEY (id_plano_tratamento) REFERENCES TB_PLANO_TRATAMENTO(id_plano);

ALTER TABLE TB_LANCAMENTO_PONTOS ADD CONSTRAINT uk_lancamento_plano UNIQUE (id_plano_tratamento);

ALTER TABLE TB_LANCAMENTO_PONTOS ADD CONSTRAINT ck_lancamento_origem CHECK (
    (id_evento IS NOT NULL AND id_plano_tratamento IS NULL)
        OR
    (id_evento IS NULL AND id_plano_tratamento IS NOT NULL)
    );

-- ============================================================================
-- V10__adiciona_horario_evento.sql
-- ============================================================================


ALTER TABLE TB_EVENTO_SAUDE ADD hr_evento VARCHAR2(5);

-- ============================================================================
-- V11__adiciona_profissional_estetica.sql
-- ============================================================================

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

-- ============================================================================
-- V12__adiciona_servicos_estetica.sql
-- ============================================================================

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

-- ============================================================================
-- V13__adiciona_imagem_recompensa.sql
-- ============================================================================

ALTER TABLE TB_RECOMPENSA ADD (
    ds_imagem      BLOB,
    ds_imagem_tipo VARCHAR2(100)
);

-- ============================================================================
-- V14__regras_de_pontos.sql
-- ============================================================================

-- =========================================================
-- Regra de pontos: o que o tutor GANHA (TB_TIPO_EVENTO.nr_pontos)
-- =========================================================
-- Observações:
--  * 'Comparecer à consulta' definido como 25 pontos (ajustado, era 50 na tabela original).
--  * 'Banho e Tosa' representa os serviços de estética e vale 30 pontos.
--    O nome precisa conter a palavra "banho" para ser reconhecido como
--    serviço de estética por EventoService.isServicoEstetica(...).
--  * 'Completar todas as etapas do plano' (200 pontos bonus) não entra aqui:
--    esse valor é definido pelo veterinário por plano, no campo
--    nr_pontos_bonus de TB_PLANO_TRATAMENTO (ver PlanoTratamentoService.criar).

INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Comparecer à consulta', 'PREVENTIVO', 25);

INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Realizar vacinação', 'PREVENTIVO', 100);

INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Realizar exames', 'TERAPEUTICO', 100);

INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Seguir o plano médico', 'TERAPEUTICO', 100);

INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Retorno dentro do prazo', 'TERAPEUTICO', 50);

INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Manter acompanhamento preventivo', 'PREVENTIVO', 100);

INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Realizar procedimento preventivo', 'PREVENTIVO', 150);

INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Banho e Tosa (Estética)', 'BEM_ESTAR', 30);

-- =========================================================
-- Regra de pontos: o que o tutor GASTA (TB_RECOMPENSA.nr_custo_pontos)
-- =========================================================

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Bifinho', 'Petisco tradicional para cães e gatos', 50, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Snack cremoso', 'Petisco cremoso para recompensar o pet', 100, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Petiscos naturais', 'Petiscos naturais e saudáveis', 120, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Bolinha', 'Bolinha de brinquedo', 120, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Bola cravo', 'Bola com cravos de borracha para mastigar', 120, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Mordedor', 'Mordedor resistente para cães', 200, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Mordedor de corda', 'Mordedor de corda trançada', 150, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Pelúcia', 'Brinquedo de pelúcia', 250, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Rasqueadeira', 'Escova/rasqueadeira para cuidado do pelo', 200, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Coleira simples', 'Coleira simples ajustável', 150, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Cupom 15% OFF', 'Cupom de 15% de desconto em serviços/produtos da clínica', 300, 'CUPOM_DESCONTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Cupom 25% OFF', 'Cupom de 25% de desconto em serviços/produtos da clínica', 500, 'CUPOM_DESCONTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Cupom 50% OFF', 'Cupom de 50% de desconto em serviços/produtos da clínica', 1000, 'CUPOM_DESCONTO', 1);

-- ============================================================================
-- V15__adiciona_vermifugacao.sql
-- ============================================================================

-- Vermifugação não fazia parte da lista original de ações do tutor.
-- Adicionada aqui como tipo preventivo, mesmo peso de 'Realizar vacinação'.
INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Realizar vermifugação', 'PREVENTIVO', 100);

-- ============================================================================
-- V16__consolida_tipos_evento_reais.sql
-- ============================================================================

-- Consolida os tipos de evento com os nomes REAIS usados no app,
-- garantindo que qualquer evento de saúde vinculado a um desses tipos
-- receba os pontos correspondentes ao ser concluído
-- (EventoService.concluir -> PontosService.lancarPendente lê tipoEvento.nr_pontos).

-- 'Retorno veterinário' já existe (criado na V11, usado por LinkAgendamentoVetService
-- via nome fixo "Retorno veterinário" — não pode ser renomeado). Estava com 0 pontos.
UPDATE TB_TIPO_EVENTO
SET nr_pontos = 50
WHERE nm_tipo_evento = 'Retorno veterinário';

-- 'Realizar vacinação' (nome antigo, inventado) -> renomeado para 'Vacina'
UPDATE TB_TIPO_EVENTO
SET nm_tipo_evento = 'Vacina', nr_pontos = 100
WHERE nm_tipo_evento = 'Realizar vacinação';

-- 'Comparecer à consulta' (nome antigo, inventado) -> renomeado para 'Consulta de rotina'
UPDATE TB_TIPO_EVENTO
SET nm_tipo_evento = 'Consulta de rotina', nr_pontos = 25
WHERE nm_tipo_evento = 'Comparecer à consulta';

-- 'Banho e Tosa (Estética)' (nome antigo, inventado) -> renomeado para 'Banho e tosa'
-- Mantém a palavra "banho" no nome, exigida por EventoService.isServicoEstetica(...)
UPDATE TB_TIPO_EVENTO
SET nm_tipo_evento = 'Banho e tosa', nr_pontos = 30
WHERE nm_tipo_evento = 'Banho e Tosa (Estética)';

-- 'Cirurgia' ainda não existia no catálogo — valor sugerido, ajuste se necessário
INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Cirurgia', 'TERAPEUTICO', 200);

-- 'Atendimento de emergência' ainda não existia no catálogo — valor sugerido, ajuste se necessário
INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Atendimento de emergência', 'EMERGENCIA', 100);

-- ============================================================================
-- V17__cria_carteira_compartilhada.sql
-- ============================================================================

-- Carteira de vacinação compartilhável por QR Code / link público.
-- O token puro NUNCA é persistido: apenas o hash SHA-256 (hex, 64 chars) é gravado.

CREATE TABLE TB_CARTEIRA_COMPARTILHADA (
                                           id_carteira_compartilhada NUMBER(19)     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                           id_pet                    NUMBER(10)     NOT NULL,
                                           token_hash                VARCHAR2(64)   NOT NULL UNIQUE,
                                           criada_em                 TIMESTAMP      NOT NULL,
                                           expira_em                 TIMESTAMP      NOT NULL,
                                           revogada_em                TIMESTAMP     NULL,
                                           id_usuario_criador        NUMBER(10)     NOT NULL,
                                           ultimo_acesso_em          TIMESTAMP      NULL,
                                           CONSTRAINT fk_carteira_compartilhada_pet FOREIGN KEY (id_pet) REFERENCES TB_PET(id_pet),
                                           CONSTRAINT fk_carteira_compartilhada_tutor FOREIGN KEY (id_usuario_criador) REFERENCES TB_TUTOR(id_tutor)
);

-- Acelera a busca da carteira pelo pet (usada ao criar/renovar/consultar/revogar).
CREATE INDEX ix_carteira_compartilhada_pet ON TB_CARTEIRA_COMPARTILHADA (id_pet);

-- ============================================================================
-- V18__cria_convites_e_acessos_pet.sql
-- ============================================================================

-- Convite de acesso (cuidador/cônjuge) a um pet já cadastrado.
-- O convite é um pré-cadastro: o tutor dono informa e-mail/relação/permissão,
-- o convidado recebe um link com token, e só ao aceitar é que a conta dele
-- (TB_TUTOR) e o acesso (TB_PET_ACESSO) são criados. O token puro NUNCA é
-- persistido: apenas o hash SHA-256 (hex, 64 chars), no mesmo padrão já usado
-- pela carteira compartilhável (V17).
--
-- O pet continua tendo apenas um proprietário principal em TB_PET.id_tutor.
-- O acesso de cuidador/cônjuge é sempre um registro adicional em TB_PET_ACESSO,
-- nunca substitui o dono.

CREATE TABLE TB_PET_CONVITE (
                                id_convite         NUMBER(19)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                id_pet             NUMBER(10)    NOT NULL,
                                id_tutor_origem    NUMBER(10)    NOT NULL,
                                id_tutor_destino   NUMBER(10)    NULL,
                                ds_email_destino   VARCHAR2(150) NOT NULL,
                                ds_relacao         VARCHAR2(20)  NOT NULL,
                                ds_permissao       VARCHAR2(10)  NOT NULL,
                                ds_token_hash      VARCHAR2(64)  NOT NULL,
                                ds_status          VARCHAR2(15)  NOT NULL,
                                dt_criacao         TIMESTAMP     NOT NULL,
                                dt_expiracao       TIMESTAMP     NOT NULL,
                                dt_aceite          TIMESTAMP     NULL,
                                CONSTRAINT fk_convite_pet           FOREIGN KEY (id_pet)           REFERENCES TB_PET(id_pet),
                                CONSTRAINT fk_convite_tutor_origem  FOREIGN KEY (id_tutor_origem)  REFERENCES TB_TUTOR(id_tutor),
                                CONSTRAINT fk_convite_tutor_destino FOREIGN KEY (id_tutor_destino) REFERENCES TB_TUTOR(id_tutor),
                                CONSTRAINT uq_convite_token_hash    UNIQUE (ds_token_hash),
                                CONSTRAINT ck_convite_relacao   CHECK (ds_relacao IN ('CUIDADOR','CONJUGE','OUTRO')),
                                CONSTRAINT ck_convite_permissao CHECK (ds_permissao IN ('LEITURA','EDICAO')),
                                CONSTRAINT ck_convite_status    CHECK (ds_status IN ('PENDENTE','ACEITO','RECUSADO','EXPIRADO','CANCELADO'))
);

CREATE INDEX ix_convite_pet   ON TB_PET_CONVITE (id_pet);
CREATE INDEX ix_convite_tutor ON TB_PET_CONVITE (id_tutor_destino);
CREATE INDEX ix_convite_email ON TB_PET_CONVITE (ds_email_destino);

CREATE TABLE TB_PET_ACESSO (
                               id_acesso     NUMBER(19)   GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               id_pet        NUMBER(10)   NOT NULL,
                               id_tutor      NUMBER(10)   NOT NULL,
                               ds_relacao    VARCHAR2(20) NOT NULL,
                               ds_permissao  VARCHAR2(10) NOT NULL,
                               ds_status     VARCHAR2(10) NOT NULL,
                               dt_concedido  TIMESTAMP    NOT NULL,
                               dt_revogado   TIMESTAMP    NULL,
                               CONSTRAINT fk_acesso_pet        FOREIGN KEY (id_pet)   REFERENCES TB_PET(id_pet),
                               CONSTRAINT fk_acesso_tutor      FOREIGN KEY (id_tutor) REFERENCES TB_TUTOR(id_tutor),
                               CONSTRAINT uq_acesso_pet_tutor  UNIQUE (id_pet, id_tutor),
                               CONSTRAINT ck_acesso_relacao    CHECK (ds_relacao IN ('CUIDADOR','CONJUGE','OUTRO')),
                               CONSTRAINT ck_acesso_permissao  CHECK (ds_permissao IN ('LEITURA','EDICAO')),
                               CONSTRAINT ck_acesso_status     CHECK (ds_status IN ('ATIVO','REVOGADO'))
);

CREATE INDEX ix_acesso_pet   ON TB_PET_ACESSO (id_pet);
CREATE INDEX ix_acesso_tutor ON TB_PET_ACESSO (id_tutor);

-- ============================================================================
-- V19__adiciona_anexo_pdf_prescricao.sql
-- ============================================================================

-- V19__adiciona_anexo_pdf_prescricao.sql
ALTER TABLE TB_PRESCRICAO ADD (
    ds_anexo_pdf      BLOB,
    ds_anexo_pdf_nome VARCHAR2(255),
    ds_anexo_pdf_tipo VARCHAR2(100)
);

-- ============================================================================
-- V20__adiciona_foto_pet.sql
-- ============================================================================

-- Foto do pet (perfil e carteirinha). Use o próximo número livre da pasta db/migration.
ALTER TABLE TB_PET ADD (
    ds_foto      BLOB,
    ds_foto_tipo VARCHAR2(100)
);

-- ============================================================================
-- V21__adiciona_especialidade_veterinario.sql
-- ============================================================================

-- Especialidade do veterinário (ex.: Clínica Geral, Cardiologia, Dermatologia...),
-- usada para filtrar os profissionais na hora de agendar um evento.
ALTER TABLE TB_VETERINARIO ADD (
    ds_especialidade VARCHAR2(50)
);

-- ============================================================================
-- V22__cria_notificacoes_dispositivos_preferencias.sql
-- ============================================================================

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

-- ============================================================================
-- V24__completa_detalhes_clinicos_evento.sql
-- ============================================================================

ALTER TABLE TB_EVENTO_SAUDE ADD (
    ds_observacao_tutor VARCHAR2(500),
    ds_observacao_clinica VARCHAR2(1000),
    ds_diagnostico VARCHAR2(500),
    ds_conduta VARCHAR2(1000),
    dt_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ============================================================================
-- V30__cria_acoes_ia.sql
-- ============================================================================

CREATE TABLE TB_ACAO_IA (
    id_acao NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    ds_acao VARCHAR2(40) NOT NULL,
    ds_resumo VARCHAR2(500) NOT NULL,
    ds_dados CLOB NOT NULL,
    ds_status VARCHAR2(20) NOT NULL,
    ds_email_usuario VARCHAR2(150) NOT NULL,
    dt_expiracao TIMESTAMP NOT NULL,
    dt_criacao TIMESTAMP NOT NULL
);

CREATE INDEX IX_ACAO_IA_USUARIO ON TB_ACAO_IA (ds_email_usuario);

-- ============================================================================
-- V31__cria_carteira_perfil_e_peso.sql
-- ============================================================================

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

-- ============================================================================
-- V32__cria_historico_anexos_auditoria.sql
-- ============================================================================

CREATE TABLE TB_EVENTO_HISTORICO (
    id_historico NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_evento NUMBER(10) NOT NULL,
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
    id_evento NUMBER(10) NOT NULL,
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

-- ============================================================================
-- V33__expande_permissoes_pet.sql
-- ============================================================================

ALTER TABLE TB_PET_ACESSO DROP CONSTRAINT ck_acesso_permissao;
ALTER TABLE TB_PET_ACESSO ADD CONSTRAINT ck_acesso_permissao
    CHECK (ds_permissao IN ('LEITURA','EDICAO','VISUALIZAR_PERFIL','VISUALIZAR_AGENDA',
                            'VISUALIZAR_CARTEIRA','CRIAR_EVENTO','EDITAR_PERFIL'));
ALTER TABLE TB_PET_CONVITE DROP CONSTRAINT ck_convite_permissao;
ALTER TABLE TB_PET_CONVITE ADD CONSTRAINT ck_convite_permissao
    CHECK (ds_permissao IN ('LEITURA','EDICAO','VISUALIZAR_PERFIL','VISUALIZAR_AGENDA',
                            'VISUALIZAR_CARTEIRA','CRIAR_EVENTO','EDITAR_PERFIL'));

-- ============================================================================
-- V34__adiciona_modalidade_e_duracao_agendamento.sql
-- ============================================================================

-- Modalidade de agendamento é independente da categoria clínica já existente.
ALTER TABLE TB_TIPO_EVENTO ADD (
    ds_modalidade_agendamento VARCHAR2(30),
    nr_duracao_minutos NUMBER(3) DEFAULT 30 NOT NULL
);

ALTER TABLE TB_TIPO_EVENTO ADD CONSTRAINT ck_tipo_evento_modalidade_agendamento
    CHECK (ds_modalidade_agendamento IS NULL OR ds_modalidade_agendamento IN ('CLINICO_GERAL'));

-- V16 garante este nome para a consulta regular do catálogo atual.
UPDATE TB_TIPO_EVENTO
SET ds_modalidade_agendamento = 'CLINICO_GERAL'
WHERE nm_tipo_evento = 'Consulta de rotina';

-- ============================================================================
-- V35__adiciona_endereco_tutor.sql
-- ============================================================================

ALTER TABLE TB_TUTOR ADD (
    nr_cep          VARCHAR2(8),
    ds_logradouro   VARCHAR2(150),
    nr_endereco     VARCHAR2(20),
    ds_complemento  VARCHAR2(100),
    ds_bairro       VARCHAR2(100),
    nm_cidade       VARCHAR2(100),
    sg_uf           CHAR(2)
);

-- ============================================================================
-- V36__cria_responsaveis_tutor.sql
-- ============================================================================

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

-- ============================================================================
-- V37__invalida_sessoes_apos_troca_de_senha.sql
-- ============================================================================

ALTER TABLE TB_TUTOR ADD dt_senha_alterada_em TIMESTAMP(6) NULL;
ALTER TABLE TB_VETERINARIO ADD dt_senha_alterada_em TIMESTAMP(6) NULL;
ALTER TABLE TB_PROFISSIONAL_ESTETICA ADD dt_senha_alterada_em TIMESTAMP(6) NULL;
ALTER TABLE TB_ADMIN ADD dt_senha_alterada_em TIMESTAMP(6) NULL;

-- ============================================================================
-- V38__cria_vinculo_tutor_clinica.sql
-- ============================================================================

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
-- ds_token_hash já possui índice criado pela restrição UNIQUE da tabela.

-- ============================================================================
-- V39__registra_clinica_origem_evento.sql
-- ============================================================================

ALTER TABLE TB_EVENTO_SAUDE ADD (id_clinica NUMBER(10));

UPDATE TB_EVENTO_SAUDE e
SET id_clinica = (
    SELECT v.id_clinica FROM TB_VETERINARIO v WHERE v.id_veterinario = e.id_veterinario
)
WHERE e.id_veterinario IS NOT NULL;

UPDATE TB_EVENTO_SAUDE e
SET id_clinica = (
    SELECT p.id_clinica FROM TB_PROFISSIONAL_ESTETICA p WHERE p.id_profissional_estetica = e.id_profissional_estetica
)
WHERE e.id_clinica IS NULL AND e.id_profissional_estetica IS NOT NULL;

ALTER TABLE TB_EVENTO_SAUDE ADD CONSTRAINT fk_evento_clinica_origem
    FOREIGN KEY (id_clinica) REFERENCES TB_CLINICA(id_clinica);
CREATE INDEX idx_evento_clinica_origem ON TB_EVENTO_SAUDE (id_clinica);

-- ============================================================================
-- V40__vinculo_codigo_contrato_inativo.sql
-- ============================================================================

-- 1) Clínicas existentes e novas passam a nascer como contratantes INATIVAS.
--    O Admin confirma e ativa cada contrato manualmente.
UPDATE TB_CLINICA SET st_contratante = 'I';
ALTER TABLE TB_CLINICA MODIFY (st_contratante DEFAULT 'I');

-- 2) Uma única chave ativa por clínica (a emissão de novo código revoga o anterior).
UPDATE TB_CODIGO_VINCULO_CLINICA c
   SET st_ativo = 'I', dt_revogacao = CURRENT_TIMESTAMP
 WHERE st_ativo = 'A'
   AND EXISTS (SELECT 1 FROM TB_CODIGO_VINCULO_CLINICA o
                WHERE o.id_clinica = c.id_clinica
                  AND o.st_ativo = 'A'
                  AND o.id_codigo_vinculo > c.id_codigo_vinculo);

CREATE UNIQUE INDEX uq_codigo_ativo_clinica ON TB_CODIGO_VINCULO_CLINICA
    (CASE WHEN st_ativo = 'A' THEN id_clinica END);

-- 3) A sessão temporária carrega a referência do código validado.
--    Sessões em aberto (sem referência) são expiradas.
ALTER TABLE TB_SESSAO_VINCULO_CLINICA ADD (id_codigo_vinculo NUMBER(10));
ALTER TABLE TB_SESSAO_VINCULO_CLINICA ADD CONSTRAINT fk_sessao_codigo
    FOREIGN KEY (id_codigo_vinculo) REFERENCES TB_CODIGO_VINCULO_CLINICA(id_codigo_vinculo);
UPDATE TB_SESSAO_VINCULO_CLINICA SET dt_expiracao = CURRENT_TIMESTAMP WHERE dt_utilizacao IS NULL;
CREATE INDEX idx_sessao_codigo ON TB_SESSAO_VINCULO_CLINICA (id_codigo_vinculo);

-- 4) Cada novo vínculo fica relacionado ao código usado (nulo nos vínculos legados).
ALTER TABLE TB_VINCULO_TUTOR_CLINICA ADD (id_codigo_vinculo NUMBER(10));
ALTER TABLE TB_VINCULO_TUTOR_CLINICA ADD CONSTRAINT fk_vinculo_codigo
    FOREIGN KEY (id_codigo_vinculo) REFERENCES TB_CODIGO_VINCULO_CLINICA(id_codigo_vinculo);
CREATE INDEX idx_vinculo_codigo ON TB_VINCULO_TUTOR_CLINICA (id_codigo_vinculo);

-- ============================================================================
-- V41__ativa_contratos_das_clinicas.sql
-- ============================================================================

-- Corrige a regra introduzida na V40: todas as clínicas existentes
-- e as cadastradas a partir desta versão possuem contrato ativo por padrão.
UPDATE TB_CLINICA
   SET st_contratante = 'A'
 WHERE st_contratante <> 'A';

ALTER TABLE TB_CLINICA MODIFY (st_contratante DEFAULT 'A');

-- ============================================================================
-- V42__clinica_nova_inicia_inativa.sql
-- ============================================================================

-- Clínicas cadastradas a partir desta versão nascem com contrato INATIVO.
-- O Admin confirma e ativa o contrato manualmente (PATCH /vinculos-clinica/clinicas/{id}/contrato).
-- As clínicas já existentes mantêm o contrato atual (ativado na V41).
ALTER TABLE TB_CLINICA MODIFY (st_contratante DEFAULT 'I');

-- ============================================================================
-- V43__escopo_clinica_pontos_e_catalogos.sql
-- ============================================================================

-- Pontos passam a ser por tutor DENTRO de cada clínica (não acompanham o tutor ao trocar de clínica).
-- Decisão de negócio: os pontos antigos são apagados para que a nova regra comece do zero.
-- (Resgates antigos ficam como histórico: o saldo só considera resgates de recompensas da própria clínica.)
DELETE FROM TB_LANCAMENTO_PONTOS;

ALTER TABLE TB_LANCAMENTO_PONTOS ADD (id_clinica NUMBER(10));
ALTER TABLE TB_LANCAMENTO_PONTOS ADD CONSTRAINT fk_lancamento_clinica
    FOREIGN KEY (id_clinica) REFERENCES TB_CLINICA(id_clinica);
CREATE INDEX idx_lancamento_clinica ON TB_LANCAMENTO_PONTOS (id_clinica, ds_status);

-- Plano de tratamento fica na clínica do veterinário que o criou.
ALTER TABLE TB_PLANO_TRATAMENTO ADD (id_clinica NUMBER(10));
UPDATE TB_PLANO_TRATAMENTO p
SET id_clinica = (SELECT v.id_clinica FROM TB_VETERINARIO v WHERE v.id_veterinario = p.id_veterinario);
ALTER TABLE TB_PLANO_TRATAMENTO ADD CONSTRAINT fk_plano_clinica
    FOREIGN KEY (id_clinica) REFERENCES TB_CLINICA(id_clinica);
CREATE INDEX idx_plano_clinica ON TB_PLANO_TRATAMENTO (id_clinica);

-- Catálogos passam a pertencer a uma clínica. Registros antigos ficam sem clínica (coluna nula):
--  * recompensas sem clínica deixam de aparecer para o tutor (não há saldo a debitar) até o admin escolher a clínica;
--  * medicamentos sem clínica continuam visíveis para os veterinários até o admin escolher a clínica.
ALTER TABLE TB_RECOMPENSA ADD (id_clinica NUMBER(10));
ALTER TABLE TB_RECOMPENSA ADD CONSTRAINT fk_recompensa_clinica
    FOREIGN KEY (id_clinica) REFERENCES TB_CLINICA(id_clinica);
CREATE INDEX idx_recompensa_clinica ON TB_RECOMPENSA (id_clinica);

ALTER TABLE TB_MEDICAMENTO ADD (id_clinica NUMBER(10));
ALTER TABLE TB_MEDICAMENTO ADD CONSTRAINT fk_medicamento_clinica
    FOREIGN KEY (id_clinica) REFERENCES TB_CLINICA(id_clinica);
CREATE INDEX idx_medicamento_clinica ON TB_MEDICAMENTO (id_clinica);

ALTER TABLE TB_TIPO_VACINA ADD (id_clinica NUMBER(10));
ALTER TABLE TB_TIPO_VACINA ADD CONSTRAINT fk_tipo_vacina_clinica
    FOREIGN KEY (id_clinica) REFERENCES TB_CLINICA(id_clinica);
CREATE INDEX idx_tipo_vacina_clinica ON TB_TIPO_VACINA (id_clinica);
