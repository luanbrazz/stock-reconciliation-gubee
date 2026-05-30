package com.gubee.stock.infrastructure.persistence.repository;

import com.gubee.stock.infrastructure.persistence.entity.StockHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaStockHistoryRepository extends JpaRepository<StockHistoryEntity, Long> {
    List<StockHistoryEntity> findByAccountIdAndSkuOrderByOccurredAtAsc(String accountId, String sku);
}