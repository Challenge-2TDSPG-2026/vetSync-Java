-- Vermifugação não fazia parte da lista original de ações do tutor.
-- Adicionada aqui como tipo preventivo, mesmo peso de 'Realizar vacinação'.
INSERT INTO TB_TIPO_EVENTO (nm_tipo_evento, ds_categoria, nr_pontos)
VALUES ('Realizar vermifugação', 'PREVENTIVO', 100);