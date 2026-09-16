-- =========================================================
-- Regra de pontos: o que o tutor GANHA (TB_TIPO_EVENTO.nr_pontos)
-- =========================================================
-- Observações:
--  * 'Comparecer à consulta' definido como 25 pontos (ajustado, era 50 na tabela original).
--  * 'Banho e Tosa' representa os serviços de estética e vale 30 pontos.
--    O nome precisa conter a palavra "banho" para ser reconhecido como
--    serviço de estética por EventoService.isServicoEstetica(...).
--  * 'Completar todas as etapas do plano' (200 pontos bonus) não entra aqui:
--    esse valor é definido pelo veterinário por plano, no campo
--    nr_pontos_bonus de TB_PLANO_TRATAMENTO (ver PlanoTratamentoService.criar).

INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Comparecer à consulta', 'PREVENTIVO', 25);

INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Realizar vacinação', 'PREVENTIVO', 100);

INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Realizar exames', 'TERAPEUTICO', 100);

INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Seguir o plano médico', 'TERAPEUTICO', 100);

INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Retorno dentro do prazo', 'TERAPEUTICO', 50);

INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Manter acompanhamento preventivo', 'PREVENTIVO', 100);

INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Realizar procedimento preventivo', 'PREVENTIVO', 150);

INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Banho e Tosa (Estética)', 'BEM_ESTAR', 30);

-- =========================================================
-- Regra de pontos: o que o tutor GASTA (TB_RECOMPENSA.nr_custo_pontos)
-- =========================================================

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Bifinho', 'Petisco tradicional para cães e gatos', 50, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Snack cremoso', 'Petisco cremoso para recompensar o pet', 100, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Petiscos naturais', 'Petiscos naturais e saudáveis', 120, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Bolinha', 'Bolinha de brinquedo', 120, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Bola cravo', 'Bola com cravos de borracha para mastigar', 120, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Mordedor', 'Mordedor resistente para cães', 200, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Mordedor de corda', 'Mordedor de corda trançada', 150, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Pelúcia', 'Brinquedo de pelúcia', 250, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Rasqueadeira', 'Escova/rasqueadeira para cuidado do pelo', 200, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Coleira simples', 'Coleira simples ajustável', 150, 'PRODUTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Cupom 15% OFF', 'Cupom de 15% de desconto em serviços/produtos da clínica', 300, 'CUPOM_DESCONTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Cupom 25% OFF', 'Cupom de 25% de desconto em serviços/produtos da clínica', 500, 'CUPOM_DESCONTO', 1);

INSERT INTO TB_RECOMPENSA (nm_recompensa, ds_descricao, nr_custo_pontos, ds_tipo, fl_ativo)
VALUES ('Cupom 50% OFF', 'Cupom de 50% de desconto em serviços/produtos da clínica', 1000, 'CUPOM_DESCONTO', 1);