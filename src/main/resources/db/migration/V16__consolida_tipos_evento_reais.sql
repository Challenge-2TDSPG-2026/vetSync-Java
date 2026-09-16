-- Consolida os tipos de evento com os nomes REAIS usados no app,
-- garantindo que qualquer evento de saúde vinculado a um desses tipos
-- receba os pontos correspondentes ao ser concluído
-- (EventoService.concluir -> PontosService.lancarPendente lê tipoEvento.nr_pontos).

-- 'Retorno veterinário' já existe (criado na V11, usado por LinkAgendamentoVetService
-- via nome fixo "Retorno veterinário" — não pode ser renomeado). Estava com 0 pontos.
UPDATE TB_TIPO_EVENTO
SET nr_pontos = 50
WHERE nm_tipo_evento = 'Retorno veterinário';

-- 'Realizar vacinação' (nome antigo, inventado) -> renomeado para 'Vacina'
UPDATE TB_TIPO_EVENTO
SET nm_tipo_evento = 'Vacina', nr_pontos = 100
WHERE nm_tipo_evento = 'Realizar vacinação';

-- 'Comparecer à consulta' (nome antigo, inventado) -> renomeado para 'Consulta de rotina'
UPDATE TB_TIPO_EVENTO
SET nm_tipo_evento = 'Consulta de rotina', nr_pontos = 25
WHERE nm_tipo_evento = 'Comparecer à consulta';

-- 'Banho e Tosa (Estética)' (nome antigo, inventado) -> renomeado para 'Banho e tosa'
-- Mantém a palavra "banho" no nome, exigida por EventoService.isServicoEstetica(...)
UPDATE TB_TIPO_EVENTO
SET nm_tipo_evento = 'Banho e tosa', nr_pontos = 30
WHERE nm_tipo_evento = 'Banho e Tosa (Estética)';

-- 'Cirurgia' ainda não existia no catálogo — valor sugerido, ajuste se necessário
INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Cirurgia', 'TERAPEUTICO', 200);

-- 'Atendimento de emergência' ainda não existia no catálogo — valor sugerido, ajuste se necessário
INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Atendimento de emergência', 'EMERGENCIA', 100);