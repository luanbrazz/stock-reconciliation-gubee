package com.gubee.stock.infrastructure.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gubee.stock.application.port.in.ProcessEventUseCase;
import com.gubee.stock.domain.model.StockEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventConsumer {

    private final ProcessEventUseCase processEventUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "stock-events",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "3"
    )
    public void consume(@Payload String message,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Consumed message partition={} offset={}", partition, offset);
        try {
            StockEvent event = objectMapper.readValue(message, StockEvent.class);
            var status = processEventUseCase.process(event);
            log.info("Event processed eventId={} status={}", event.getEventId(), status);
        } catch (Exception ex) {
            log.error("Failed to process message: {} — payload={}", ex.getMessage(), message, ex);
            // Em produção: enviado para DLQ.
        }
    }
}