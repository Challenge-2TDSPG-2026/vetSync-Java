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