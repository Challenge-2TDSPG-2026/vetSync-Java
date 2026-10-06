-- Lançamentos de pontos: novos estados (BLOQUEADO, EXPIRADO), data de liberação e validade.
-- A validade passa a contar a partir da liberação (padrão de 365 dias, configurável em app.pontos.validade-dias).
-- O estado EXPIRADO é derivado na leitura (LIBERADO com dt_validade vencida), então nenhum job precisa gravá-lo.

ALTER TABLE TB_LANCAMENTO_PONTOS ADD (
    dt_liberacao       DATE,
    dt_validade        DATE,
    dt_bloqueio        DATE,
    ds_motivo_bloqueio VARCHAR2(300),
    id_admin_bloqueio  NUMBER(10)
);

ALTER TABLE TB_LANCAMENTO_PONTOS ADD CONSTRAINT fk_lancamento_admin_bloqueio
    FOREIGN KEY (id_admin_bloqueio) REFERENCES TB_ADMIN(id_admin);

ALTER TABLE TB_LANCAMENTO_PONTOS DROP CONSTRAINT ck_lancamento_status;
ALTER TABLE TB_LANCAMENTO_PONTOS ADD CONSTRAINT ck_lancamento_status
    CHECK (ds_status IN ('PENDENTE', 'LIBERADO', 'BLOQUEADO', 'EXPIRADO'));

-- Backfill: lançamentos já liberados ganham liberação na data do lançamento e 365 dias de validade.
UPDATE TB_LANCAMENTO_PONTOS
SET dt_liberacao = dt_lancamento,
    dt_validade  = dt_lancamento + 365
WHERE ds_status = 'LIBERADO';

CREATE INDEX idx_lancamento_validade ON TB_LANCAMENTO_PONTOS (id_clinica, ds_status, dt_validade);