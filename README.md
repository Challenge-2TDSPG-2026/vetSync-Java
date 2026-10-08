<h1 align="center">🐾 VetSync (JornadaPet)</h1>

<p align="center">
  API REST para continuidade do cuidado e engajamento na jornada de saúde do pet,<br/>
  desenvolvida em parceria com a <strong>Clyvo Vet</strong>.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?logo=openjdk" alt="Java 17" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?logo=springboot" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Maven-3.9-C71A36?logo=apachemaven" alt="Maven" />
  <img src="https://img.shields.io/badge/Oracle-FIAP-F80000?logo=oracle" alt="Oracle" />
  <img src="https://img.shields.io/badge/Flyway-migrations-CC0200?logo=flyway" alt="Flyway" />
  <img src="https://img.shields.io/badge/Docker-ready-2496ED?logo=docker" alt="Docker" />
  <img src="https://img.shields.io/badge/Azure-App%20Service-0078D4?logo=microsoftazure" alt="Azure" />
  <img src="https://img.shields.io/badge/License-MIT-green" alt="MIT" />
</p>

---

## 📑 Sumário

- [Sobre o projeto](#-sobre-o-projeto)
- [Funcionalidades](#-funcionalidades)
- [Perfis de acesso](#-perfis-de-acesso)
- [Tecnologias](#-tecnologias)
- [Arquitetura](#-arquitetura)
- [Estrutura do projeto](#-estrutura-do-projeto)
- [Pré-requisitos](#-pré-requisitos)
- [Como executar](#-como-executar)
- [Variáveis de ambiente](#-variáveis-de-ambiente)
- [Banco de dados e migrations](#-banco-de-dados-e-migrations)
- [Documentação da API](#-documentação-da-api)
- [Segurança](#-segurança)
- [Testes](#-testes)
- [CI/CD e deploy](#-cicd-e-deploy)
- [Documentação complementar](#-documentação-complementar)
- [Equipe](#-equipe)
- [Licença](#-licença)

---

## 📖 Sobre o projeto

O **VetSync** (também referenciado como *JornadaPet*) é uma API REST que centraliza a jornada de saúde de pets, conectando **tutores**, **clínicas veterinárias**, **veterinários** e **profissionais de estética animal**.

O sistema ajuda o tutor a não perder vacinas, consultas e retornos, dá ao veterinário uma visão operacional da agenda e dos pacientes, e mantém um histórico clínico confiável e auditável. Um programa de **pontos e recompensas** incentiva o cuidado preventivo.

Projeto desenvolvido para o **Challenge FIAP 2026** (disciplina *Java Advanced*, turma 2TDSPG) em parceria com a **Clyvo Vet**. A API é consumida por um aplicativo mobile (React Native / Expo) e por um site web.

## ✨ Funcionalidades

### 🐕 Pets e tutores
- Cadastro, edição, busca e paginação de pets, com **foto** e catálogo de **espécies e raças**
- Gestão de tutores e **responsáveis adicionais**
- **Perfil de saúde** do pet (alergias, medicamentos contínuos, condições pré-existentes, etc.)
- **Histórico de peso** para gráficos de evolução
- **Acesso compartilhado**: convites por link/token com expiração, permissões granulares e revogação imediata

### 🩺 Eventos clínicos e agenda
- Eventos de saúde (consultas, vacinas, vermifugação, estética etc.) com status, custo, observações clínicas, diagnóstico e conduta
- **Detalhes completos**, **histórico de alterações**, **anexos** e **reagendamento** de eventos
- Conclusão e cancelamento de eventos
- **Agenda do dia** e **slots disponíveis** por veterinário, com disponibilidades e bloqueios
- Cálculo de **gasto total** e **alertas** por pet
- **Agendamento de retorno** por link público com token

### 💉 Vacinação e prevenção
- **Carteira de vacinação** com tipos de vacina, periodicidade e status calculado (em dia, vencendo, atrasada)
- **Carteira compartilhável** por link público/QR Code, com expiração configurável
- **Próximas ações e pendências** por pet

### 💊 Tratamentos
- **Prescrições** (com anexo em PDF) e catálogo de **medicamentos**
- **Planos de tratamento** com itens agendáveis

### ✂️ Estética
- Cadastro de **profissionais de estética**, serviços base e extras
- Disponibilidade e bloqueios de agenda dedicados
- **Relatórios de estética** com liberação para o tutor

### 🏥 Clínicas
- **Vínculo tutor–clínica** por código, com troca de clínica e ativação de contrato
- **Painel administrativo** com resumo e **relatórios da clínica**
- **Auditoria** das alterações importantes, filtrável por tipo

### 🎁 Pontos e recompensas
- Lançamento de **pontos** por eventos realizados, com estados (pendente, liberado, bloqueado) e **validade**
- **Catálogo de recompensas** com imagem, **resgate** pelo tutor e **validação** pela clínica

### 🔔 Notificações
- Registro de dispositivos (**Expo Push**), notificações in-app e **preferências** por usuário
- Job agendado de **lembretes** com respeito ao fuso horário

### 🤖 IA (SIA)
- **Ações assistidas por IA** com fluxo de *preview* e **confirmação explícita** antes da execução

## 👥 Perfis de acesso

| Perfil | Descrição |
|--------|-----------|
| `TUTOR` | Dono do pet: gerencia pets, eventos, vacinas, pontos e resgates |
| `VETERINARIO` | Atende pacientes, gerencia agenda, prescrições e planos de tratamento |
| `PROFISSIONAL_ESTETICA` | Gerencia serviços, agenda e relatórios de estética |
| `ADMIN` | Administra clínicas, vínculos, recompensas, pontos e painel |

A autorização é feita por *roles* e por regras de propriedade (por tutor, pet, veterinário e clínica) nas classes do pacote `security`.

## 🛠 Tecnologias

| Categoria | Tecnologia |
|-----------|------------|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.3.5 (Web, Data JPA, Validation, Security, Actuator, Cache, Mail, Thymeleaf) |
| Autenticação | JWT (jjwt 0.11.5) + BCrypt |
| Banco de dados | Oracle (FIAP) em produção · H2 (modo Oracle) nos testes |
| Migrations | Flyway (`flyway-core` + `flyway-database-oracle`) |
| Documentação | SpringDoc OpenAPI 2.6.0 (Swagger UI) |
| Produtividade | Lombok |
| Testes | JUnit 5, Spring Boot Test, Spring Security Test, JaCoCo |
| Containers | Docker, Docker Compose |
| Cloud / CI-CD | Azure App Service, Azure Key Vault, GitHub Actions (OIDC) |

## 🏗 Arquitetura

Arquitetura em camadas, com separação clara de responsabilidades:

```
Cliente (App Mobile / Web)
        │  HTTPS + JWT
        ▼
┌───────────────────────────────┐
│  Controller   (REST + OpenAPI) │
├───────────────────────────────┤
│  Security     (JWT, regras de acesso por perfil/propriedade) │
├───────────────────────────────┤
│  Service      (regras de negócio)  │
├───────────────────────────────┤
│  Repository   (Spring Data JPA)    │
└──────────────┬────────────────┘
               ▼
        Oracle (Flyway)
```

Pontos de atenção do projeto:
- **Sessões stateless** com JWT; logout com *blacklist* de tokens e invalidação de sessões após troca de senha
- **Rate limiter** no endpoint público da carteira compartilhada
- Tratamento global de exceções (`GlobalExceptionHandler`)
- Cache em memória (`spring.cache.type=simple`)
- Jobs agendados (`@Scheduled`) para lembretes e notificações

## 📂 Estrutura do projeto

```
vetSync-Java/
├── .github/workflows/        # Pipeline CI/CD (deploy-azure.yml)
├── .mvn/                     # Maven Wrapper
├── documentos/               # Coleção Postman, cronograma e imagens
├── scripts/                  # Provisionamento Azure e DDL Oracle
│   └── Database/script_bd.sql
├── src/
│   ├── main/
│   │   ├── java/br/com/fiap/VetSync/
│   │   │   ├── config/       # SecurityConfig, SwaggerConfig
│   │   │   ├── controller/   # Endpoints REST
│   │   │   ├── entity/       # Entidades JPA e enums de status
│   │   │   ├── exception/    # GlobalExceptionHandler
│   │   │   ├── repository/   # Repositórios Spring Data
│   │   │   ├── security/     # JWT, filtros e regras de acesso
│   │   │   └── service/      # Regras de negócio
│   │   └── resources/
│   │       ├── db/migration/ # Migrations Flyway (V1 a V47)
│   │       ├── static/       # Console de testes (index.html)
│   │       ├── templates/    # Páginas da carteira pública (Thymeleaf)
│   │       └── application.properties
│   └── test/                 # Testes unitários, de controller e de integração
├── Dockerfile
├── docker-compose.yml
├── deploy.sh
└── pom.xml
```

## ✅ Pré-requisitos

- **JDK 17+**
- **Git**
- **Maven 3.9+** (opcional, o projeto inclui o `mvnw`)
- **Docker** e **Docker Compose** (opcional, para execução em containers)
- Acesso a um banco **Oracle** (ou o container Oracle XE do `docker-compose.yml`)

## 🚀 Como executar

### 1. Clonar o repositório

```bash
git clone https://github.com/Challenge-2TDSPG-2026/vetSync-Java.git
cd vetSync-Java
```

### 2. Configurar as variáveis de ambiente

As variáveis `JWT_SECRET` e `ADMIN_BOOTSTRAP_KEY` são **obrigatórias**. Veja a [lista completa](#-variáveis-de-ambiente).

```bash
export JWT_SECRET="uma-chave-secreta-com-no-minimo-32-caracteres"
export ADMIN_BOOTSTRAP_KEY="chave-para-criar-o-primeiro-admin"
export SPRING_DATASOURCE_URL="jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL"
export SPRING_DATASOURCE_USERNAME="RMXXXXXX"
export DB_PASSWORD="sua-senha"
```

> No Windows PowerShell, use `$env:JWT_SECRET = "..."`.

### 3a. Executar localmente com Maven

```bash
./mvnw spring-boot:run
```

### 3b. Executar com Docker Compose (Oracle XE + API)

```bash
docker compose up --build
```

O compose sobe um **Oracle XE 21** e a aplicação na porta **8080**. Defina `JWT_SECRET` e `ADMIN_BOOTSTRAP_KEY` no ambiente antes de subir (ou em um arquivo `.env`, que já está no `.gitignore`).

> Os valores de usuário e senha do `docker-compose.yml` são apenas para **desenvolvimento local**. Nunca os reutilize em produção.

### 3c. Gerar o JAR

```bash
./mvnw clean package
java -jar target/VetSync-0.0.1-SNAPSHOT.jar
```

### 4. Acessar

| Recurso | URL |
|---------|-----|
| API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI (JSON) | `http://localhost:8080/v3/api-docs` |
| Health check | `http://localhost:8080/actuator/health` |
| Console de testes | `http://localhost:8080/` |

### 5. Criar o primeiro administrador

Com a aplicação no ar, use o endpoint de bootstrap, protegido pela `ADMIN_BOOTSTRAP_KEY`:

```
POST /admins/bootstrap
```

Consulte o Swagger para o formato do corpo e do cabeçalho esperados.

## ⚙️ Variáveis de ambiente

| Variável | Obrigatória | Padrão | Descrição |
|----------|:-----------:|--------|-----------|
| `JWT_SECRET` | ✅ | — | Segredo de assinatura dos tokens JWT |
| `ADMIN_BOOTSTRAP_KEY` | ✅ | — | Chave para criar o primeiro administrador |
| `SPRING_DATASOURCE_URL` | | `jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL` | URL JDBC do banco |
| `SPRING_DATASOURCE_USERNAME` | | — | Usuário do banco |
| `DB_PASSWORD` | | — | Senha do banco |
| `SPRING_FLYWAY_ENABLED` | | `true` | Habilita as migrations |
| `SERVER_PORT` | | `8080` | Porta da aplicação |
| `MAIL_HOST` / `MAIL_PORT` | | `localhost` / `587` | Servidor SMTP |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | | — | Credenciais SMTP (recuperação de senha) |
| `APP_CORS_WEB_ORIGIN` | | — | Origens web extras liberadas no CORS (separadas por vírgula) |
| `WEB_INVITE_URL` | | `https://vetsync-theta.vercel.app/convite` | Página web que trata o aceite de convites |
| `PET_CONVITE_EXPIRACAO_HORAS` | | `72` | Validade do convite de acesso ao pet |
| `CARTEIRA_COMPARTILHADA_EXPIRACAO_DIAS` | | `30` | Validade do link da carteira compartilhada |
| `NOTIFICACOES_PUSH_HABILITADO` | | `true` | Liga/desliga o envio de push |
| `NOTIFICACOES_JOB_HABILITADO` | | `true` | Liga/desliga o job de lembretes |
| `NOTIFICACOES_FUSO_CLINICA` | | `America/Sao_Paulo` | Fuso padrão da clínica |
| `EXPO_PUSH_URL` | | `https://exp.host/--/api/v2/push/send` | Endpoint do Expo Push |
| `MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS` | | `never` | Detalhes do `/actuator/health` |

Parâmetros fixos relevantes: validade dos pontos de **365 dias**, upload máximo de **5 MB** e expiração do JWT de **24 h**.

## 🗄 Banco de dados e migrations

- O esquema é gerenciado **exclusivamente pelo Flyway** (`spring.jpa.hibernate.ddl-auto=none`).
- As migrations ficam em `src/main/resources/db/migration` (de `V1__create_tables.sql` até `V47__...`).
- O pool Hikari é limitado a **2 conexões** para respeitar a cota `SESSIONS_PER_USER` do Oracle FIAP.
- O DDL consolidado está em `scripts/Database/script_bd.sql`.
- Nos testes é usado **H2 em modo Oracle**, sem dependência de banco externo.

## 📡 Documentação da API

A documentação interativa está no **Swagger UI** (`/swagger-ui.html`). Uma coleção Postman está em [`documentos/JornadaPet_Postman_Collection.json`](documentos/JornadaPet_Postman_Collection.json).

Resumo dos principais grupos de rotas (consulte o Swagger para o detalhamento):

| Grupo | Base | Destaques |
|-------|------|-----------|
| Autenticação | `/auth` | `login`, `registrar`, `logout`, `me`, `esqueci-senha`, `validar-codigo`, `redefinir-senha`, convites |
| Admin e painel | `/admins`, `/painel` | `bootstrap`, criação de admin, `resumo` |
| Tutores | `/tutores` | consulta, edição e remoção |
| Pets | `/pets` | CRUD, busca, raças, foto, por tutor |
| Saúde do pet | `/pets/{id}/...` | `proximas-acoes`, `carteira-vacinacao`, `perfil-saude`, `peso`, `peso/historico` |
| Acesso compartilhado | `/pets/{idPet}/acessos`, `/convites` | convites, aceite, permissões, revogação |
| Carteira compartilhável | `/pets/{idPet}/carteira-compartilhavel`, `/carteiras-publicas/{token}` | link público com expiração |
| Eventos | `/eventos` | CRUD, `detalhes`, `historico`, `reagendar`, `concluir`, `cancelar`, `anexos`, estética |
| Tipos de evento | `/tipos-evento` | catálogo de tipos |
| Agenda | `/agenda` | `slots`, `dia` |
| Veterinários | `/veterinarios` | CRUD, disponibilidade, bloqueios |
| Estética | `/profissionais-estetica`, `/servicos-estetica`, `/relatorios-estetica` | profissionais, serviços, relatórios |
| Prescrições | `/prescricoes`, `/medicamentos` | emissão, liberação, catálogo |
| Planos de tratamento | `/planos` | criação, listagem, agendamento de itens |
| Clínicas | `/vinculos-clinica` | validar código, trocar, gerar/remover código, contrato |
| Pontos e recompensas | `/pontos`, `/recompensas` | saldos, liberar/bloquear, resgatar, validar resgate |
| Notificações | `/notificacoes`, `/usuarios/preferencias/notificacoes` | dispositivos, lidas, preferências |
| Relatórios e auditoria | `/veterinarios/me/relatorios`, `/auditoria` | resumo, trilha de auditoria |
| IA | `/ia/acoes` | `preview`, `confirmar` |
| Retorno | `/agendamentos-retorno/{token}` | consulta e confirmação públicas |

### Exemplo de autenticação

```bash
# Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"tutor@exemplo.com","senha":"suaSenha"}'

# Chamada autenticada
curl http://localhost:8080/auth/me \
  -H "Authorization: Bearer <TOKEN>"
```

A resposta do login traz `token`, `idUsuario`, `email`, `nome`, `perfil` e `temVinculoAtivo`.

## 🔐 Segurança

- Autenticação **JWT stateless** e senhas com **BCrypt**
- `@EnableMethodSecurity` com regras de propriedade por tutor, pet, evento, plano e profissional
- **Logout** com blacklist de tokens e invalidação de sessões após troca de senha
- Recuperação de senha por **código enviado por e-mail**
- **CORS** restrito a localhost, emulador Android, site oficial e origens configuradas por ambiente
- Rotas públicas limitadas ao necessário: login, registro, recuperação de senha, convites, carteira pública, retorno por token, health e Swagger
- Segredos fora do código: variáveis de ambiente e **Azure Key Vault**; arquivos `.env` ignorados pelo Git
- Container executado com **usuário não-root**

## 🧪 Testes

O projeto possui testes de **repositório**, **serviço**, **controller**, **segurança (CORS e controle de acesso)** e **integração** (fluxo de autenticação, bootstrap de admin, planos de tratamento e pontos/recompensas).

```bash
# Executar todos os testes
./mvnw test

# Build completo com verificação (usado no CI)
./mvnw clean verify
```

O relatório de cobertura do **JaCoCo** é gerado em `target/site/jacoco/index.html`.

## ⚡ CI/CD e deploy

O pipeline em [`.github/workflows/deploy-azure.yml`](.github/workflows/deploy-azure.yml) executa:

1. **Build and tests**: `mvn clean verify` com Java 17 (em *push* e *pull request* na `main`) e geração do artefato com checksum SHA-256
2. **Deploy**: em *push* na `main`, autentica no Azure via **OIDC**, publica o JAR no **Azure App Service** e valida o `/actuator/health`

Scripts de infraestrutura (Azure CLI) na pasta [`scripts/`](scripts):

| Script | Função |
|--------|--------|
| `1-system.sh` | Cria grupo de recursos, plano e App Service |
| `2-oracle-fiap.sh` | Configura a conexão Oracle FIAP e segredos |
| `3-backend-deploy.sh` | Faz o deploy do backend |
| `4-github-oidc.sh` | Configura identidade federada GitHub → Azure |
| `sync-oracle-ddl.sh` | Sincroniza o DDL no Oracle |
| `x-delete.sh` | Remove os recursos criados |

## 📚 Documentação complementar

- [`implementaçõesBACKEND.md`](implementaçõesBACKEND.md): roadmap de evolução do backend (notificações, próximas ações, vacinação, auditoria, relatórios e SIA)
- [`documentos/cronograma-sprint1.md`](documentos/cronograma-sprint1.md): cronograma e divisão de responsabilidades da Sprint 1
- [`documentos/JornadaPet_Postman_Collection.json`](documentos/JornadaPet_Postman_Collection.json): coleção Postman

## 👨‍💻 Equipe

| Nome | RM |
|------|----|
| Arthur Brito da Silva | 562085 |
| Luiz Felipe Flosi dos Santos | 563197 |
| Pedro Henrique Brum Lopes | 561780 |

**Instituição:** FIAP · **Disciplina:** Java Advanced · **Empresa parceira:** Clyvo Vet

## 📄 Licença

Distribuído sob a licença **MIT**. Veja o arquivo [`LICENSE`](LICENSE) para mais detalhes.
