package com.gubee.stock.application.port.out;

import com.gubee.stock.domain.model.EventProcessingStatus;
import com.gubee.stock.domain.model.StockEventType;

import java.time.Instant;

public record ProcessedEventSummary(
        String eventId,
        StockEventType type,
        EventProcessingStatus status,
        String accountId,
        String sku,
        String marketplace,
        String externalOrderId,
        Integer quantity,
        Instant occurredAt
) {
}