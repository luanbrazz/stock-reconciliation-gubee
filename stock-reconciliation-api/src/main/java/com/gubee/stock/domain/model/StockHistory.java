package com.gubee.stock.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class StockHistory {

    private final String eventId;
    private final String accountId;
    private final String sku;
    private final StockEventType eventType;
    private final int quantityBefore;
    private final int quantityAfter;
    private final int delta;
    private final String marketplace;
    private final String externalOrderId;
    private final String reason;
    private final Instant occurredAt;
    private final Instant processedAt;
}