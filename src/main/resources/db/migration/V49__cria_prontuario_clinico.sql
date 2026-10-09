-- Prontuário clínico completo: orientações, resultados de exames e compartilhamento por link.
-- Rollback operacional (executar manualmente, nesta ordem):
--   DROP TABLE TB_PRONTUARIO_COMPARTILHADO;
--   DROP TABLE TB_RESULTADO_EXAME;
--   DROP TABLE TB_ORIENTACAO_CLINICA;
--   (e recriar os CHECKs de TB_PET_ACESSO / TB_PET_CONVITE sem 'VISUALIZAR_PRONTUARIO', como na V33;
--    o alargamento das colunas ds_permissao para VARCHAR2(30) é compatível e pode ser mantido)

CREATE TABLE TB_ORIENTACAO_CLINICA (
                                       id_orientacao   NUMBER(19)     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                       id_evento       NUMBER(10)     NOT NULL,
                                       id_veterinario  NUMBER(10),
                                       ds_titulo       VARCHAR2(120)  NOT NULL,
                                       ds_texto        VARCHAR2(2000) NOT NULL,
                                       ds_ator         VARCHAR2(150),
                                       dt_criacao      TIMESTAMP      NOT NULL,
                                       CONSTRAINT fk_orientacao_evento FOREIGN KEY (id_evento) REFERENCES TB_EVENTO_SAUDE(id_evento),
                                       CONSTRAINT fk_orientacao_vet    FOREIGN KEY (id_veterinario) REFERENCES TB_VETERINARIO(id_veterinario)
);
CREATE INDEX ix_orientacao_evento ON TB_ORIENTACAO_CLINICA(id_evento);

CREATE TABLE TB_RESULTADO_EXAME (
                                    id_resultado     NUMBER(19)     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                    id_pet           NUMBER(10)     NOT NULL,
                                    id_evento        NUMBER(10),
                                    id_veterinario   NUMBER(10),
                                    nm_exame         VARCHAR2(120)  NOT NULL,
                                    nm_laboratorio   VARCHAR2(120),
                                    dt_coleta        DATE,
                                    dt_resultado     DATE           NOT NULL,
                                    ds_resultado     VARCHAR2(2000),
                                    ds_interpretacao VARCHAR2(1000),
                                    nm_arquivo       VARCHAR2(255),
                                    ds_mime_type     VARCHAR2(100),
                                    nr_tamanho       NUMBER(19),
                                    ds_conteudo      BLOB,
                                    ds_ator          VARCHAR2(150),
                                    dt_criacao       TIMESTAMP      NOT NULL,
                                    CONSTRAINT fk_exame_pet    FOREIGN KEY (id_pet) REFERENCES TB_PET(id_pet),
                                    CONSTRAINT fk_exame_evento FOREIGN KEY (id_evento) REFERENCES TB_EVENTO_SAUDE(id_evento),
                                    CONSTRAINT fk_exame_vet    FOREIGN KEY (id_veterinario) REFERENCES TB_VETERINARIO(id_veterinario)
);
CREATE INDEX ix_exame_pet ON TB_RESULTADO_EXAME(id_pet, dt_resultado);

-- Link de compartilhamento do prontuário com outro atendimento.
-- O token puro NUNCA é persistido: apenas o hash SHA-256 (hex, 64 chars).
CREATE TABLE TB_PRONTUARIO_COMPARTILHADO (
                                             id_compartilhamento NUMBER(19)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                             id_pet              NUMBER(10)    NOT NULL,
                                             token_hash          VARCHAR2(64)  NOT NULL UNIQUE,
                                             ds_secoes           VARCHAR2(200) NOT NULL,
                                             ds_destinatario     VARCHAR2(150),
                                             dt_periodo_inicio   DATE,
                                             dt_periodo_fim      DATE,
                                             id_usuario_criador  NUMBER(10)    NOT NULL,
                                             criada_em           TIMESTAMP     NOT NULL,
                                             expira_em           TIMESTAMP     NOT NULL,
                                             revogada_em         TIMESTAMP,
                                             ultimo_acesso_em    TIMESTAMP,
                                             nr_acessos          NUMBER(10)    DEFAULT 0 NOT NULL,
                                             CONSTRAINT fk_prontuario_comp_pet   FOREIGN KEY (id_pet) REFERENCES TB_PET(id_pet),
                                             CONSTRAINT fk_prontuario_comp_tutor FOREIGN KEY (id_usuario_criador) REFERENCES TB_TUTOR(id_tutor)
);
CREATE INDEX ix_prontuario_comp_pet ON TB_PRONTUARIO_COMPARTILHADO(id_pet);

-- Nova permissão granular de acesso compartilhado ao pet.
-- A coluna nasceu como VARCHAR2(10) (V18) e não comporta nomes como VISUALIZAR_PERFIL/VISUALIZAR_PRONTUARIO.
ALTER TABLE TB_PET_ACESSO MODIFY (ds_permissao VARCHAR2(30));
ALTER TABLE TB_PET_CONVITE MODIFY (ds_permissao VARCHAR2(30));
ALTER TABLE TB_PET_ACESSO DROP CONSTRAINT ck_acesso_permissao;
ALTER TABLE TB_PET_ACESSO ADD CONSTRAINT ck_acesso_permissao
    CHECK (ds_permissao IN ('LEITURA','EDICAO','VISUALIZAR_PERFIL','VISUALIZAR_AGENDA',
                            'VISUALIZAR_CARTEIRA','CRIAR_EVENTO','EDITAR_PERFIL','VISUALIZAR_PRONTUARIO'));
ALTER TABLE TB_PET_CONVITE DROP CONSTRAINT ck_convite_permissao;
ALTER TABLE TB_PET_CONVITE ADD CONSTRAINT ck_convite_permissao
    CHECK (ds_permissao IN ('LEITURA','EDICAO','VISUALIZAR_PERFIL','VISUALIZAR_AGENDA',
                            'VISUALIZAR_CARTEIRA','CRIAR_EVENTO','EDITAR_PERFIL','VISUALIZAR_PRONTUARIO'));