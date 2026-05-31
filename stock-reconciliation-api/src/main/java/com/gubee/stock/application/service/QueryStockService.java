package com.gubee.stock.application.service;

import com.gubee.stock.application.port.in.QueryStockUseCase;
import com.gubee.stock.application.port.out.StockBalanceRepository;
import com.gubee.stock.application.port.out.StockHistoryRepository;
import com.gubee.stock.domain.exception.StockNotFoundException;
import com.gubee.stock.domain.model.StockBalance;
import com.gubee.stock.domain.model.StockHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueryStockService implements QueryStockUseCase {

    private final StockBalanceRepository balanceRepository;
    private final StockHistoryRepository historyRepository;

    @Override
    @Transactional(readOnly = true)
    public StockBalance getCurrentStock(String accountId, String sku) {
        return balanceRepository.findByAccountIdAndSku(accountId, sku)
                .orElseThrow(() -> new StockNotFoundException(accountId, sku));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockHistory> getHistory(String accountId, String sku, Pageable pageable) {
        getCurrentStock(accountId, sku);
        return historyRepository.findByAccountIdAndSku(accountId, sku, pageable);
    }
}