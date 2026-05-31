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

    // Método auxiliar — evita repetição de código nos testes
    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> postEvent(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(json.trim(), headers);
        return restTemplate.postForEntity("/events", entity, Map.class);
    }
}