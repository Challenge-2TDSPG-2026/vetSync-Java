-- Auditoria com contexto de clínica: a clínica em cujo contexto a ação ocorreu (id + nome da época).
ALTER TABLE TB_AUDITORIA ADD (id_clinica NUMBER(19), nm_clinica VARCHAR2(150));
CREATE INDEX ix_auditoria_clinica ON TB_AUDITORIA(id_clinica, dt_ocorrencia);
CREATE INDEX ix_auditoria_acao ON TB_AUDITORIA(ds_acao, dt_ocorrencia);