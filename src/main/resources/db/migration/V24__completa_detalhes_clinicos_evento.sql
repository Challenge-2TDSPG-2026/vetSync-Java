ALTER TABLE TB_EVENTO_SAUDE ADD (
    ds_observacao_tutor VARCHAR2(500),
    ds_observacao_clinica VARCHAR2(1000),
    ds_diagnostico VARCHAR2(500),
    ds_conduta VARCHAR2(1000),
    dt_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
