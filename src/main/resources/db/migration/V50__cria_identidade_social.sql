-- Identidades de login social (Google / Apple) vinculadas a um tutor.
-- A identidade é (provedor, subject): o e-mail do provedor é apenas informativo.
CREATE TABLE TB_IDENTIDADE_SOCIAL (
    id_identidade_social NUMBER(10) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_tutor NUMBER(10) NOT NULL,
    ds_provedor VARCHAR2(20) NOT NULL,
    ds_subject VARCHAR2(255) NOT NULL,
    ds_email_provedor VARCHAR2(150),
    dt_vinculo TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_identidade_social_tutor FOREIGN KEY (id_tutor)
        REFERENCES TB_TUTOR(id_tutor) ON DELETE CASCADE,
    CONSTRAINT ck_identidade_social_provedor CHECK (ds_provedor IN ('GOOGLE', 'APPLE')),
    -- Uma identidade externa pertence a no máximo um tutor.
    CONSTRAINT uk_identidade_provedor_subject UNIQUE (ds_provedor, ds_subject),
    -- Um tutor tem no máximo uma identidade por provedor.
    CONSTRAINT uk_identidade_tutor_provedor UNIQUE (id_tutor, ds_provedor)
);

CREATE INDEX idx_identidade_social_tutor ON TB_IDENTIDADE_SOCIAL (id_tutor);