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
