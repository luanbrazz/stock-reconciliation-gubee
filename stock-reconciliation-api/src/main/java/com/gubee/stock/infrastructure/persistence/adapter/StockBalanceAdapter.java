package com.gubee.stock.infrastructure.persistence.adapter;

import com.gubee.stock.application.port.out.StockBalanceRepository;
import com.gubee.stock.domain.model.StockBalance;
import com.gubee.stock.infrastructure.persistence.entity.StockBalanceEntity;
import com.gubee.stock.infrastructure.persistence.repository.JpaStockBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StockBalanceAdapter implements StockBalanceRepository {

    private final JpaStockBalanceRepository jpaRepository;

    @Override
    public Optional<StockBalance> findByAccountIdAndSku(String accountId, String sku) {
        return jpaRepository.findByAccountIdAndSku(accountId, sku)
                .map(e -> new StockBalance(
                        e.getAccountId(), e.getSku(),
                        e.getQuantity(), e.getLastUpdatedAt()));
    }

    @Override
    public StockBalance save(StockBalance balance) {
        StockBalanceEntity entity = jpaRepository
                .findByAccountIdAndSku(balance.getAccountId(), balance.getSku())
                .orElse(new StockBalanceEntity());

        entity.setAccountId(balance.getAccountId());
        entity.setSku(balance.getSku());
        entity.setQuantity(balance.getQuantity());
        entity.setLastUpdatedAt(balance.getLastUpdatedAt());

        StockBalanceEntity saved = jpaRepository.save(entity);

        return new StockBalance(
                saved.getAccountId(), saved.getSku(),
                saved.getQuantity(), saved.getLastUpdatedAt());
    }
}