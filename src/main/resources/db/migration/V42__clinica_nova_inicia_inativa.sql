-- Clínicas cadastradas a partir desta versão nascem com contrato INATIVO.
-- O Admin confirma e ativa o contrato manualmente (PATCH /vinculos-clinica/clinicas/{id}/contrato).
-- As clínicas já existentes mantêm o contrato atual (ativado na V41).
ALTER TABLE TB_CLINICA MODIFY (st_contratante DEFAULT 'I');