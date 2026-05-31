package com.gubee.stock.application.port.out;

import com.gubee.stock.domain.model.StockHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockHistoryRepository {
    void save(StockHistory history);

    Page<StockHistory> findByAccountIdAndSku(String accountId, String sku, Pageable pageable);
}