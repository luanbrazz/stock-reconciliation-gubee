package com.gubee.stock.infrastructure.persistence.adapter;

import com.gubee.stock.application.port.out.StockHistoryRepository;
import com.gubee.stock.domain.model.StockHistory;
import com.gubee.stock.infrastructure.persistence.entity.StockHistoryEntity;
import com.gubee.stock.infrastructure.persistence.repository.JpaStockHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockHistoryAdapter implements StockHistoryRepository {

    private final JpaStockHistoryRepository jpaRepository;

    @Override
    public void save(StockHistory history) {
        jpaRepository.save(StockHistoryEntity.builder()
                .eventId(history.getEventId())
                .accountId(history.getAccountId())
                .sku(history.getSku())
                .eventType(history.getEventType())
                .quantityBefore(history.getQuantityBefore())
                .quantityAfter(history.getQuantityAfter())
                .delta(history.getDelta())
                .marketplace(history.getMarketplace())
                .externalOrderId(history.getExternalOrderId())
                .reason(history.getReason())
                .occurredAt(history.getOccurredAt())
                .processedAt(history.getProcessedAt())
                .build());
    }

    @Override
    public Page<StockHistory> findByAccountIdAndSku(String accountId, String sku, Pageable pageable) {
        return jpaRepository
                .findByAccountIdAndSkuOrderByOccurredAtDesc(accountId, sku, pageable)
                .map(e -> StockHistory.builder()
                        .eventId(e.getEventId())
                        .accountId(e.getAccountId())
                        .sku(e.getSku())
                        .eventType(e.getEventType())
                        .quantityBefore(e.getQuantityBefore())
                        .quantityAfter(e.getQuantityAfter())
                        .delta(e.getDelta())
                        .marketplace(e.getMarketplace())
                        .externalOrderId(e.getExternalOrderId())
                        .reason(e.getReason())
                        .occurredAt(e.getOccurredAt())
                        .processedAt(e.getProcessedAt())
                        .build());
    }
}