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