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