# gubee-stock-reconciliation

Serviço de reconciliação de estoque para integração com marketplaces (Mercado Livre, Shopee, etc), desenvolvido como desafio técnico para a posição de Desenvolvedor Java Pleno na Gubee.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Stack](#stack)
- [Arquitetura](#arquitetura)
- [Pré-requisitos](#pré-requisitos)
- [Como rodar](#como-rodar)
- [Endpoints](#endpoints)
- [Cenários de negócio](#cenários-de-negócio)
- [Testes](#testes)
- [i18n](#i18n)
- [Docker](#docker)

---

## Visão Geral

O serviço recebe eventos de estoque de múltiplos marketplaces e os reconcilia em uma fonte única de verdade por `accountId + SKU`. Os principais desafios tratados são:

- **Idempotência**: eventos duplicados são ignorados via `eventId` único
- **Concorrência**: Redis distributed lock + `@Version` otimista no JPA garantem que pedidos simultâneos não gerem estoque negativo
- **Eventos fora de ordem**: `ORDER_CANCELLED` sem `ORDER_CREATED` fica `PENDING` e é reprocessado automaticamente quando o pedido chegar
- **Inconsistências**: cancelamentos duplicados são detectados e registrados como `INCONSISTENT`

---

## Stack

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 17 | Linguagem |
| Spring Boot | 3.5.x | Framework principal |
| PostgreSQL | 16 | Persistência + auditoria |
| Redis | 7 | Lock distribuído |
| Apache Kafka | 3.9 | Ingestão assíncrona de eventos |
| Flyway | 11 | Migrations |
| Testcontainers | 1.21 | Testes de integração |
| SpringDoc/Swagger | 2.8.8 | Documentação da API |
| Docker | - | Containerização |

---

## Arquitetura

O projeto segue **arquitetura hexagonal** (Ports & Adapters):

```
com.gubee.stock
├── domain/
│   ├── model/          → StockBalance, StockEvent, StockHistory, enums
│   └── exception/      → BusinessException, InsufficientStockException, StockNotFoundException
├── application/
│   ├── port/
│   │   ├── in/         → ProcessEventUseCase, QueryStockUseCase, QueryInconsistenciesUseCase
│   │   └── out/        → StockBalanceRepository, StockHistoryRepository, ProcessedEventRepository, StockLockPort, EventPublisherPort
│   └── service/        → ProcessEventService, QueryStockService, QueryInconsistenciesService
└── infrastructure/
    ├── config/         → KafkaConfig, LocaleConfig
    ├── kafka/          → StockEventConsumer, KafkaEventPublisher
    ├── persistence/    → Entities, JPA Repositories, Adapters
    ├── redis/          → RedisStockLockAdapter
    ├── util/           → MessageUtil
    └── web/            → StockController, GlobalExceptionHandler, DTOs, Mapper
```

**Decisão de design:** a camada de domínio e aplicação não conhece nada de Spring, JPA ou Kafka — toda a infraestrutura é injetada via interfaces (ports). Isso facilita testes unitários e troca de tecnologia.

---

## Pré-requisitos

- Java 17+
- Docker e Docker Compose
- Maven 3.8+

---

## Como rodar

### 1. Subir a infraestrutura

```bash
docker-compose up -d postgres redis zookeeper kafka
```

### 2. Rodar a aplicação

```bash
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

Swagger UI disponível em: `http://localhost:8080/swagger-ui.html`

### 3. Rodar via Docker (imagem completa)

```bash
# Build da imagem
docker build -t gubee-stock-reconciliation:latest .

# Rodar com a infra do docker-compose
docker-compose up
```

---

## Endpoints

### POST /events
Recebe um evento de estoque para processamento.

```json
{
  "eventId": "evt-001",
  "type": "STOCK_ADJUSTED",
  "occurredAt": "2026-01-01T10:00:00Z",
  "accountId": "account-001",
  "sku": "ABC-123",
  "available": 10,
  "reason": "carga_inicial"
}
```

**Tipos de evento suportados:**

| Tipo | Descrição |
|---|---|
| `STOCK_ADJUSTED` | Ajuste direto de estoque |
| `ORDER_CREATED` | Novo pedido — reduz estoque |
| `ORDER_CANCELLED` | Cancelamento — devolve estoque |
| `MARKETPLACE_STOCK_RESTORED` | Recomposição automática pelo marketplace |
| `STOCK_SYNC_SENT` | Informacional — não altera saldo |

**Status de retorno:**

| Status | Descrição |
|---|---|
| `PROCESSED` | Evento aplicado com sucesso |
| `IGNORED` | EventId duplicado |
| `PENDING` | Cancelamento chegou antes do pedido |
| `INCONSISTENT` | Cancelamento duplicado ou estoque insuficiente |

### GET /stocks/{accountId}/{sku}
Retorna o saldo atual de estoque.

### GET /stocks/{accountId}/{sku}/history?page=0&size=20
Retorna o histórico paginado de eventos aplicados.

### GET /inconsistencies?page=0&size=20
Lista eventos com status `INCONSISTENT`.

### GET /events?status=PENDING&page=0&size=20
Lista eventos por status.

---

## Cenários de negócio

Todos os cenários abaixo foram validados manualmente via Swagger e automaticamente via testes de integração:

| # | Cenário | Resultado |
|---|---|---|
| 1 | `STOCK_ADJUSTED available=10` | Estoque = 10 |
| 2 | `ORDER_CREATED quantity=2` | Estoque = 8 |
| 3 | `ORDER_CANCELLED quantity=2` | Estoque = 10 |
| 4 | EventId duplicado | `IGNORED` |
| 5 | Cancelamento duplicado | `INCONSISTENT` |
| 6 | `ORDER_CANCELLED` antes de `ORDER_CREATED` | `PENDING` → reprocessado automaticamente |
| 7 | 5 pedidos simultâneos com estoque=3 | Nunca negativo (Redis lock + `@Version`) |
| 8 | `MARKETPLACE_STOCK_RESTORED` + `ORDER_CANCELLED` | `INCONSISTENT` |

---

## Testes

### Testes de integração (Testcontainers)

Sobem PostgreSQL, Redis e Kafka em containers Docker automaticamente:

```bash
./mvnw test
```

8 cenários cobertos — todos os cenários de negócio do desafio.

### Testes e2e (Postman)

Collection disponível em `postman/gubee-stock-reconciliation.postman_collection.json`.

Para importar: Postman → Import → seleciona o arquivo.

Para rodar todos: botão direito na collection → **Run collection**.

---

## i18n

A API suporta respostas em português e inglês via header `Accept-Language`:

```bash
# Português (padrão)
curl -H "Accept-Language: pt-BR" http://localhost:8080/events ...
# → "Evento aplicado com sucesso"

# Inglês
curl -H "Accept-Language: en" http://localhost:8080/events ...
# → "Event applied successfully"
```

---

## Docker

O Dockerfile usa **multi-stage build**:

- **Stage 1 (build):** JDK completo — compila e gera o `.jar`
- **Stage 2 (runtime):** JRE mínimo — copia só o `.jar`, roda como usuário não-root

Tamanho final da imagem: **~274MB** (vs ~600MB+ sem multi-stage).

```bash
docker build -t gubee-stock-reconciliation:latest .
docker run -p 8080:8080 gubee-stock-reconciliation:latest
```

---

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/stock_reconciliation` | URL do banco |
| `SPRING_DATASOURCE_USERNAME` | `gubee` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | `gubee` | Senha do banco |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Endereço do Kafka |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Host do Redis |
| `SPRING_DATA_REDIS_PORT` | `6379` | Porta do Redis |
| `JAVA_OPTS` | `-Xms256m -Xmx512m` | Opções da JVM |