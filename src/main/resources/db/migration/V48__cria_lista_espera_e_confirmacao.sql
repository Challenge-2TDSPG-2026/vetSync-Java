-- Lista de espera por vaga + etapa de confirmação do agendamento.

CREATE TABLE TB_LISTA_ESPERA (
                                 id_espera NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 id_tutor NUMBER(10) NOT NULL,
                                 id_pet NUMBER(10) NOT NULL,
                                 id_servico_clinica NUMBER(10) NOT NULL,
                                 id_veterinario NUMBER(10),
                                 id_profissional_estetica NUMBER(10),
                                 dt_desejada_inicio DATE NOT NULL,
                                 dt_desejada_fim DATE NOT NULL,
                                 hr_min VARCHAR2(5),
                                 hr_max VARCHAR2(5),
                                 ds_status VARCHAR2(20) DEFAULT 'AGUARDANDO' NOT NULL,
                                 dt_vaga DATE,
                                 hr_vaga VARCHAR2(5),
                                 ds_tipo_profissional_vaga VARCHAR2(20),
                                 id_profissional_vaga NUMBER(10),
                                 dt_notificacao TIMESTAMP,
                                 dt_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                 CONSTRAINT fk_espera_tutor FOREIGN KEY (id_tutor) REFERENCES TB_TUTOR(id_tutor),
                                 CONSTRAINT fk_espera_pet FOREIGN KEY (id_pet) REFERENCES TB_PET(id_pet),
                                 CONSTRAINT fk_espera_servico FOREIGN KEY (id_servico_clinica) REFERENCES TB_SERVICO_CLINICA(id_servico_clinica)
);

CREATE INDEX ix_espera_servico_status ON TB_LISTA_ESPERA (id_servico_clinica, ds_status);
CREATE INDEX ix_espera_tutor_status ON TB_LISTA_ESPERA (id_tutor, ds_status);

-- Eventos existentes continuam valendo como confirmados.
ALTER TABLE TB_EVENTO_SAUDE ADD (
    ds_confirmacao VARCHAR2(20) DEFAULT 'CONFIRMADO' NOT NULL,
    dt_confirmacao TIMESTAMP
);