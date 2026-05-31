package com.gubee.stock.infrastructure.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gubee.stock.application.port.out.EventPublisherPort;
import com.gubee.stock.domain.model.StockEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisherPort {

    public static final String TOPIC = "stock-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(StockEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            // Chave = accountId:sku — garante que eventos do mesmo SKU
            // vão para a mesma partição, mantendo ordem relativa
            String key = event.getAccountId() + ":" + event.getSku();
            kafkaTemplate.send(TOPIC, key, payload);
            log.info("Event published topic={} key={} eventId={}", TOPIC, key, event.getEventId());
        } catch (Exception ex) {
            log.error("Failed to publish event: {}", ex.getMessage(), ex);
            throw new RuntimeException("Kafka publish failed", ex);
        }
    }
}