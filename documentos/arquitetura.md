# Arquitetura do VetSync (JornadaPet)

Diagramas em Mermaid (renderizados automaticamente pelo GitHub).

## 1. Visão geral

```mermaid
flowchart LR
    subgraph Clientes
        APP[App mobile<br/>React Native / Expo]
        WEB[Site web]
        PUB[Link público<br/>carteira / retorno / convite]
    end

    subgraph Azure
        subgraph APPSVC[Azure App Service - Spring Boot 3.3 / Java 17]
            SEC[Security<br/>JWT + @PreAuthorize + regras por propriedade]
            CTRL[Controllers REST<br/>OpenAPI / Swagger]
            SVC[Services<br/>regras de negócio + jobs @Scheduled]
            REPO[Repositories<br/>Spring Data JPA]
        end
        KV[(Azure Key Vault<br/>segredos)]
    end

    DB[(Oracle FIAP<br/>schema versionado pelo Flyway)]
    EXPO[Expo Push API]
    GOOGLE[Google / Apple<br/>login social]
    SMTP[SMTP<br/>recuperação de senha]

    APP -->|HTTPS + JWT| SEC
    WEB -->|HTTPS + JWT| SEC
    PUB -->|HTTPS + token com expiração| CTRL
    SEC --> CTRL --> SVC --> REPO --> DB
    SVC --> EXPO
    SVC --> SMTP
    SEC --> GOOGLE
    KV -. segredos em runtime .-> APPSVC
```

## 2. Pipeline de CI/CD

```mermaid
flowchart LR
    DEV[Desenvolvedor] -->|push / PR em main| GH[GitHub]
    GH --> CI[Build + testes<br/>mvnw clean verify<br/>Java 17]
    CI --> ART[Artefato app.jar<br/>+ SHA-256]
    ART --> VAL[Verifica checksum]
    VAL --> OIDC[Login Azure via OIDC<br/>sem segredo de longa duração]
    OIDC --> DEPLOY[Deploy no App Service]
    DEPLOY --> HEALTH[GET /actuator/health]
```

## 3. Domínios da aplicação

```mermaid
flowchart TB
    subgraph Identidade
        AUTH[Auth / JWT / Login social]
        ADM[Admin e Admin de clínica]
    end
    subgraph Cuidado
        PET[Pets, perfil de saúde, peso, atividades]
        EVT[Eventos, agenda, lista de espera]
        VAC[Carteira de vacinação]
        PRO[Prontuário, prescrições, planos de tratamento]
        EST[Estética]
    end
    subgraph Relacionamento
        CLI[Clínicas, vínculos e equipe]
        SHR[Convites e acesso compartilhado]
        NOT[Notificações e push]
        IA[SIA - ações assistidas por IA<br/>preview + confirmação]
    end
    subgraph Engajamento
        PTS[Pontos com estado e validade]
        REC[Recompensas e resgates]
    end
    AUD[Auditoria]

    AUTH --> PET
    PET --> EVT --> VAC
    EVT --> PRO
    EVT --> PTS --> REC
    CLI --> EVT
    SHR --> PET
    NOT --> EVT
    IA --> EVT
    EVT --> AUD
    PRO --> AUD
```

> Os diagramas descrevem a estrutura lógica. O modelo físico completo está em
> [`scripts/Database/script_bd.sql`](../scripts/Database/script_bd.sql) e nas migrations em
> [`src/main/resources/db/migration`](../src/main/resources/db/migration).
## 4. Organização dos serviços de evento (SOLID)

O ciclo de vida de um evento de saúde foi dividido por responsabilidade (SRP), em vez de concentrado em uma única classe:

| Classe | Responsabilidade única |
|--------|------------------------|
| `EventoService` | Agendar (veterinário, estética e serviço da clínica), buscar e excluir |
| `EventoCancelamentoService` | Cancelar e reagendar; avisa a lista de espera quando um horário é liberado |
| `EventoConclusaoService` | Concluir o atendimento (observações, custo, pontos de fidelidade) |
| `EventoConfirmacaoService` | Confirmar ou recusar o agendamento pela clínica e avisar o tutor |
| `EventoAnexoService` | Anexos (imagens e PDFs) do evento |
| `EventoConsultaService` | Listagens, histórico, gasto total e alertas de atraso |
| `EventoRegistroService` | Histórico e auditoria das mudanças |
| `HorarioAgendaValidator` | Horário existente, não bloqueado e sem conflito |
| `PlanoEventoSincronizador` | Reflete conclusão/cancelamento do evento no plano de tratamento |
| `PoliticaConfirmacaoAgenda` | Regra: agendamento nasce PENDENTE ou já CONFIRMADO |

Utilitários compartilhados que eliminam código repetido (DRY):

- `security/TokenUtils`: geração de tokens, senhas temporárias e códigos aleatórios seguros, e SHA-256 em hexadecimal para guardar tokens apenas como hash.
- `exception/Erros`: fábrica das exceções HTTP repetidas (por exemplo, `Erros.petNaoEncontrado()`), mantendo status e mensagem em um lugar só.
