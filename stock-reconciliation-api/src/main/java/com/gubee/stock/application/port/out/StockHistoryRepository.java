package com.gubee.stock.application.port.out;

import com.gubee.stock.domain.model.StockHistory;

import java.util.List;

public interface StockHistoryRepository {
    void save(StockHistory history);

    List<StockHistory> findByAccountIdAndSku(String accountId, String sku);
}