# 💎 Projeto Diamante 02 — Delivery com microsserviços

Monorepo **delivery-system** (Gradle) — Java Advanced, 2º semestre.

## Integrantes

| Nome | RM |
|------|----|
| Arthur Brito da Silva | 562085 |
| Luiz Felipe Flosi dos Santos | 563197 |
| Pedro Henrique Brum Lopes | 561780 |

## Stack

Java 25 · Spring Boot 4.0.8 · Spring Cloud 2025.1.2 (Eureka, LoadBalancer) · Spring Framework 7 (`@Retryable`) · Spring AMQP + RabbitMQ 4 · Spring Data JPA + H2 · Spring AI 2.0.1

## Arquitetura

```
App React Native ──REST──▶ order-service :8080 ──registro──▶ eureka-server :8761
 (usa :8080 e :8083)         │  cardápio · pedidos com lock    ▲   ▲
                             │  publica avaliações · IA        │   │ (todos se registram)
                             ├─ LB + retry ─▶ payment-service :8081 / :8082  (falha em ~50%)
                             ├─ AMQP ─▶ RabbitMQ: delivery.exchange ─(reviews.new)─▶ reviews.queue
                             │                                                    │
                             └─ ChatClient ─▶ Groq / OpenAI / LM Studio / Ollama         ▼
                                                              review-service :8083
                                                              buffer + flush a cada 5 s · ranking
```

| Serviço | Porta | Responsabilidade |
|---|---|---|
| `eureka-server` | 8761 | Service discovery |
| `order-service` | 8080 | Cardápio, pedidos (lock pessimista), publicação de avaliações, assistente de IA |
| `payment-service` | 8081 / 8082 | Pagamento simulado, instável (500 em ~50% das chamadas) |
| `review-service` | 8083 | Consome avaliações, acumula em memória, grava a cada 5 s, ranking |

## Como rodar (passo a passo)

### 0. Pré-requisitos

| Ferramenta | Versão | Como conferir |
|---|---|---|
| JDK | **25** | `java -version` |
| Docker Desktop | qualquer recente (precisa estar **aberto**) | `docker --version` |
| Git | qualquer | `git --version` |

Não precisa instalar Gradle: o projeto usa o wrapper (`gradlew`).

### 1. Clonar o projeto

```bash
git clone https://github.com/PedroBrum-DEV/diamante-delivery.git
cd diamante-delivery
```

### 2. Subir o RabbitMQ

Com o Docker Desktop aberto, na raiz do projeto:

```bash
docker compose up -d
```

Confirme em <http://localhost:15672> (usuário `guest`, senha `guest`).

### 3. Configurar o assistente de IA (Groq, gratuito)

O assistente usa uma API compatível com a OpenAI. Recomendamos a **Groq**, que tem camada gratuita.

1. Crie uma chave em <https://console.groq.com/keys> (ela começa com `gsk_`).
2. Copie o arquivo de exemplo:

   ```bash
   cp .env.example .env          # Windows PowerShell: Copy-Item .env.example .env
   ```

3. Edite o `.env` (ele está no `.gitignore`, **nunca** vai para o repositório) deixando assim:

   ```properties
   OPENAI_BASE_URL=https://api.groq.com/openai/v1
   OPENAI_API_KEY=gsk_sua_chave_aqui
   OPENAI_MODEL=openai/gpt-oss-20b
   ```

   O nome do modelo pode mudar com o tempo. Confira a lista atual em <https://console.groq.com/docs/models>.

4. **Importante:** o perfil `local` (que aplica o `OPENAI_BASE_URL`) precisa ser ativado como **variável de ambiente do terminal**. Colocar `SPRING_PROFILES_ACTIVE` dentro do `.env` **não funciona**. Isso é feito no passo 4, no terminal do `order-service`.

> Sem chave configurada, todo o resto funciona normalmente. Apenas `POST /assistant` responde `503 {"error": "..."}`.

### 4. Subir os serviços (um terminal para cada)

Abra **cinco terminais** na raiz do projeto e rode nesta ordem. Espere cada serviço terminar de subir (aparece `Started ...` no log) antes de passar ao próximo.

| Terminal | Serviço | Comando (Linux/macOS) | Comando (Windows PowerShell) |
|---|---|---|---|
| 1 | Eureka | `./gradlew :eureka-server:bootRun` | `.\gradlew.bat :eureka-server:bootRun` |
| 2 | Pagamento (8081) | `./gradlew :payment-service:bootRun` | `.\gradlew.bat :payment-service:bootRun` |
| 3 | Pagamento (8082) | `./gradlew :payment-service:bootRun --args='--server.port=8082'` | `.\gradlew.bat :payment-service:bootRun --args="--server.port=8082"` |
| 4 | Avaliações | `./gradlew :review-service:bootRun` | `.\gradlew.bat :review-service:bootRun` |
| 5 | Pedidos + IA | veja abaixo | veja abaixo |

**Terminal 5 (order-service) com a IA da Groq:**

Linux/macOS:

```bash
export SPRING_PROFILES_ACTIVE=local
./gradlew :order-service:bootRun
```

Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
.\gradlew.bat :order-service:bootRun
```

No log de inicialização deve aparecer `The following 1 profile is active: "local"`. Se aparecer `No active profile set`, o perfil não foi ativado e a chamada irá para a OpenAI (erro 401).

Se quiser usar a OpenAI de verdade em vez da Groq, remova `OPENAI_BASE_URL` do `.env`, coloque sua chave `sk-...` em `OPENAI_API_KEY` e **não** ative o perfil `local`.

### 5. Conferir que está tudo no ar

Abra o Eureka em <http://localhost:8761>. Devem aparecer:

- `ORDER-SERVICE`
- `PAYMENT-SERVICE` (2 instâncias: 8081 e 8082)
- `REVIEW-SERVICE`

### 6. Testar o assistente

Linux/macOS/Git Bash:

```bash
curl -X POST http://localhost:8080/assistant \
  -H "Content-Type: application/json" \
  -d '{"question": "O que vocês têm no cardápio?"}'
```

Windows PowerShell (o corpo é enviado em UTF-8 para os acentos funcionarem):

```powershell
$body = '{"question": "O que vocês têm no cardápio?"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/assistant -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
```

O PowerShell 5.1 pode exibir os acentos da resposta quebrados (`OpÃ§Ãµes`). É só a exibição no terminal: a API envia o texto correto. No app e no `requests.http` isso não acontece.

Outros exemplos prontos estão em [`requests.http`](requests.http) (extensão REST Client no VS Code ou o HTTP Client do IntelliJ).

### 7. Parar tudo

`Ctrl+C` em cada terminal de serviço e, para o RabbitMQ:

```bash
docker compose down
```

### Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| `POST /assistant` retorna **400** no PowerShell | Acentos enviados fora de UTF-8 | Use o comando do passo 6 com `GetBytes` |
| `POST /assistant` retorna **503** | A chamada à IA falhou | Veja o `Caused by:` no log do `order-service` |
| `401 Incorrect API key ... platform.openai.com` | Perfil `local` não ativado, a chave da Groq foi para a OpenAI | `SPRING_PROFILES_ACTIVE=local` no terminal (passo 4) |
| `429 ... no credits remaining` | Conta da OpenAI sem crédito | Adicione crédito ou use a Groq |
| `429` da Groq | Limite da camada gratuita | Aguarde alguns instantes |
| `404` / `model_not_found` | Nome do modelo inválido | Confira `OPENAI_MODEL` na lista da Groq |
| `SSLHandshakeException: PKIX path building failed` | Rede (escola, empresa ou antivírus) intercepta o HTTPS e o Java não confia no certificado | Veja abaixo |
| Log com `localhost:8761 Connection refused` | Eureka não está rodando | Suba o `eureka-server` (terminal 1) |
| `Connection refused` na porta 5672 | RabbitMQ parado | `docker compose up -d` com o Docker Desktop aberto |

**Erro de certificado (PKIX):** faça o Java usar os certificados do Windows, no mesmo terminal do `order-service`, antes do `bootRun`:

```powershell
.\gradlew.bat --stop
$env:JAVA_TOOL_OPTIONS = "-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT"
```

Se persistir, teste em outra rede (por exemplo, o hotspot do celular).

### Alternativa: modelo local (LM Studio / Ollama)

Sem internet e sem chave. Suba o servidor local, ajuste o `.env` e ative o perfil `local`:

```properties
OPENAI_BASE_URL=http://localhost:11434/v1     # Ollama (LM Studio: http://localhost:1234/v1)
OPENAI_MODEL=nome-do-modelo-carregado
```

### Variáveis opcionais

| Variável | Para quê |
|---|---|
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Endereço do Eureka, se não estiver na mesma máquina |
| `SPRING_RABBITMQ_HOST` | Endereço do RabbitMQ |
| `DELIVERY_RATE_LIMIT_ORDERS_PER_SECOND` | Limite do bônus (padrão 20; `0` desliga) |

Nenhum serviço chama outro por `localhost`: o pagamento é chamado por `http://PAYMENT-SERVICE/payments`, resolvido pelo Eureka.

## Contrato da API

Todos os erros têm o formato `{"error": "mensagem"}`.

| Método | Rota | Corpo | Respostas |
|---|---|---|---|
| GET | `/dishes` | — | 200 lista de Dish |
| GET | `/dishes/{id}` | — | 200 Dish · 404 |
| POST | `/orders` | `{"dishId":1,"quantity":2}` | 201 Order · 400 · 404 · 409 · 502 · 429 (bônus) |
| GET | `/orders/{id}` | — | 200 Order · 404 |
| POST | `/reviews` | `{"dishId":1,"rating":5,"comment":"Great"}` | 202 sem corpo · 400 · 404 |
| POST | `/assistant` | `{"question":"..."}` | 200 `{"answer":"..."}` |
| GET | `:8083/reviews/ranking` | — | 200 `[{dishId,dishName,average,count}]` |
| POST | `:8081/:8082/payments` (interno) | `{"amount":79.80}` | 200 `{"status":"APPROVED","instance":8081}` · 500 |

Exemplos prontos em [`requests.http`](requests.http).

## Como cada requisito foi atendido

| Tema | Onde / como |
|---|---|
| **Eureka** | `eureka-server`; os três serviços usam `spring.application.name` e `eureka-client`. Instâncias do pagamento têm `instance-id` com a porta |
| **Load balance** | `RestClientConfig`: `RestTemplate` com `@LoadBalanced`; `PaymentClient` chama `http://PAYMENT-SERVICE/payments`. Os logs mostram `[payment-service:8081]` / `[payment-service:8082]` alternando |
| **Retry** | `PaymentClient` (bean separado) com `@Retryable` (`maxRetries=4`, `delay=200`, `multiplier=2`, `jitter=100`, `maxDelay=2000`) e `@EnableResilientMethods`. Esgotadas as tentativas: rollback, estoque intacto, **502** |
| **Race condition** | `DishRepository.findByIdForUpdate` com `@Lock(PESSIMISTIC_WRITE)` + `@Transactional` em `OrderService.createOrder`. 50 requisições simultâneas → exatamente 10 confirmados, stock 0 |
| **Mensageria** | `RabbitConfig`: `TopicExchange delivery.exchange`, fila durável `reviews.queue`, routing key `reviews.new` e `Binding` explícitos; JSON via `JacksonJsonMessageConverter`. `POST /reviews` valida (1–5), publica e responde **202** sem gravar no banco |
| **Backpressure** | `review-service`: `@RabbitListener` → `ConcurrentHashMap` (soma + quantidade por prato) → `@Scheduled` a cada 5 s grava em `ReviewSummary` (H2) e limpa o buffer. `GET /reviews/ranking` lê do banco |
| **Spring AI** | `ChatService`: `ChatClient` criado do `ChatClient.Builder`, system message (atendente, respostas curtas em PT-BR, recusa fora do tema) e cardápio (nome, preço, estoque) lido do banco a cada pergunta |
| **Bônus rate limit** | `TokenBucket` + `RateLimitInterceptor`: 20 req/s em `POST /orders`, excedente recebe **429** `{"error": ...}` |
| **CORS** | Liberado em 8080 e 8083 (o app roda no navegador via Expo web) |

### Decisão de projeto: pagamento dentro da transação

O estoque é reservado, o pagamento é cobrado e o pedido é confirmado numa **única transação**, com a linha do prato bloqueada. Isso garante a regra "pagamento falhou ⇒ estoque intacto" por simples rollback. O custo: pedidos do mesmo prato ficam em fila enquanto o pagamento (com retries) responde. Para este cenário (10 unidades) é o comportamento desejado; em produção real, o usual seria reservar → cobrar fora do lock → compensar em caso de falha.

## Testando

```bash
# 50 pedidos simultâneos no prato da promoção (id 1, stock 10)
# (script em bash: no Windows, rode pelo Git Bash)
./scripts/concurrency-test.sh

# Teste automatizado (pagamento mockado): 50 threads => 10 pedidos, stock 0; falha de pagamento => rollback
./gradlew :order-service:test
```

Para ver apenas o par 201/409 sem o rate limit: `./gradlew :order-service:bootRun --args='--delivery.rate-limit.orders-per-second=0'`.

## App React Native

Use o app do professor ([joaocarloslima/diamante-delivery](https://github.com/joaocarloslima/diamante-delivery)) apontando para o IP da máquina que roda os serviços. Ele usa `:8080` (order-service) e `:8083` (review-service).
