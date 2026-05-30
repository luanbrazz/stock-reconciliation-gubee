package com.gubee.stock.application.port.out;

public interface StockLockPort {
    String acquire(String accountId, String sku);

    void release(String accountId, String sku, String lockToken);
}