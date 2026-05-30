package com.gubee.stock.application.port.out;

import com.gubee.stock.domain.model.StockBalance;

import java.util.Optional;

public interface StockBalanceRepository {
    Optional<StockBalance> findByAccountIdAndSku(String accountId, String sku);

    StockBalance save(StockBalance balance);
}