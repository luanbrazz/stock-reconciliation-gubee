package com.gubee.stock.domain.exception;

public class StockNotFoundException extends BusinessException {

    public StockNotFoundException(String accountId, String sku) {
        super("error.stock.not.found", accountId, sku);
    }
}