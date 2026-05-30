package com.gubee.stock.domain.model;

import com.gubee.stock.domain.exception.InsufficientStockException;
import lombok.Getter;

import java.time.Instant;

@Getter
public class StockBalance {

    private final String accountId;
    private final String sku;
    private int quantity;
    private Instant lastUpdatedAt;

    public StockBalance(String accountId, String sku, int quantity, Instant lastUpdatedAt) {
        this.accountId = accountId;
        this.sku = sku;
        this.quantity = quantity;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public static StockBalance create(String accountId, String sku) {
        return new StockBalance(accountId, sku, 0, Instant.now());
    }

    public void applyAdjustment(int available, Instant occurredAt) {
        this.quantity = available;
        this.lastUpdatedAt = occurredAt;
    }

    public void reserve(int qty, Instant occurredAt) {
        if (this.quantity - qty < 0) {
            throw new InsufficientStockException(accountId, sku, this.quantity, qty);
        }
        this.quantity -= qty;
        this.lastUpdatedAt = occurredAt;
    }

    public void release(int qty, Instant occurredAt) {
        this.quantity += qty;
        this.lastUpdatedAt = occurredAt;
    }
}