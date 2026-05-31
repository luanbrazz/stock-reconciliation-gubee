package com.gubee.stock.infrastructure.web.dto;

import java.time.Instant;

public record StockResponse(
        String accountId,
        String sku,
        int quantity,
        Instant lastUpdatedAt
) {
}