# Melhorias de Backend para o VetSync

Documento de referência para a próxima evolução da API Spring Boot do VetSync. As propostas abaixo foram levantadas a partir das necessidades da experiência do tutor e do veterinário no aplicativo mobile.

## Objetivos

- Aumentar a frequência de uso preventivo do aplicativo.
- Evitar que o tutor perca vacinas, consultas e retornos.
- Dar ao veterinário uma visão operacional da agenda e dos pacientes.
- Manter histórico clínico confiável e auditável.
- Permitir que o aplicativo ofereça notificações e sincronização mais completas.
- Preservar compatibilidade com o aplicativo atual durante a migração.

## Prioridades

| Prioridade | Entrega | Impacto |
| --- | --- | --- |
| P0 | Notificações e lembretes de eventos | Alto |
| P0 | Próximas ações e pendências por pet | Alto |
| P0 | Detalhes clínicos completos dos eventos | Alto |
| P1 | Carteira de vacinação com vencimentos | Alto |
| P1 | Busca e filtros para pacientes | Médio/alto |
| P1 | Preferências do usuário e notificações | Médio |
| P1 | Auditoria e histórico de alterações | Alto |
| P2 | Relatórios e métricas da clínica | Médio |
| P2 | Integração avançada com SIA | Médio/alto |

---

## 1. Notificações e lembretes de eventos — P0

Atualmente o app registra o Expo Push Token e também consegue criar lembretes locais. O backend precisa assumir os eventos que dependem de sincronização e notificações remotas.

### Endpoints sugeridos

#### Registrar dispositivo

`POST /notificacoes/dispositivos`

```json
{
  "token": "ExponentPushToken[...]",
  "plataforma": "ANDROID",
  "nomeDispositivo": "Opcional",
  "fusoHorario": "America/Sao_Paulo"
}
```

O registro deve ser idempotente por usuário, token e plataforma.

#### Remover dispositivo

`DELETE /notificacoes/dispositivos/{token}`

Deve ser executado no logout ou quando o token deixar de ser válido.

#### Listar notificações do usuário

`GET /notificacoes?lida=false&page=0&size=20`

Resposta sugerida:

```json
{
  "content": [
    {
      "id": "uuid",
      "tipo": "EVENTO_PROXIMO",
      "titulo": "Vacina de Luna amanhã",
      "mensagem": "A vacinação está agendada para 14:00.",
      "referenciaTipo": "EVENTO",
      "referenciaId": "uuid-evento",
      "lida": false,
      "criadaEm": "2026-09-28T12:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

#### Marcar como lida

- `PATCH /notificacoes/{id}/lida`
- `PATCH /notificacoes/lidas` para marcar todas como lidas

#### Preferências de notificação

- `GET /usuarios/preferencias/notificacoes`
- `PUT /usuarios/preferencias/notificacoes`

```json
{
  "pushAtivo": true,
  "lembreteSeteDias": true,
  "lembreteUmDia": true,
  "lembreteDuasHoras": false,
  "vacinasVencendo": true,
  "retornosPendentes": true,
  "convitesDeAcesso": true,
  "resgates": true
}
```

### Regras de negócio

- Não enviar a mesma notificação duas vezes para o mesmo evento e janela.
- Respeitar o fuso horário salvo pelo usuário.
- Não enviar notificações para eventos cancelados.
- Recalcular lembretes quando a data do evento mudar.
- Remover ou invalidar tokens que retornarem erro permanente do Expo.
- Registrar o status do envio para diagnóstico.

### Modelo sugerido

`notificacao`

- `id`
- `usuario_id`
- `tipo`
- `titulo`
- `mensagem`
- `referencia_tipo`
- `referencia_id`
- `lida`
- `enviada_em`
- `criada_em`

`dispositivo_push`

- `id`
- `usuario_id`
- `token`
- `plataforma`
- `fuso_horario`
- `ativo`
- `ultimo_uso_em`
- `criado_em`

---

## 2. Próximas ações e pendências por pet — P0

O aplicativo consegue calcular alguns atrasos localmente, mas o backend deve centralizar regras preventivas e gerar pendências confiáveis.

### Endpoint sugerido

`GET /pets/{petId}/proximas-acoes`

Resposta:

```json
[
  {
    "id": "uuid",
    "tipo": "VACINA_ATRASADA",
    "prioridade": "ALTA",
    "titulo": "Vacina antirrábica atrasada",
    "descricao": "O reforço deveria ter sido realizado em 2026-09-20.",
    "eventoReferenciaId": "uuid-evento",
    "dataLimite": "2026-09-20",
    "podeAgendar": true
  }
]
```

### Tipos iniciais

- `EVENTO_ATRASADO`
- `VACINA_VENCENDO`
- `VACINA_ATRASADA`
- `RETORNO_NAO_AGENDADO`
- `CHECKUP_RECOMENDADO`
- `PESO_DESATUALIZADO`
- `DOCUMENTO_PENDENTE`

### Regras

- A prioridade deve ser calculada pela gravidade e pelo tempo de atraso.
- Uma pendência resolvida não deve voltar a aparecer.
- O endpoint deve considerar somente dados do pet e do usuário autorizado.
- A API deve retornar o evento relacionado para o app abrir a tela correta.

---

## 3. Detalhes clínicos completos dos eventos — P0

O evento atual já possui observação, custo, status e veterinário. Para um histórico clínico mais confiável, é necessário separar informações do tutor, da clínica e do atendimento.

### Endpoint sugerido

`GET /eventos/{id}/detalhes`

Resposta sugerida:

```json
{
  "id": "uuid",
  "pet": {
    "id": "uuid",
    "nome": "Luna"
  },
  "tipo": {
    "id": "uuid",
    "nome": "Consulta",
    "categoria": "TERAPEUTICO"
  },
  "status": "CONCLUIDO",
  "dataAgendada": "2026-09-28T14:00:00",
  "criadoEm": "2026-09-20T10:00:00",
  "tutorObservacao": "Está com pouco apetite.",
  "observacaoClinica": "Avaliação sem alterações relevantes.",
  "diagnostico": "Opcional",
  "conduta": "Opcional",
  "custo": 150.0,
  "veterinario": {
    "id": "uuid",
    "nome": "Dra. Ana",
    "crmv": "00000"
  },
  "cancelamento": null
}
```

### Endpoints complementares

- `GET /eventos/{id}/historico`
- `POST /eventos/{id}/anexos`
- `GET /eventos/{id}/anexos`
- `DELETE /eventos/{id}/anexos/{anexoId}`
- `PATCH /eventos/{id}/reagendar`

### Segurança

- Tutor pode visualizar eventos dos pets aos quais possui acesso.
- Veterinário pode visualizar eventos relacionados à sua clínica/agenda.
- Observações clínicas podem exigir permissão diferenciada.
- Anexos devem ser validados por tamanho e tipo MIME.

---

## 4. Carteira de vacinação e vencimentos — P1

O app hoje identifica vacinas pelo nome do evento. Essa regra não deve ficar somente no frontend.

### Endpoint sugerido

`GET /pets/{petId}/carteira-vacinacao`

Resposta:

```json
{
  "petId": "uuid",
  "vacinas": [
    {
      "id": "uuid",
      "nome": "Antirrábica",
      "aplicadaEm": "2026-01-10",
      "proximaDoseEm": "2027-01-10",
      "status": "EM_DIA",
      "eventoId": "uuid-evento",
      "veterinario": "Dra. Ana",
      "comprovanteUrl": null
    }
  ],
  "resumo": {
    "emDia": 3,
    "vencendo": 1,
    "atrasadas": 0
  }
}
```

### Regras

- Cada tipo de vacina deve possuir periodicidade configurável.
- O status deve ser calculado pelo backend com base na data atual.
- Doses futuras e doses aplicadas devem ser diferenciadas.
- O histórico não deve depender de buscas textuais pelo nome do evento.
- Permitir anexar comprovante quando a clínica registrar a aplicação.

### Modelo sugerido

`vacina_pet`

- `id`
- `pet_id`
- `tipo_vacina_id`
- `evento_id`
- `data_aplicacao`
- `proxima_dose_em`
- `comprovante_url`
- `criado_em`

---

## 5. Busca e filtros para pacientes — P1

Hoje a área do veterinário pode agregar pacientes a partir dos eventos. Com o crescimento da base, essa estratégia deve ser suportada por consulta paginada no backend.

### Endpoint sugerido

`GET /veterinarios/me/pacientes?busca=luna&especie=CANINO&pendencia=true&page=0&size=20&ordenar=PROXIMO_EVENTO`

Resposta:

```json
{
  "content": [
    {
      "pet": {},
      "ultimoAtendimentoEm": "2026-09-10T14:00:00",
      "proximoAtendimentoEm": "2026-10-02T10:00:00",
      "eventosPendentes": 1,
      "alertas": []
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

### Filtros mínimos

- Nome do pet.
- Nome do tutor.
- Espécie.
- Raça.
- Evento pendente.
- Último atendimento.
- Próximo retorno.

---

## 6. Perfil de saúde do pet — P1

Adicionar dados úteis para o atendimento sem depender de campos livres.

### Endpoints

- `GET /pets/{id}/perfil-saude`
- `PUT /pets/{id}/perfil-saude`

### Campos sugeridos

- `pesoAtual`
- `pesoAtualizadoEm`
- `alergias`
- `medicamentosContinuos`
- `restricoesAlimentares`
- `condicoesPreExistentes`
- `observacoesImportantes`
- `contatoEmergencia`
- `veterinarioPreferencialId`

### Histórico de peso

`GET /pets/{id}/peso/historico`

O endpoint deve retornar registros suficientes para montar um gráfico de evolução no aplicativo.

---

## 7. Acesso compartilhado e permissões — P1

A funcionalidade atual já possui convites e revogação. A próxima etapa é tornar as permissões mais granulares e auditáveis.

### Endpoints

- `PATCH /pets/{petId}/acessos/{acessoId}`
- `GET /pets/{petId}/convites`
- `DELETE /pets/{petId}/convites/{conviteId}`
- `POST /convites/{token}/aceitar`

### Permissões sugeridas

- `VISUALIZAR_PERFIL`
- `VISUALIZAR_AGENDA`
- `VISUALIZAR_CARTEIRA`
- `CRIAR_EVENTO`
- `EDITAR_PERFIL`

### Regras

- Convite deve expirar.
- Aceite deve ser de uso único.
- Revogação deve invalidar imediatamente o acesso.
- Toda alteração deve ficar registrada.
- O proprietário deve visualizar quem acessou e quando.

---

## 8. Auditoria e histórico de alterações — P1

Para preservar confiança no prontuário, registrar alterações importantes.

### Endpoint sugerido

`GET /auditoria?entidade=EVENTO&entidadeId={id}`

### Eventos auditáveis

- Criação, alteração e cancelamento de evento.
- Conclusão de consulta.
- Alteração de observação clínica.
- Alteração de custo.
- Inclusão ou remoção de vacina.
- Convite, aceite e revogação de acesso.
- Validação de resgate.

### Campos

- Usuário responsável.
- Perfil.
- Ação.
- Data/hora.
- Valor anterior.
- Valor novo.
- IP ou identificador técnico, conforme política de privacidade.

---

## 9. Relatórios da clínica — P2

Criar endpoints agregados para o painel do veterinário, evitando cálculos incorretos no aplicativo.

### Endpoint sugerido

`GET /veterinarios/me/relatorios/resumo?inicio=2026-09-01&fim=2026-09-30`

### Indicadores

- Consultas agendadas.
- Consultas concluídas.
- Cancelamentos.
- Faltas, se o domínio suportar.
- Faturamento.
- Pacientes atendidos.
- Retornos pendentes.
- Vacinas aplicadas.
- Tempo médio até conclusão.

O backend deve validar o intervalo de datas e aplicar autorização por clínica.

---

## 10. Integração segura com a SIA — P2

Caso a SIA passe a executar ações, o backend deve oferecer comandos controlados em vez de permitir que o modelo acesse diretamente a base.

### Endpoint sugerido

`POST /ia/acoes/preview`

Recebe a intenção e retorna uma prévia sem executar:

```json
{
  "acao": "CRIAR_EVENTO",
  "resumo": "Agendar consulta para Luna em 02/10 às 10:00",
  "dados": {
    "petId": "uuid",
    "tipoEventoId": "uuid",
    "data": "2026-10-02T10:00:00"
  },
  "requerConfirmacao": true
}
```

Execução confirmada:

`POST /ia/acoes/{id}/confirmar`

### Regras

- A SIA nunca deve executar ação sensível sem confirmação explícita.
- Validar autorização novamente no endpoint de confirmação.
- Registrar a ação em auditoria.
- Expirar prévias não confirmadas.
- Limitar ações disponíveis por perfil.

---

## 11. Contratos e compatibilidade

- Manter os endpoints atuais durante a adoção dos novos contratos.
- Preferir adicionar campos opcionais antes de remover campos existentes.
- Versionar alterações incompatíveis, por exemplo `/api/v2`.
- Padronizar datas em ISO 8601 com timezone.
- Padronizar erros:

```json
{
  "timestamp": "2026-09-28T18:00:00Z",
  "status": 422,
  "codigo": "VALIDACAO",
  "mensagem": "Existem campos inválidos.",
  "campos": {
    "data": "A data deve ser futura."
  },
  "correlationId": "uuid"
}
```

- Incluir paginação em listas que podem crescer.
- Retornar `ETag` ou `updatedAt` em recursos que precisam de sincronização.
- Documentar todos os contratos no OpenAPI/Swagger.

## 12. Segurança e operação

- Aplicar autorização por tutor, pet, veterinário e clínica em todos os endpoints.
- Não confiar somente no `petId` enviado pelo cliente.
- Validar ownership antes de retornar prontuários e anexos.
- Usar expiração e rotação de tokens.
- Rate-limit em login, recuperação de senha, convites e endpoints de IA.
- Sanitizar observações exibidas em notificações.
- Não expor tokens de push ou dados clínicos em logs.
- Criar métricas para falhas de notificação e erros de sincronização.
- Criar jobs idempotentes para gerar lembretes.

## 13. Ordem recomendada para implementação

1. Notificações, dispositivos e preferências.
2. Próximas ações e pendências.
3. Detalhes completos dos eventos.
4. Carteira de vacinação com vencimentos.
5. Perfil de saúde do pet.
6. Busca paginada de pacientes.
7. Permissões e auditoria de acessos.
8. Auditoria clínica.
9. Relatórios da clínica.
10. Ações confirmáveis da SIA.

## Critérios de aceite gerais

- Todas as rotas possuem testes unitários e de integração.
- Casos de autorização negativa são testados.
- Eventos cancelados não geram lembretes.
- Requisições repetidas não duplicam notificações, resgates ou convites.
- Datas são testadas em mais de um fuso horário.
- Migrações Flyway possuem rollback operacional documentado.
- O aplicativo atual continua funcionando sem exigir atualização imediata.
