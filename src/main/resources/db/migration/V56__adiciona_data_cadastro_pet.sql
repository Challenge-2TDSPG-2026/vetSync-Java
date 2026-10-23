-- Pets existentes permanecem sem data conhecida; novos cadastros recebem a data pela entidade.
ALTER TABLE TB_PET ADD (dt_cadastro TIMESTAMP);
