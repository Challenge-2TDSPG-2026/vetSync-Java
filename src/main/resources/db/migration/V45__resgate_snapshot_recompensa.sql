-- Resgate passa a guardar uma cópia dos dados da recompensa e o custo aplicado NO MOMENTO do resgate.
-- Assim, editar ou inativar a recompensa depois não altera o histórico nem o saldo de pontos de quem já resgatou.

ALTER TABLE TB_RESGATE ADD (
    nm_recompensa           VARCHAR2(150),
    ds_descricao_recompensa VARCHAR2(500),
    ds_tipo_recompensa      VARCHAR2(20),
    nr_custo_pontos         NUMBER(10),
    id_clinica              NUMBER(10)
);

-- Backfill: resgates já existentes recebem os valores atuais da recompensa (melhor informação disponível).
UPDATE TB_RESGATE r SET
                        nm_recompensa           = (SELECT x.nm_recompensa   FROM TB_RECOMPENSA x WHERE x.id_recompensa = r.id_recompensa),
                        ds_descricao_recompensa = (SELECT x.ds_descricao    FROM TB_RECOMPENSA x WHERE x.id_recompensa = r.id_recompensa),
                        ds_tipo_recompensa      = (SELECT x.ds_tipo         FROM TB_RECOMPENSA x WHERE x.id_recompensa = r.id_recompensa),
                        nr_custo_pontos         = (SELECT x.nr_custo_pontos FROM TB_RECOMPENSA x WHERE x.id_recompensa = r.id_recompensa),
                        id_clinica              = (SELECT x.id_clinica      FROM TB_RECOMPENSA x WHERE x.id_recompensa = r.id_recompensa);

ALTER TABLE TB_RESGATE MODIFY (nm_recompensa NOT NULL, nr_custo_pontos NOT NULL);
ALTER TABLE TB_RESGATE ADD CONSTRAINT ck_resgate_custo CHECK (nr_custo_pontos > 0);
ALTER TABLE TB_RESGATE ADD CONSTRAINT ck_resgate_tipo CHECK (ds_tipo_recompensa IN ('PRODUTO','CUPOM_DESCONTO'));
ALTER TABLE TB_RESGATE ADD CONSTRAINT fk_resgate_clinica
    FOREIGN KEY (id_clinica) REFERENCES TB_CLINICA(id_clinica);
CREATE INDEX idx_resgate_clinica ON TB_RESGATE (id_clinica, ds_status);