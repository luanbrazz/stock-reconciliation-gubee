package com.gubee.stock.domain.exception;

public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(String accountId, String sku,
                                      int current, int requested) {
        super("error.insufficient.stock", accountId, sku, current, requested);
    }
}