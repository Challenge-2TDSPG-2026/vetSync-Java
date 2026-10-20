-- Carga repetivel de dados demonstrativos (ficticios) para o VetSync.
-- Execute depois das migrations, conectado ao schema local de desenvolvimento.
-- Cria 4 clinicas, 8 veterinarios, 12 tutores, 24 pets e consultas/vacinas
-- em dias uteis entre hoje e 28/02/2027. Nenhum dado pessoal real e usado.
-- As chaves naturais com sufixo @demo.vetsync.local tornam a carga idempotente.

SET SERVEROUTPUT ON;

DECLARE
    v_especie NUMBER;
    v_raca NUMBER;
    v_tipo_consulta NUMBER;
    v_tipo_vacina NUMBER;
    v_clinica NUMBER;
    v_vet NUMBER;
    v_tutor NUMBER;
    v_pet NUMBER;
    v_data DATE;
    v_hora VARCHAR2(5);
    v_n NUMBER := 0;
    v_tipo NUMBER;
    v_obs VARCHAR2(500);
    v_custo NUMBER(10,2);
    v_count NUMBER;
BEGIN
    -- Especies e racas suficientes para os registros demonstrativos.
    FOR r IN (SELECT 'Cachorro' nome FROM dual UNION ALL SELECT 'Gato' FROM dual) LOOP
        MERGE INTO TB_ESPECIE e USING (SELECT r.nome nm FROM dual) s ON (LOWER(e.nm_especie)=LOWER(s.nm))
        WHEN NOT MATCHED THEN INSERT (nm_especie) VALUES (s.nm);
    END LOOP;
    SELECT id_especie INTO v_especie FROM TB_ESPECIE WHERE LOWER(nm_especie)='cachorro';
    FOR r IN (SELECT 'SRD' nome FROM dual UNION ALL SELECT 'Labrador' FROM dual UNION ALL SELECT 'Golden Retriever' FROM dual UNION ALL SELECT 'Shih-tzu' FROM dual UNION ALL SELECT 'Poodle' FROM dual) LOOP
        SELECT COUNT(*) INTO v_count FROM TB_RACA WHERE LOWER(nm_raca)=LOWER(r.nome) AND id_especie=v_especie;
        IF v_count=0 THEN INSERT INTO TB_RACA(nm_raca,id_especie) VALUES(r.nome,v_especie); END IF;
    END LOOP;
    SELECT id_especie INTO v_especie FROM TB_ESPECIE WHERE LOWER(nm_especie)='gato';
    FOR r IN (SELECT 'SRD' nome FROM dual UNION ALL SELECT 'Siames' FROM dual UNION ALL SELECT 'Persa' FROM dual UNION ALL SELECT 'Maine Coon' FROM dual) LOOP
        SELECT COUNT(*) INTO v_count FROM TB_RACA WHERE LOWER(nm_raca)=LOWER(r.nome) AND id_especie=v_especie;
        IF v_count=0 THEN INSERT INTO TB_RACA(nm_raca,id_especie) VALUES(r.nome,v_especie); END IF;
    END LOOP;

    -- Tipos globais existem nas migrations; se o schema foi iniciado parcialmente,
    -- criamos apenas os dois tipos usados nesta agenda.
    MERGE INTO TB_TIPO_EVENTO t USING (SELECT 'Consulta de rotina' nm,'PREVENTIVO' cat,25 pts FROM dual) s
      ON (LOWER(t.nm_tipo_evento)=LOWER(s.nm) AND t.id_clinica IS NULL)
      WHEN NOT MATCHED THEN INSERT(nm_tipo_evento,ds_categoria,nr_pontos,ds_modalidade_agendamento,nr_duracao_minutos)
      VALUES(s.nm,s.cat,s.pts,'CLINICO_GERAL',30);
    MERGE INTO TB_TIPO_EVENTO t USING (SELECT 'Vacina' nm,'PREVENTIVO' cat,100 pts FROM dual) s
      ON (LOWER(t.nm_tipo_evento)=LOWER(s.nm) AND t.id_clinica IS NULL)
      WHEN NOT MATCHED THEN INSERT(nm_tipo_evento,ds_categoria,nr_pontos,ds_modalidade_agendamento,nr_duracao_minutos)
      VALUES(s.nm,s.cat,s.pts,'CLINICO_GERAL',30);
    SELECT id_tipo_evento INTO v_tipo_consulta FROM TB_TIPO_EVENTO WHERE LOWER(nm_tipo_evento)='consulta de rotina' AND id_clinica IS NULL AND ROWNUM=1;
    SELECT id_tipo_evento INTO v_tipo_vacina FROM TB_TIPO_EVENTO WHERE LOWER(nm_tipo_evento)='vacina' AND id_clinica IS NULL AND ROWNUM=1;

    -- Quatro clinicas ficticias em cidades distintas.
    FOR r IN (SELECT 'Clinica Veterinaria Parque' nm,'00000000000101' cnpj,'Sao Paulo' cidade,'SP' uf,'Rua dos Pinheiros, 420' ender FROM dual UNION ALL
              SELECT 'Centro Veterinario Vila Nova','00000000000102','Campinas','SP','Av. Brasil, 1180' FROM dual UNION ALL
              SELECT 'Hospital Veterinario Horizonte','00000000000103','Santos','SP','Rua das Acacias, 75' FROM dual UNION ALL
              SELECT 'Clinica Bicho Feliz','00000000000104','Santo Andre','SP','Av. Portugal, 930' FROM dual) LOOP
        MERGE INTO TB_CLINICA c USING (SELECT r.nm nm,r.cnpj cnpj,r.cidade cidade,r.uf uf,r.ender ender FROM dual) s ON (c.ds_cnpj=s.cnpj)
        WHEN NOT MATCHED THEN INSERT(nm_clinica,ds_cnpj,ds_cidade,ds_uf,ds_endereco,ds_telefone,st_contratante)
          VALUES(s.nm,s.cnpj,s.cidade,s.uf,s.ender,'11990000000','A');
    END LOOP;

    -- Tutores ficticios; CPFs de teste reservados para demonstracao (nao validos como identidade real).
    FOR i IN 1..12 LOOP
        MERGE INTO TB_TUTOR t USING (SELECT i idx FROM dual) s ON (t.ds_email='tutor'||TO_CHAR(s.idx,'FM00')||'@demo.vetsync.local')
        WHEN NOT MATCHED THEN INSERT(nm_tutor,ds_email,nr_telefone,ds_cpf,ds_senha,dt_cadastro)
          VALUES(CASE MOD(s.idx,6) WHEN 0 THEN 'Mariana Costa' WHEN 1 THEN 'Rafael Oliveira' WHEN 2 THEN 'Camila Santos' WHEN 3 THEN 'Bruno Almeida' WHEN 4 THEN 'Fernanda Lima' ELSE 'Lucas Ferreira' END,
            'tutor'||TO_CHAR(s.idx,'FM00')||'@demo.vetsync.local','1199000'||TO_CHAR(s.idx,'FM0000'),'9999999'||TO_CHAR(s.idx,'FM0000'),'$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJLaR3VQw0',TRUNC(SYSDATE));
    END LOOP;

    -- Veterinarios, distribuidos entre as quatro clinicas.
    FOR i IN 1..8 LOOP
        v_clinica := NULL;
        SELECT id_clinica INTO v_clinica FROM TB_CLINICA WHERE ds_cnpj='000000000001'||TO_CHAR(CEIL(i/2),'FM00');
        MERGE INTO TB_VETERINARIO v USING (SELECT i idx,v_clinica cid FROM dual) s ON (v.ds_email='vet'||TO_CHAR(s.idx,'FM00')||'@demo.vetsync.local')
        WHEN NOT MATCHED THEN INSERT(nm_veterinario,nr_crmv,ds_email,ds_senha,id_clinica,ds_especialidade)
          VALUES(CASE MOD(s.idx,5) WHEN 0 THEN 'Ana Paula Ribeiro' WHEN 1 THEN 'Diego Martins' WHEN 2 THEN 'Juliana Carvalho' WHEN 3 THEN 'Pedro Henrique Souza' ELSE 'Beatriz Mendes' END,
            'CRMV-SP-DEMO-'||TO_CHAR(s.idx,'FM000'),'vet'||TO_CHAR(s.idx,'FM00')||'@demo.vetsync.local','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJLaR3VQw0',s.cid,
            CASE MOD(s.idx,4) WHEN 0 THEN 'Clinica geral' WHEN 1 THEN 'Dermatologia' WHEN 2 THEN 'Medicina felina' ELSE 'Ortopedia' END);
    END LOOP;

    -- Pets: 2 por tutor, usando racas existentes por especie.
    FOR i IN 1..24 LOOP
        v_tutor := NULL;
        SELECT id_tutor INTO v_tutor FROM TB_TUTOR WHERE ds_email='tutor'||TO_CHAR(CEIL(i/2),'FM00')||'@demo.vetsync.local';
        v_especie := NULL;
        SELECT id_especie INTO v_especie FROM TB_ESPECIE WHERE LOWER(nm_especie)=CASE WHEN MOD(i,3)=0 THEN 'gato' ELSE 'cachorro' END;
        SELECT id_raca INTO v_raca FROM (SELECT id_raca,ROW_NUMBER() OVER(ORDER BY id_raca) rn FROM TB_RACA WHERE id_especie=v_especie) WHERE rn=1+MOD(i,4);
        SELECT COUNT(*) INTO v_count FROM TB_PET WHERE nm_pet='Pet Demo '||TO_CHAR(i,'FM00') AND id_tutor=v_tutor;
        IF v_count=0 THEN
          INSERT INTO TB_PET(nm_pet,dt_nascimento,ds_sexo,id_tutor,id_raca)
          VALUES('Pet Demo '||TO_CHAR(i,'FM00'),ADD_MONTHS(TRUNC(SYSDATE),-12-MOD(i*7,96)),CASE WHEN MOD(i,2)=0 THEN 'F' ELSE 'M' END,v_tutor,v_raca);
        END IF;
    END LOOP;

    -- Agenda simulada: aproximadamente 3 atendimentos por dia util, horarios
    -- escalonados em quatro clinicas; inclui consultas e vacinacoes preventivas.
    v_data := TRUNC(SYSDATE)+1;
    WHILE v_data <= DATE '2027-02-28' LOOP
      IF v_data - TRUNC(v_data,'IW') < 5 THEN
        FOR i IN 1..3 LOOP
          -- Sequencia derivada da data para manter os mesmos registros ao reexecutar.
          v_n := (v_data - DATE '2026-01-01')*3 + i;
          v_tutor := NULL;
          SELECT id_tutor INTO v_tutor FROM TB_TUTOR WHERE ds_email='tutor'||TO_CHAR(MOD(v_n-1,12)+1,'FM00')||'@demo.vetsync.local';
          SELECT id_pet INTO v_pet FROM (SELECT id_pet,ROW_NUMBER() OVER(ORDER BY id_pet) rn FROM TB_PET WHERE id_tutor=v_tutor) WHERE rn=1+MOD(v_n,2);
          v_clinica := NULL;
          SELECT id_clinica INTO v_clinica FROM TB_CLINICA WHERE ds_cnpj='000000000001'||TO_CHAR(MOD(v_n-1,4)+1,'FM00');
          SELECT id_veterinario INTO v_vet FROM (SELECT id_veterinario,ROW_NUMBER() OVER(ORDER BY id_veterinario) rn FROM TB_VETERINARIO WHERE id_clinica=v_clinica) WHERE rn=1+MOD(TRUNC(v_n/3),2);
          v_tipo := CASE WHEN MOD(v_n,4)=0 THEN v_tipo_vacina ELSE v_tipo_consulta END;
          v_hora := CASE MOD(i,3) WHEN 1 THEN '09:00' WHEN 2 THEN '13:30' ELSE '16:00' END;
          v_obs := CASE WHEN v_tipo=v_tipo_vacina THEN 'Revisao preventiva e atualizacao do protocolo vacinal.' ELSE 'Consulta de rotina; avaliacao clinica e orientacoes ao tutor.' END;
          v_custo := CASE WHEN v_tipo=v_tipo_vacina THEN 145+MOD(v_n,5)*10 ELSE 180+MOD(v_n,7)*15 END;
          SELECT COUNT(*) INTO v_count FROM TB_EVENTO_SAUDE WHERE id_clinica=v_clinica AND dt_evento=v_data AND hr_evento=v_hora AND ds_observacao LIKE 'DEMO-VETSYNC:%';
          IF v_count=0 THEN
            INSERT INTO TB_EVENTO_SAUDE(id_pet,id_tipo_evento,id_veterinario,id_clinica,dt_evento,hr_evento,ds_observacao,ds_observacao_tutor,vl_custo,ds_status,ds_confirmacao,dt_criacao)
            VALUES(v_pet,v_tipo,v_vet,v_clinica,v_data,v_hora,'DEMO-VETSYNC: '||v_obs,'Agendamento demonstrativo. Dados ficticios para simulacao.',v_custo,'AGENDADO',CASE WHEN MOD(v_n,5)=0 THEN 'PENDENTE' ELSE 'CONFIRMADO' END,SYSTIMESTAMP);
          END IF;
        END LOOP;
      END IF;
      v_data := v_data+1;
    END LOOP;
    DBMS_OUTPUT.PUT_LINE('Carga demonstrativa concluida. Intervalo da agenda: '||TO_CHAR(TRUNC(SYSDATE)+1,'DD/MM/YYYY')||' a 28/02/2027.');
END;
/

COMMIT;
