package com.gubee.stock.application.port.in;

import com.gubee.stock.domain.model.StockBalance;
import com.gubee.stock.domain.model.StockHistory;

import java.util.List;

public interface QueryStockUseCase {
    StockBalance getCurrentStock(String accountId, String sku);

    List<StockHistory> getHistory(String accountId, String sku);
}