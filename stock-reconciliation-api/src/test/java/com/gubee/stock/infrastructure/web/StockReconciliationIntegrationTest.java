package com.gubee.stock.infrastructure.web;

import com.gubee.stock.BaseIntegrationTest;
import com.gubee.stock.application.port.out.StockBalanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StockReconciliationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StockBalanceRepository balanceRepository;

    @Test
    @DisplayName("Cenário 1: STOCK_ADJUSTED available=10 → estoque atual = 10")
    void scenario1_stockAdjustment() {
        // Arrange
        String eventJson = """
                    {
                      "eventId": "test-s1-evt-001",
                      "type": "STOCK_ADJUSTED",
                      "occurredAt": "2026-05-31T10:00:00Z",
                      "accountId": "test-account-s1",
                      "sku": "SKU-S1",
                      "available": 10,
                      "reason": "initial_load"
                    }
                """;

        // Act
        ResponseEntity<Map> response = postEvent(eventJson);

        // Assert — resposta HTTP
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("PROCESSED");

        // Assert — banco de dados
        var balance = balanceRepository
                .findByAccountIdAndSku("test-account-s1", "SKU-S1");

        assertThat(balance).isPresent();
        assertThat(balance.get().getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("Cenário 2: ORDER_CREATED quantity=2 reduz estoque de 10 para 8")
    void scenario2_orderReducesStock() {
        // Arrange
        postEvent("""
                    {"eventId":"s2-adj","type":"STOCK_ADJUSTED","occurredAt":"2026-01-01T10:00:00Z",
                     "accountId":"acc-s2","sku":"SKU-S2","available":10,"reason":"init"}
                """);

        // Act
        ResponseEntity<Map> response = postEvent("""
                    {"eventId":"s2-order","type":"ORDER_CREATED","occurredAt":"2026-01-01T10:01:00Z",
                     "marketplace":"MERCADO_LIVRE","accountId":"acc-s2","externalOrderId":"ML-S2",
                     "sku":"SKU-S2","quantity":2}
                """);

        // Assert
        assertThat(response.getBody().get("status")).isEqualTo("PROCESSED");
        assertThat(balanceRepository.findByAccountIdAndSku("acc-s2", "SKU-S2")
                .get().getQuantity()).isEqualTo(8);
    }

    @Test
    @DisplayName("Cenário 3: ORDER_CANCELLED devolve estoque ao valor correto")
    void scenario3_cancellationRestoresStock() {
        postEvent("""
                    {"eventId":"s3-adj","type":"STOCK_ADJUSTED","occurredAt":"2026-01-01T10:00:00Z",
                     "accountId":"acc-s3","sku":"SKU-S3","available":10,"reason":"init"}
                """);
        postEvent("""
                    {"eventId":"s3-order","type":"ORDER_CREATED","occurredAt":"2026-01-01T10:01:00Z",
                     "marketplace":"MERCADO_LIVRE","accountId":"acc-s3","externalOrderId":"ML-S3",
                     "sku":"SKU-S3","quantity":2}
                """);

        // Act
        postEvent("""
                    {"eventId":"s3-cancel","type":"ORDER_CANCELLED","occurredAt":"2026-01-01T10:05:00Z",
                     "marketplace":"MERCADO_LIVRE","accountId":"acc-s3","externalOrderId":"ML-S3",
                     "sku":"SKU-S3","quantity":2}
                """);

        // Assert — estoque voltou para 10
        assertThat(balanceRepository.findByAccountIdAndSku("acc-s3", "SKU-S3")
                .get().getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("Cenário 4: Mesmo eventId recebido duas vezes retorna IGNORED")
    void scenario4_duplicateEventIdIsIgnored() {
        String event = """
                    {"eventId":"s4-dup","type":"STOCK_ADJUSTED","occurredAt":"2026-01-01T10:00:00Z",
                     "accountId":"acc-s4","sku":"SKU-S4","available":10,"reason":"init"}
                """;

        // Act
        var first = postEvent(event);
        var second = postEvent(event);

        // Assert
        assertThat(first.getBody().get("status")).isEqualTo("PROCESSED");
        assertThat(second.getBody().get("status")).isEqualTo("IGNORED");
        assertThat(balanceRepository.findByAccountIdAndSku("acc-s4", "SKU-S4")
                .get().getQuantity()).isEqualTo(10); // não duplicou
    }

    @Test
    @DisplayName("Cenário 5: Cancelamento duplicado retorna INCONSISTENT")
    void scenario5_duplicateCancellationIsInconsistent() {
        postEvent("""
                    {"eventId":"s5-adj","type":"STOCK_ADJUSTED","occurredAt":"2026-01-01T10:00:00Z",
                     "accountId":"acc-s5","sku":"SKU-S5","available":10,"reason":"init"}
                """);
        postEvent("""
                    {"eventId":"s5-order","type":"ORDER_CREATED","occurredAt":"2026-01-01T10:01:00Z",
                     "marketplace":"MERCADO_LIVRE","accountId":"acc-s5","externalOrderId":"ML-S5",
                     "sku":"SKU-S5","quantity":2}
                """);
        postEvent("""
                    {"eventId":"s5-cancel1","type":"ORDER_CANCELLED","occurredAt":"2026-01-01T10:05:00Z",
                     "marketplace":"MERCADO_LIVRE","accountId":"acc-s5","externalOrderId":"ML-S5",
                     "sku":"SKU-S5","quantity":2}
                """);

        // Act — segundo cancelamento do mesmo pedido
        var duplicate = postEvent("""
                    {"eventId":"s5-cancel2","type":"ORDER_CANCELLED","occurredAt":"2026-01-01T10:06:00Z",
                     "marketplace":"MERCADO_LIVRE","accountId":"acc-s5","externalOrderId":"ML-S5",
                     "sku":"SKU-S5","quantity":2}
                """);

        // Assert
        assertThat(duplicate.getBody().get("status")).isEqualTo("INCONSISTENT");
        assertThat(balanceRepository.findByAccountIdAndSku("acc-s5", "SKU-S5")
                .get().getQuantity()).isEqualTo(10); // não voltou mais que devia
    }

    @Test
    @DisplayName("Cenário 6: ORDER_CANCELLED antes de ORDER_CREATED fica PENDING e é reprocessado")
    void scenario6_outOfOrderCancellationIsPendingAndReprocessed() {
        postEvent("""
                    {"eventId":"s6-adj","type":"STOCK_ADJUSTED","occurredAt":"2026-01-01T09:59:00Z",
                     "accountId":"acc-s6","sku":"SKU-S6","available":10,"reason":"init"}
                """);

        // Act — cancelamento chega antes do pedido
        var cancelResult = postEvent("""
                    {"eventId":"s6-cancel","type":"ORDER_CANCELLED","occurredAt":"2026-01-01T10:05:00Z",
                     "marketplace":"MERCADO_LIVRE","accountId":"acc-s6","externalOrderId":"ML-S6",
                     "sku":"SKU-S6","quantity":2}
                """);

        assertThat(cancelResult.getBody().get("status")).isEqualTo("PENDING");

        // ORDER_CREATED chega depois — deve acionar o reprocessamento
        postEvent("""
                    {"eventId":"s6-order","type":"ORDER_CREATED","occurredAt":"2026-01-01T10:00:00Z",
                     "marketplace":"MERCADO_LIVRE","accountId":"acc-s6","externalOrderId":"ML-S6",
                     "sku":"SKU-S6","quantity":2}
                """);

        // Assert — adj=10, -2 (order), +2 (cancel reprocessado) = 10
        assertThat(balanceRepository.findByAccountIdAndSku("acc-s6", "SKU-S6")
                .get().getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("Cenário 7: Pedidos simultâneos não geram estoque negativo")
    void scenario7_concurrentOrdersDoNotCauseNegativeStock() throws InterruptedException {
        postEvent("""
                    {"eventId":"s7-adj","type":"STOCK_ADJUSTED","occurredAt":"2026-01-01T10:00:00Z",
                     "accountId":"acc-s7","sku":"SKU-S7","available":3,"reason":"init"}
                """);

        int threads = 5;
        var latch = new java.util.concurrent.CountDownLatch(threads);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    postEvent(String.format(
                            """
                                    {"eventId":"s7-order-%d","type":"ORDER_CREATED",
                                     "occurredAt":"2026-01-01T10:01:00Z","marketplace":"MERCADO_LIVRE",
                                     "accountId":"acc-s7","externalOrderId":"ML-S7-%d",
                                     "sku":"SKU-S7","quantity":1}
                                    """, idx, idx));
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        // Assert — nunca negativo
        int qty = balanceRepository.findByAccountIdAndSku("acc-s7", "SKU-S7")
                .get().getQuantity();
        assertThat(qty).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Cenário 8: MARKETPLACE_STOCK_RESTORED seguido de ORDER_CANCELLED não duplica recomposição")
    void scenario8_marketplaceRestorationPlusCancel() {
        postEvent("""
                    {"eventId":"s8-adj","type":"STOCK_ADJUSTED","occurredAt":"2026-01-01T10:00:00Z",
                     "accountId":"acc-s8","sku":"SKU-S8","available":10,"reason":"init"}
                """);
        postEvent("""
                    {"eventId":"s8-order","type":"ORDER_CREATED","occurredAt":"2026-01-01T10:01:00Z",
                     "marketplace":"MERCADO_LIVRE","accountId":"acc-s8","externalOrderId":"ML-S8",
                     "sku":"SKU-S8","quantity":2}
                """);

        // Marketplace restaura automaticamente
        postEvent("""
                    {"eventId":"s8-restore","type":"MARKETPLACE_STOCK_RESTORED",
                     "occurredAt":"2026-01-01T10:10:00Z","marketplace":"MERCADO_LIVRE",
                     "accountId":"acc-s8","externalOrderId":"ML-S8","sku":"SKU-S8","quantity":2}
                """);

        // Act — cancelamento chega depois do restore — deve ser INCONSISTENT
        var cancelResult = postEvent("""
                    {"eventId":"s8-cancel","type":"ORDER_CANCELLED","occurredAt":"2026-01-01T10:15:00Z",
                     "marketplace":"MERCADO_LIVRE","accountId":"acc-s8","externalOrderId":"ML-S8",
                     "sku":"SKU-S8","quantity":2}
                """);

        // Assert — restore já recompôs, cancel é duplicata de recomposição
        assertThat(cancelResult.getBody().get("status")).isEqualTo("INCONSISTENT");
        assertThat(balanceRepository.findByAccountIdAndSku("acc-s8", "SKU-S8")
                .get().getQuantity()).isEqualTo(10);
    }

    // Método auxiliar — evita repetição de código nos testes
    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> postEvent(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(json.trim(), headers);
        return restTemplate.postForEntity("/events", entity, Map.class);
    }
}