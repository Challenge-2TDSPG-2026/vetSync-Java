# VetSync

> **API REST + console web para continuidade do cuidado e engajamento na jornada de saúde do pet**
> FIAP Challenge 2026 — Parceria com **Clyvo Vet** | Java Advanced | 2º Ano ADS

---

## Integrantes

| Nome | RM |
|---|---|
| Arthur Brito da Silva| RM 562085 |
| Luiz Felipe Flosi dos Santos| RM 563197 |
| Pedro Henrique Brum Lopes | RM 561780 |

---

## Link do vídeo de apresentação

https://youtu.be/uW6jSxBvBPk

---

## Descrição do projeto

O **VetSync** é uma API backend em **Spring Boot** que resolve um problema real identificado em parceria com a **Clyvo Vet**: tutores de pets esquecem ou negligenciam eventos preventivos de saúde (vacinas, vermifugações, consultas, banhos), o que gera visitas de emergência evitáveis e agrava condições tratáveis.

A aplicação cobre três perfis de usuário — **Tutor**, **Veterinário** e **Admin** — e oferece:

- **Cadastro e gestão de pets e tutores**, com espécie, raça e idade calculada automaticamente.
- **Agendamento de eventos de saúde** (vacina, consulta, banho, cirurgia etc.) com validação de conflito de horário e de bloqueios de agenda do veterinário.
- **Conclusão e cancelamento de eventos**, com reagendamento automático opcional e histórico de motivo de cancelamento.
- **Planos de tratamento**: o veterinário define uma sequência de eventos futuros para o pet; o tutor agenda um item de cada vez e recebe um bônus de pontos ao concluir todos em ordem.
- **Prescrição de medicamentos**: o veterinário solicita, o admin libera ou nega, e o tutor é avisado por e-mail quando aprovado.
- **Programa de pontos e recompensas**: eventos concluídos e bônus de plano geram pontos (que ficam pendentes até o admin liberar); o tutor troca pontos por recompensas no catálogo, validadas por um veterinário.
- **Agenda do veterinário**: horários fixos de disponibilidade por dia da semana e bloqueios pontuais (férias, compromissos).
- **Autenticação JWT** stateless com autorização granular por perfil e por dono do recurso.
- **Console web integrado** (API tester), servido pelo próprio Spring Boot, para testar todos os fluxos sem precisar do Postman.

---

## Benefícios para o negócio

| Benefício | Impacto |
|---|---|
| Redução de emergências veterinárias | Tutores recebem alertas de eventos pendentes/atrasados, antecipando cuidados |
| Fidelização do cliente | Programa de pontos e recompensas cria vínculo contínuo entre tutor, pet e clínica |
| Adesão a tratamentos longos | Planos de tratamento guiam o tutor por uma sequência de cuidados, com bônus ao concluir |
| Histórico clínico centralizado | Eventos, prescrições e custos ficam registrados e consultáveis |
| Escalabilidade da solução | Arquitetura REST + Oracle + Flyway suporta crescimento e evolução do schema |
| Diferencial competitivo | Clyvo Vet entra no mercado digital com solução de acompanhamento contínuo |

---

## Arquitetura

```
+-----------------------------------------------------------------------+
|                          CLIENTE / FRONTEND                           |
|        Console web embutido (/index.html) - Postman - Swagger UI      |
+-------------------------------------+-----------------------------------+
                                      | HTTP/REST (JSON)
                                      v
+-----------------------------------------------------------------------+
|                        SPRING BOOT APPLICATION                        |
|  +-------------------------------------------------------------------+|
|  |            Security Layer - JwtFilter + Spring Security           ||
|  |    AppUserDetailsService (busca em Tutor/Veterinario/Admin)        ||
|  |    @PreAuthorize por role + Security beans de posse do recurso     ||
|  +---------------------------------+-----------------------------------+|
|                                    |                                    |
|  +----------+  +-----------+  +---v-------+  +-----------+  +----------+|
|  |AuthCtrl  |  |PetCtrl    |  |EventoCtrl |  |PlanoCtrl  |  |Prescricao||
|  |/auth/**  |  |/pets/**   |  |/eventos/**|  |/planos/** |  |Ctrl      ||
|  +----+-----+  +-----+-----+  +-----+-----+  +-----+-----+  +----+-----+|
|       |              |              |              |              |     |
|  +----v--------------v--------------v--------------v--------------v---+|
|  |                 Service layer - regras de negocio                  ||
|  | EventoService - PlanoTratamentoService - PontosService -           ||
|  | PrescricaoService - RecompensaService - AgendaService - ...        ||
|  +---------------------------------+-----------------------------------+|
|  +---------------------------------v-----------------------------------+|
|  |              Spring Data JPA / Repositories                         ||
|  +---------------------------------+-----------------------------------+|
+--------------------------------------+---------------------------------+
                                       | JDBC (ojdbc11) - versionado por Flyway
                                       v
+-----------------------------------------------------------------------+
|                    ORACLE XE 21c (Docker) ou Oracle FIAP               |
|                Schema: APP_USER - DB: XEPDB1 - Port: 1521              |
+-----------------------------------------------------------------------+
```

### Camadas da aplicação

| Camada | Responsabilidade |
|---|---|
| **Controller** | Recebe requisições HTTP, valida entrada com Bean Validation, delega ao Service, retorna DTOs (Java `record`) |
| **Service** | Regras de negócio: validação de conflito de horário, progressão de plano de tratamento, cálculo de saldo de pontos, aprovação de prescrições, envio de e-mail |
| **Repository** | Acesso a dados via Spring Data JPA |
| **Security** | `JwtFilter` stateless, `BCryptPasswordEncoder`, `@PreAuthorize` por perfil (`hasRole`) combinado com beans de posse do recurso (`isOwner`, `isSelf`, `isRelacionado`) |
| **Entity** | Mapeamento JPA das tabelas `TB_*`, com enums de status e validações |

### Perfis de usuário

| Perfil | Como é criado | O que pode fazer |
|---|---|---|
| **TUTOR** | Autocadastro em `POST /auth/registrar` | Gerencia os próprios pets, agenda/cancela eventos, acompanha planos de tratamento, pontos e recompensas |
| **VETERINARIO** | Cadastrado por um ADMIN em `POST /veterinarios` (recebe CRM e senha temporária por e-mail) | Conclui eventos, prescreve planos de tratamento e medicamentos, gerencia a própria agenda, valida resgates de recompensa |
| **ADMIN** | O primeiro é criado via `POST /admins/bootstrap` (chave secreta); os demais são criados por um ADMIN autenticado | Cadastra veterinários e outros admins, libera/nega prescrições e lançamentos de pontos pendentes |

### Modelo de domínio (simplificado)

```
Tutor (1)--<Pet (N)--<EventoSaude (N)--<Prescricao (0..1)
                |               |
                |               +--<LancamentoPontos (0..1)
                |
                +--<PlanoTratamento (N)--<PlanoItem (N)--1:1 EventoSaude

Tutor--<Resgate (N)>--1 Recompensa
Veterinario (1)--<Disponibilidade (N)
Veterinario (1)--<BloqueioAgenda (N)
Veterinario (1)--1 Clinica
```

---

## Estrutura do projeto

```
vetSync-java-main/
├── documentos/
│   ├── JornadaPet_Postman_Collection.json   (collection do Postman)
│   └── cronograma-sprint1.md
├── src/
│   ├── main/
│   │   ├── java/br/com/fiap/VetSync/
│   │   │   ├── config/         SecurityConfig, SwaggerConfig
│   │   │   ├── controller/     Auth, Tutor, Pet, Evento, Plano, Prescricao,
│   │   │   │                   Pontos, Recompensa, Medicamento, TipoEvento,
│   │   │   │                   Veterinario, Admin
│   │   │   ├── entity/         Tutor, Pet, EventoSaude, PlanoTratamento,
│   │   │   │                   PlanoItem, Prescricao, LancamentoPontos,
│   │   │   │                   Recompensa, Resgate, Veterinario, Admin, ...
│   │   │   ├── repository/     Spring Data JPA repositories
│   │   │   ├── service/        Regras de negócio de cada domínio
│   │   │   ├── security/       JwtFilter, AppUserDetailsService, TokenBlacklist,
│   │   │   │                   PetSecurity, TutorSecurity, EventoSecurity, ...
│   │   │   ├── exception/      GlobalExceptionHandler
│   │   │   └── data/           MockData (seed de exemplo)
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── db/migration/   V1 a V10 (Flyway)
│   │       └── static/index.html   (console web / API tester embutido)
│   └── test/                   41 arquivos: unitários, integração e segurança
├── Dockerfile
├── docker-compose.yml           (sobe Oracle XE local + a aplicação)
├── deploy.sh                    (script de deploy via Azure CLI)
└── pom.xml
```

---

## Stack técnica

- **Java 17** + **Spring Boot 3.3.5**
- Spring Web, Spring Data JPA, Spring Security, Bean Validation
- **JWT** (`jjwt`) para autenticação stateless
- **Flyway** (`flyway-core` + `flyway-database-oracle`) para versionamento de schema
- **Oracle** (`ojdbc11`) em produção/dev · **H2** em modo compatibilidade Oracle nos testes
- **Springdoc OpenAPI** (Swagger UI)
- **Spring Mail** para notificação de senha temporária e liberação de prescrição
- **JUnit 5 + Spring Security Test + JaCoCo** (cobertura de testes)
- **Lombok**

---

## Como rodar

### Pré-requisitos

- Java 17+
- Maven 3.8+ (ou use o `./mvnw` incluso)
- Docker e Docker Compose (para subir o Oracle localmente)

### Opção 1 — Docker Compose (recomendado)

Sobe o Oracle XE 21c e a aplicação em containers, sem precisar de nenhuma instalação local de banco:

```bash
git clone https://github.com/<seu-usuario>/vetSync-java.git
cd vetSync-java

docker compose up --build
```

Aguarde o Oracle inicializar (o `healthcheck` do compose já garante que a aplicação só sobe depois que o banco estiver pronto — leva cerca de 60-90s na primeira vez). Ao final, o Flyway aplica todas as migrations automaticamente.

| Recurso | URL |
|---|---|
| API | http://localhost:8080 |
| Console web (frontend/API tester) | http://localhost:8080/index.html |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |

### Opção 2 — Maven, com Oracle próprio (ex: Oracle FIAP)

```bash
export SPRING_DATASOURCE_URL=jdbc:oracle:thin:@<host>:1521:<SID_ou_SERVICE>
export SPRING_DATASOURCE_USERNAME=<usuario>
export DB_PASSWORD=<senha>

./mvnw spring-boot:run
```

Por padrão, `application.properties` já aponta para o Oracle da FIAP (`oracle.fiap.com.br`) caso nenhuma variável seja exportada — ajuste conforme o ambiente disponível.

### Variáveis de ambiente

| Variável | Padrão (compose) | Descrição |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:oracle:thin:@oracle-db:1521/XEPDB1` | URL JDBC do Oracle |
| `SPRING_DATASOURCE_USERNAME` | `APP_USER` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` / `DB_PASSWORD` | `AppPassword123` | Senha do banco |
| `SPRING_FLYWAY_ENABLED` | `true` | Liga/desliga o Flyway |
| `JWT_SECRET` | chave dev incluída | Chave de assinatura do JWT (troque em produção) |
| `ADMIN_BOOTSTRAP_KEY` | `boot-secret-dev-12345` | Chave exigida para criar o primeiro admin |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP Gmail (dev) | Envio de e-mails (senha temporária, liberação de prescrição) |

> Sem configurar um servidor SMTP válido, os envios de e-mail falham silenciosamente em dev — o fluxo principal da API continua funcionando normalmente (a senha temporária também é retornada na resposta do endpoint).

---

## Como acessar / dados de teste

Ao subir a aplicação **sem** um admin ainda cadastrado, siga esta ordem:

**1. Existem usuários de exemplo pré-cadastrados (seed automático em `MockData`, ativo por padrão):**

| Perfil | E-mail | Senha |
|---|---|---|
| Veterinário | `ana.vet@clyvovet.com` | `senha123` |
| Tutor | `maria@email.com` | `senha123` |
| Tutor | `joao@email.com` | `senha123` |

O seed roda apenas se as tabelas estiverem vazias, e pode ser desligado com `app.mockdata.enabled=false`.

**2. Não existe admin pré-cadastrado.** Crie o primeiro com a chave de bootstrap:

```bash
curl -X POST http://localhost:8080/admins/bootstrap \
  -H "Content-Type: application/json" \
  -d '{"nome":"Admin Geral","email":"admin@vetsync.com","chave":"boot-secret-dev-12345"}'
```

Esse endpoint só funciona **uma vez** — depois do primeiro admin, sempre retorna `409 Conflict`. A senha temporária vem na resposta.

**3. Login (funciona para qualquer perfil):**

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"maria@email.com","senha":"senha123"}'
```

Resposta:
```json
{
  "token": "eyJhbGciOi...",
  "idUsuario": 1,
  "email": "maria@email.com",
  "nome": "Maria Silva",
  "perfil": "TUTOR"
}
```

**4. Use o token nas próximas requisições:**

```bash
curl http://localhost:8080/pets \
  -H "Authorization: Bearer eyJhbGciOi..."
```

**5. Ou use o console web** em `http://localhost:8080/index.html`: faça login pela aba "Autenticação" e as demais abas são liberadas automaticamente de acordo com o perfil logado (tutor, veterinário ou admin).

---

## Rotas da API

Rotas marcadas como **pública** não exigem token. As demais exigem `Authorization: Bearer <token>`, e as com **perfil** indicado exigem também aquele papel.

### Auth (`/auth`)

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | `/auth/registrar` | Cadastra um novo **tutor** e já retorna o token | Pública |
| POST | `/auth/login` | Login por e-mail/senha (tutor, veterinário ou admin) | Pública |
| POST | `/auth/logout` | Invalida o token atual (blacklist) | Autenticado |
| GET | `/auth/me` | Dados do usuário autenticado, para restaurar sessão | Autenticado |

### Tutores (`/tutores`)

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| GET | `/tutores` | Lista todos os tutores | VETERINARIO |
| GET | `/tutores/{id}` | Busca tutor por ID | VETERINARIO ou o próprio tutor |
| PUT | `/tutores/{id}` | Atualiza nome/telefone | O próprio tutor |
| DELETE | `/tutores/{id}` | Remove o cadastro | O próprio tutor |

### Pets (`/pets`)

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | `/pets` | Cadastra pet (tutor vem do token) | TUTOR |
| GET | `/pets` | Lista os pets do tutor autenticado | TUTOR |
| GET | `/pets/{id}` | Busca pet por ID | VETERINARIO ou dono do pet |
| GET | `/pets/tutor/{idTutor}` | Lista pets de um tutor específico | VETERINARIO |
| PUT | `/pets/{id}` | Atualiza dados do pet | Dono do pet |
| DELETE | `/pets/{id}` | Remove o pet (409 se houver eventos vinculados) | Dono do pet |

### Eventos de saúde (`/eventos`)

Fluxo: **AGENDADO → CONCLUIDO** ou **AGENDADO → CANCELADO** (com reagendamento automático opcional).

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | `/eventos` | Tutor agenda evento (valida conflito de horário/bloqueio) | TUTOR |
| GET | `/eventos` | Lista eventos do tutor ou do veterinário autenticado | Autenticado |
| GET | `/eventos/{id}` | Busca evento por ID | Tutor dono ou vet responsável |
| PATCH | `/eventos/{id}/concluir` | Veterinário conclui, informa custo, gera pontos e avança plano | VETERINARIO responsável |
| PATCH | `/eventos/{id}/cancelar` | Tutor cancela (motivo obrigatório), pode reagendar direto | TUTOR dono |
| DELETE | `/eventos/{id}` | Remove evento | Vet sempre; tutor só se AGENDADO |
| GET | `/eventos/pet/{idPet}/gasto-total` | Soma custos de eventos CONCLUIDOS | Autenticado |
| GET | `/eventos/pet/{idPet}/alertas` | Histórico + alerta de atraso por tipo de evento | Autenticado |

### Planos de tratamento (`/planos`)

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | `/planos` | Veterinário cria plano com sequência de eventos (mín. 2 itens) | VETERINARIO |
| GET | `/planos` | Lista planos do tutor ou do veterinário | Autenticado |
| GET | `/planos/{id}` | Detalha plano e seus itens | Tutor dono ou vet que prescreveu |
| PATCH | `/planos/itens/{idItem}/agendar` | Tutor agenda o próximo item pendente | TUTOR dono do item |

### Prescrições (`/prescricoes`)

Fluxo: **SOLICITADO → LIBERADO** ou **SOLICITADO → NEGADO**.

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | `/prescricoes` | Veterinário solicita medicamento para um evento | VETERINARIO |
| GET | `/prescricoes` | Tutor vê as dos próprios pets; vet vê as suas; admin vê a fila pendente | Autenticado |
| GET | `/prescricoes/{id}` | Busca por ID | Tutor dono, vet responsável ou admin |
| PATCH | `/prescricoes/{id}/liberar` | Admin aprova/nega (dispara e-mail se aprovado) | ADMIN |

### Pontos (`/pontos`)

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| GET | `/pontos` | Tutor vê seus lançamentos; admin vê a fila pendente | Autenticado |
| PATCH | `/pontos/{id}/liberar` | Admin libera lançamento pendente (entra no saldo do tutor) | ADMIN |

### Recompensas (`/recompensas`)

Fluxo de resgate: **PENDENTE → VALIDADO** ou **PENDENTE → NEGADO**.

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| GET | `/recompensas` | Lista recompensas ativas do catálogo | Autenticado |
| POST | `/recompensas` | Cadastra recompensa | VETERINARIO |
| GET | `/recompensas/saldo` | Saldo de pontos do tutor autenticado | TUTOR |
| PATCH | `/recompensas/{id}/resgatar` | Resgata recompensa (debita saldo, cria resgate pendente) | TUTOR |
| GET | `/recompensas/resgates` | Tutor vê os próprios; vet vê os pendentes | Autenticado |
| PATCH | `/recompensas/resgates/{idResgate}/validar` | Veterinário valida/nega resgate | VETERINARIO |

### Medicamentos (`/medicamentos`)

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | `/medicamentos` | Cadastra medicamento no catálogo | ADMIN ou VETERINARIO |
| GET | `/medicamentos` | Lista catálogo | Autenticado |
| GET | `/medicamentos/{id}` | Busca por ID | Autenticado |
| PUT | `/medicamentos/{id}` | Atualiza | ADMIN ou VETERINARIO |
| DELETE | `/medicamentos/{id}` | Remove | ADMIN |

### Tipos de evento (`/tipos-evento`)

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| GET | `/tipos-evento` | Lista catálogo (nome, categoria, pontos) | Autenticado |

### Veterinários e agenda (`/veterinarios`)

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| GET | `/veterinarios` | Lista veterinários | Autenticado |
| GET | `/veterinarios/{id}` | Busca por ID | Autenticado |
| POST | `/veterinarios` | Cadastra veterinário (gera CRM + senha temporária por e-mail) | ADMIN |
| PUT | `/veterinarios/{id}` | Atualiza dados | O próprio veterinário |
| GET/POST/DELETE | `/veterinarios/{id}/disponibilidade[/{idDisponibilidade}]` | Horários fixos de atendimento por dia da semana | Leitura livre; escrita só o próprio |
| GET/POST/DELETE | `/veterinarios/{id}/bloqueios[/{idBloqueio}]` | Bloqueios de agenda (férias, compromissos) | Leitura livre; escrita só o próprio |

### Admin (`/admins`)

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | `/admins/bootstrap` | Cria o **primeiro** admin do sistema, exige `ADMIN_BOOTSTRAP_KEY` | Pública (só funciona uma vez) |
| POST | `/admins` | Admin autenticado cria outro admin | ADMIN |

### Utilitários

| Método | Rota | Descrição |
|---|---|---|
| GET | `/actuator/health` | Health check |
| GET | `/swagger-ui.html` | Documentação interativa (Swagger UI) |
| GET | `/index.html` | Console web / API tester |

---

## Testes

O projeto tem **41 classes de teste** cobrindo unidade, repositório, controller (`@WebMvcTest`), integração ponta-a-ponta e regras de segurança, usando H2 em memória em modo de compatibilidade Oracle (Flyway desabilitado nos testes).

```bash
./mvnw test
```

Relatório de cobertura (JaCoCo):

```bash
./mvnw test jacoco:report
# abrir target/site/jacoco/index.html
```

Principais suítes de integração:
- `AuthFlowIntegrationTest` — registro, login, logout e `/me`
- `AdminBootstrapIntegrationTest` — bootstrap do primeiro admin
- `EventoPontosRecompensaIntegrationTest` — agendar, concluir, gerar pontos, liberar, trocar por recompensa
- `PlanoTratamentoFlowIntegrationTest` — criação de plano, agendamento sequencial dos itens, conclusão, bônus
- `PrescricaoFlowIntegrationTest` — solicitação, liberação/negação, notificação

---

## Coleção Postman

Uma collection pronta está em [`documentos/JornadaPet_Postman_Collection.json`](./documentos/JornadaPet_Postman_Collection.json) — importe no Postman e configure a variável de ambiente `token` após o login.

Atenção: a collection ainda usa o nome legado `JornadaPet`; as rotas nela podem estar desatualizadas em relação à tabela acima — use a tabela de rotas deste README ou o Swagger UI (`/swagger-ui.html`) como fonte da verdade.

---

## Deploy

O deploy oficial usa Azure App Service com Oracle FIAP. O workflow
`.github/workflows/deploy-azure.yml` executa `./mvnw clean verify` em pull requests e
pushes para `main`. Em um push para `main`, o mesmo JAR validado é publicado no App
Service e o endpoint `/actuator/health` é verificado.

O acesso do GitHub Actions ao Azure usa OIDC, sem senha ou publish profile. Os recursos,
o Key Vault, a identidade federada e os identificadores `AZURE_CLIENT_ID`,
`AZURE_TENANT_ID` e `AZURE_SUBSCRIPTION_ID` são configurados pelos scripts do
repositório [vetSync-DevOps](https://github.com/Challenge-2TDSPG-2026/vetSync-DevOps).

Antes de enviar uma alteração para `main`, valide localmente:

```bash
./mvnw clean verify
```

O histórico aplicado no Oracle é controlado pelo Flyway. Não altere migrations que já
tenham sido executadas; mudanças de schema devem receber uma nova versão.

---

*VetSync — FIAP 2026 | Challenge Clyvo Vet | 2º Ano ADS*
