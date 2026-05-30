package com.gubee.stock.infrastructure.persistence.repository;

import com.gubee.stock.infrastructure.persistence.entity.StockBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaStockBalanceRepository extends JpaRepository<StockBalanceEntity, Long> {
    Optional<StockBalanceEntity> findByAccountIdAndSku(String accountId, String sku);
}