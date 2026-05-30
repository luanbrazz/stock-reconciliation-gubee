package com.gubee.stock.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class StockEvent {

    private final String eventId;
    private final StockEventType type;
    private final Instant occurredAt;
    private final String accountId;
    private final String sku;
    private final String marketplace;
    private final String externalOrderId;
    private final Integer quantity;
    private final Integer available;
    private final String reason;
    private final Integer quantitySent;
}