# Decisões Técnicas — gubee-stock-reconciliation

Este documento registra as principais decisões de design e arquitetura tomadas durante o desenvolvimento do serviço, com a justificativa de cada escolha.

---

## 1. Arquitetura Hexagonal (Ports & Adapters)

**Decisão:** organizar o projeto em três camadas — `domain`, `application` e `infrastructure` — com comunicação via interfaces (ports).

**Justificativa:**
- A camada de domínio e aplicação não possui nenhuma dependência de Spring, JPA ou Kafka. Isso significa que a lógica de negócio pode ser testada de forma unitária sem subir nenhuma infraestrutura.
- Facilita a troca de tecnologia: substituir PostgreSQL por outro banco, ou Kafka por RabbitMQ, exige mudança apenas nos adapters de infraestrutura.
- Segue o princípio de inversão de dependência — a aplicação define o contrato (port) e a infraestrutura o implementa (adapter).

---

## 2. Fonte única de verdade por accountId + SKU

**Decisão:** o estoque global é mantido por `accountId + SKU`, independente do marketplace de origem.

**Justificativa:**
- Um mesmo produto vendido no Mercado Livre e na Shopee consome do mesmo estoque físico. Manter estoques separados por marketplace causaria overselling.
- A chave `(accountId, sku)` tem constraint `UNIQUE` no banco, garantindo consistência.

---

## 3. Idempotência via tabela `processed_events`

**Decisão:** toda vez que um evento é processado, seu `eventId` é gravado na tabela `processed_events`. Eventos com `eventId` já existente retornam `IGNORED` sem reprocessar.

**Justificativa:**
- Kafka garante entrega *at-least-once* — o mesmo evento pode ser entregue mais de uma vez em caso de falha ou rebalanceamento de partições.
- A verificação de `eventId` antes de qualquer processamento garante que o sistema seja idempotente, ou seja, processar o mesmo evento N vezes tem o mesmo resultado que processar uma vez.

---

## 4. Lock distribuído com Redis + `@Version` otimista no JPA

**Decisão:** uso de duas camadas de proteção contra concorrência:
1. Redis `SET NX PX` (lock distribuído) para serializar o acesso por `accountId:sku`
2. `@Version` no JPA para detectar conflitos de escrita otimista

**Justificativa:**
- Com 3 partições Kafka e `concurrency=3` no consumer, múltiplos pedidos do mesmo SKU podem ser processados simultaneamente.
- O Redis lock garante que apenas um thread por vez aplica alterações no mesmo estoque, evitando estoque negativo.
- O `@Version` é uma segunda linha de defesa — mesmo que o lock falhe ou expire, o Hibernate detecta a versão desatualizada e lança `OptimisticLockException`.
- A combinação dos dois mecanismos garante consistência mesmo em cenários de falha.

---

## 5. Tratamento de eventos fora de ordem (PENDING)

**Decisão:** quando um `ORDER_CANCELLED` chega antes do `ORDER_CREATED` correspondente, o cancelamento é salvo com status `PENDING`. Quando o `ORDER_CREATED` chega, o sistema busca todos os `PENDING` para aquele pedido e os reprocessa automaticamente.

**Justificativa:**
- Em sistemas distribuídos com múltiplos marketplaces, a ordem de chegada dos eventos não é garantida.
- Rejeitar o cancelamento causaria perda de dados. Guardar como `PENDING` e reprocessar garante que nenhum evento seja perdido.
- O reprocessamento acontece dentro da mesma transação do `ORDER_CREATED`, garantindo atomicidade.

---

## 6. Chave de particionamento Kafka: `accountId:sku`

**Decisão:** ao publicar eventos no Kafka, a chave da mensagem é `accountId:sku`.

**Justificativa:**
- O Kafka garante ordem apenas dentro de uma mesma partição.
- Usando `accountId:sku` como chave, todos os eventos do mesmo produto vão para a mesma partição, garantindo ordem relativa de processamento.
- Produtos diferentes podem ir para partições diferentes, permitindo paralelismo.

---

## 7. MARKETPLACE_STOCK_RESTORED tratado como cancelamento

**Decisão:** `MARKETPLACE_STOCK_RESTORED` é tratado como recomposição de estoque, equivalente a `ORDER_CANCELLED` para fins de detecção de duplicidade.

**Justificativa:**
- Tanto `ORDER_CANCELLED` quanto `MARKETPLACE_STOCK_RESTORED` devolvem estoque para o mesmo pedido.
- Se o marketplace restaurou o estoque automaticamente e depois chega um `ORDER_CANCELLED` para o mesmo pedido, o segundo evento seria uma duplicata de recomposição — deve ser `INCONSISTENT`.
- A query `existsByTypesAndBusinessKey` verifica os dois tipos, evitando recomposição dupla.

---

## 8. Auditoria completa via `stock_history`

**Decisão:** cada evento aplicado gera um registro em `stock_history` com `quantityBefore`, `quantityAfter` e `delta`.

**Justificativa:**
- Permite rastrear exatamente o que aconteceu com o estoque ao longo do tempo.
- Facilita investigação de inconsistências — é possível reconstruir o histórico completo de um SKU.
- Serve como trilha de auditoria para disputas com marketplaces.

---

## 9. i18n via `Accept-Language`

**Decisão:** as mensagens de resposta da API são resolvidas com base no header `Accept-Language` da requisição, com suporte a `pt-BR` e `en`.

**Justificativa:**
- Marketplaces integrados podem ser de diferentes países.
- O uso de `LocaleContextHolder` via `MessageUtil` desacopla a resolução de locale do controller — qualquer componente pode usar `messageUtil.get(chave)` sem receber `Locale` como parâmetro.

---

## 10. Paginação nos endpoints de listagem

**Decisão:** os endpoints `/history`, `/events` e `/inconsistencies` retornam `Page<T>` com suporte a `?page=0&size=20`.

**Justificativa:**
- Em produção, um SKU com alto volume de vendas pode ter milhares de registros no histórico. Retornar tudo de uma vez sobrecarregaria o banco e a rede.
- O padrão `Spring Data Pageable` é amplamente conhecido e facilita integração com frontends e ferramentas de BI.

---

## 11. Multi-stage Dockerfile com usuário não-root

**Decisão:** o Dockerfile usa dois stages (build com JDK e runtime com JRE) e roda a aplicação com um usuário sem privilégios de root.

**Justificativa:**
- O JDK completo (~500MB) é necessário apenas para compilar. A imagem final usa apenas o JRE (~100MB), resultando em ~274MB total.
- Rodar como root em um container é uma vulnerabilidade de segurança. O usuário `gubee` limita o impacto de uma eventual exploração do container.

---

## 12. Testcontainers para testes de integração

**Decisão:** os testes de integração sobem PostgreSQL, Redis e Kafka reais via Testcontainers, em vez de usar H2 em memória ou mocks.

**Justificativa:**
- H2 não se comporta exatamente igual ao PostgreSQL — queries com `@Version`, índices específicos e constraints podem ter comportamentos diferentes.
- Testcontainers garante que os testes rodam contra a mesma tecnologia que o ambiente de produção.
- Os containers são gerenciados automaticamente pelo JUnit — sobem antes dos testes e são destruídos ao final, sem necessidade de infraestrutura externa no CI/CD.

---

## 13. Comportamento com múltiplas instâncias

**Decisão:** a solução foi projetada para funcionar corretamente com múltiplas instâncias rodando em paralelo.

**Como funciona:**
- O **Redis distributed lock** (`SET NX PX`) é compartilhado entre todas as instâncias — apenas uma instância por vez processa eventos do mesmo `accountId:sku`, independente de quantas instâncias existam.
- O **`@Version` otimista** no JPA é uma segunda camada — se duas instâncias passarem pelo lock ao mesmo tempo (ex: lock expirou), o Hibernate detecta a versão desatualizada e lança `OptimisticLockException`, evitando corrupção de dados.
- O **Kafka com chave `accountId:sku`** garante que eventos do mesmo produto sempre vão para a mesma partição — e cada partição é consumida por apenas um consumer de cada vez, o que reduz naturalmente a concorrência entre instâncias.
- A tabela `processed_events` com `event_id` como chave primária garante idempotência mesmo com múltiplas instâncias tentando inserir o mesmo evento simultaneamente — o banco retornará erro de constraint violada na segunda tentativa.

---

## 14. Trade-offs assumidos

**Redis lock com TTL fixo de 10 segundos:**
- Se uma transação demorar mais de 10 segundos, o lock expira e outra instância pode entrar. O `@Version` compensa esse cenário, mas a transação original vai falhar com `OptimisticLockException`. Em produção, o TTL deveria ser configurável e monitorado.

**Reprocessamento de PENDING em memória:**
- Os eventos `PENDING` são buscados e reprocessados dentro da mesma transação do `ORDER_CREATED`. Se existirem muitos eventos `PENDING` para o mesmo pedido (situação anormal), isso pode tornar a transação lenta. Em produção, isso poderia ser movido para um job assíncrono.

**Fonte da verdade única por `accountId + SKU`:**
- A decisão de não separar estoque por marketplace simplifica o modelo, mas significa que um cancelamento no Mercado Livre devolve estoque que também está disponível para a Shopee. Em produção, dependendo do modelo de negócio do cliente, pode ser necessário controle por marketplace.

**Sem DLQ (Dead Letter Queue):**
- Eventos que falham no consumer Kafka são apenas logados. Em produção, mensagens com falha deveriam ir para uma DLQ para reprocessamento manual ou automático.

---

## 15. O que foi simplificado por causa do prazo

- **Sem autenticação/autorização:** os endpoints são públicos. Em produção, seria necessário JWT ou API Key para proteger a API.
- **Sem DLQ no Kafka:** falhas no consumer são apenas logadas, sem mecanismo de reprocessamento automático.
- **Sem métricas e observabilidade:** não foram implementados Prometheus, Grafana ou Kibana. Em produção, seria essencial monitorar latência, throughput e taxa de inconsistências.
- **Sem rate limiting:** a API não possui proteção contra abuso. Em produção, seria necessário limitar requisições por `accountId`.
- **Testes unitários do `ProcessEventService`:** apenas testes de integração foram implementados. Testes unitários com Mockito aumentariam a velocidade do feedback no desenvolvimento.
- **Reprocessamento de PENDING síncrono:** o reprocessamento acontece dentro da transação do `ORDER_CREATED`. Uma abordagem mais robusta seria um job agendado que varre eventos `PENDING` periodicamente.

---

## 16. O que faria diferente em produção

- **DLQ para eventos com falha:** mensagens que falham repetidamente no Kafka seriam enviadas para uma Dead Letter Queue, com alertas para a equipe de operações.
- **Observabilidade completa:** Prometheus + Grafana para métricas de negócio (taxa de inconsistências por marketplace, latência de processamento, volume de eventos por tipo) e Kibana para logs estruturados e rastreamento de eventos.
- **TTL do lock configurável por ambiente:** o Redis lock TTL seria uma propriedade externa (`application.yaml`) para ajuste fino por ambiente.
- **Separação de estoque por marketplace (opcional):** dependendo do modelo de negócio, o controle por `accountId + marketplace + sku` poderia evitar que cancelamentos de um marketplace impactem a disponibilidade em outros.
- **Job de reprocessamento de PENDING:** um scheduler periódico (ex: a cada 5 minutos) que tenta reprocessar eventos `PENDING` antigos, com limite de tentativas e escalada para `INCONSISTENT` após N falhas.
- **Autenticação via API Key por accountId:** cada seller teria sua própria API Key, limitando acesso apenas aos seus próprios dados.
- **Versionamento de API:** prefixo `/v1/` nos endpoints para permitir evoluções sem quebrar clientes existentes.
- **Testes de carga:** validar o comportamento do sistema com volume alto de eventos simultâneos (ex: Black Friday) antes de ir para produção.