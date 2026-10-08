-- Limpeza única dos dados operacionais para reiniciar o ambiente de demonstração.
--
-- Preserva somente contas de ADMIN e catálogos de referência (espécies, raças,
-- tipos de evento, serviços de estética e tipos de vacina). Não há CASCADE nas
-- FKs do domínio; por isso as exclusões seguem a ordem dos dependentes para as
-- entidades principais.
--
-- Esta migration é destrutiva e não deve ser usada para reinicializações futuras.
-- Para um novo reset, crie uma nova migration deliberada ou restaure um backup.

-- 1. Cancela apenas atendimentos ainda agendados antes de removê-los.
UPDATE TB_EVENTO_SAUDE
   SET ds_status = 'CANCELADO'
 WHERE ds_status = 'AGENDADO';

-- 2. Remove registros que dependem de eventos, planos, pets, tutores ou equipe.
DELETE FROM TB_LINK_AGENDAMENTO_VET;
DELETE FROM TB_RELATORIO_ESTETICA;
DELETE FROM TB_EVENTO_SERVICO;
DELETE FROM TB_PRESCRICAO;
DELETE FROM TB_LANCAMENTO_PONTOS;
DELETE FROM TB_RESGATE;
DELETE FROM TB_PLANO_ITEM;
DELETE FROM TB_PLANO_TRATAMENTO;
DELETE FROM TB_VACINA_PET;
DELETE FROM TB_PERFIL_SAUDE;
DELETE FROM TB_HISTORICO_PESO;
DELETE FROM TB_EVENTO_HISTORICO;
DELETE FROM TB_EVENTO_ANEXO;
DELETE FROM TB_CARTEIRA_COMPARTILHADA;
DELETE FROM TB_PET_CONVITE;
DELETE FROM TB_PET_ACESSO;
DELETE FROM TB_TUTOR_RESPONSAVEL;
DELETE FROM TB_DISPOSITIVO_PUSH;
DELETE FROM TB_NOTIFICACAO;
DELETE FROM TB_PREFERENCIA_NOTIFICACAO;

-- 3. Fecha os vínculos de clínica removendo sessões e códigos antes das clínicas.
DELETE FROM TB_VINCULO_TUTOR_CLINICA;
DELETE FROM TB_SESSAO_VINCULO_CLINICA;
DELETE FROM TB_CODIGO_VINCULO_CLINICA;

-- 4. Remove agenda e especialidades associadas à equipe das clínicas.
DELETE FROM TB_DISPONIBILIDADE;
DELETE FROM TB_BLOQUEIO_AGENDA;
DELETE FROM TB_DISPONIBILIDADE_ESTETICA;
DELETE FROM TB_BLOQUEIO_AGENDA_ESTETICA;
DELETE FROM TB_PROFISSIONAL_SERVICO;

-- 5. Remove o histórico clínico, depois pets, usuários tutores e equipe.
DELETE FROM TB_EVENTO_SAUDE;
DELETE FROM TB_PET;
DELETE FROM TB_TUTOR;
DELETE FROM TB_PROFISSIONAL_ESTETICA;
DELETE FROM TB_VETERINARIO;

-- 6. Catálogos vinculados à clínica deixam de ter escopo após a limpeza.
-- Recompensas são removidas porque resgates preservam cópias delas e já foram limpos.
DELETE FROM TB_RECOMPENSA;
UPDATE TB_MEDICAMENTO SET id_clinica = NULL;
UPDATE TB_TIPO_VACINA SET id_clinica = NULL;

-- 7. Com todas as referências removidas, elimina as clínicas e os dados auxiliares.
DELETE FROM TB_CLINICA;
DELETE FROM TB_ACAO_IA;
DELETE FROM TB_AUDITORIA;
DELETE FROM TB_LOG_ERROS;
