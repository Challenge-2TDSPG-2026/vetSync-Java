-- Corrige a regra introduzida na V40: todas as clínicas existentes
-- e as cadastradas a partir desta versão possuem contrato ativo por padrão.
UPDATE TB_CLINICA
   SET st_contratante = 'A'
 WHERE st_contratante <> 'A';

ALTER TABLE TB_CLINICA MODIFY (st_contratante DEFAULT 'A');
