package com.gubee.stock.infrastructure.web.dto;

import com.gubee.stock.domain.model.StockEventType;

import java.time.Instant;

public record HistoryResponse(
        String eventId,
        StockEventType eventType,
        int quantityBefore,
        int quantityAfter,
        int delta,
        String marketplace,
        String externalOrderId,
        String reason,
        Instant occurredAt,
        Instant processedAt
) {
}