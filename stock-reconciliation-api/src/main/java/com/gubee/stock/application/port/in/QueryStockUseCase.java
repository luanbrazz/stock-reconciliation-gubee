package com.gubee.stock.application.port.in;

import com.gubee.stock.domain.model.StockBalance;
import com.gubee.stock.domain.model.StockHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QueryStockUseCase {
    StockBalance getCurrentStock(String accountId, String sku);

    Page<StockHistory> getHistory(String accountId, String sku, Pageable pageable);
}