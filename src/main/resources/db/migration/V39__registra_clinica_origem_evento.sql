ALTER TABLE TB_EVENTO_SAUDE ADD (id_clinica NUMBER(10));

UPDATE TB_EVENTO_SAUDE e
SET id_clinica = (
    SELECT v.id_clinica FROM TB_VETERINARIO v WHERE v.id_veterinario = e.id_veterinario
)
WHERE e.id_veterinario IS NOT NULL;

UPDATE TB_EVENTO_SAUDE e
SET id_clinica = (
    SELECT p.id_clinica FROM TB_PROFISSIONAL_ESTETICA p WHERE p.id_profissional_estetica = e.id_profissional_estetica
)
WHERE e.id_clinica IS NULL AND e.id_profissional_estetica IS NOT NULL;

ALTER TABLE TB_EVENTO_SAUDE ADD CONSTRAINT fk_evento_clinica_origem
    FOREIGN KEY (id_clinica) REFERENCES TB_CLINICA(id_clinica);
CREATE INDEX idx_evento_clinica_origem ON TB_EVENTO_SAUDE (id_clinica);
