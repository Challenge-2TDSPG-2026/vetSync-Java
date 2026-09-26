-- Especialidade do veterinário (ex.: Clínica Geral, Cardiologia, Dermatologia...),
-- usada para filtrar os profissionais na hora de agendar um evento.
ALTER TABLE TB_VETERINARIO ADD (
    ds_especialidade VARCHAR2(50)
);