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