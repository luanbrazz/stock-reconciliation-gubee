package com.gubee.stock.infrastructure.persistence.adapter;

import com.gubee.stock.application.port.out.ProcessedEventRepository;
import com.gubee.stock.application.port.out.ProcessedEventSummary;
import com.gubee.stock.domain.model.EventProcessingStatus;
import com.gubee.stock.domain.model.StockEventType;
import com.gubee.stock.infrastructure.persistence.entity.ProcessedEventEntity;
import com.gubee.stock.infrastructure.persistence.repository.JpaProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProcessedEventAdapter implements ProcessedEventRepository {

    private final JpaProcessedEventRepository jpaRepository;

    @Override
    public boolean existsByEventId(String eventId) {
        return jpaRepository.existsByEventId(eventId);
    }

    @Override
    public void save(String eventId, StockEventType type, EventProcessingStatus status,
                     String accountId, String sku, String marketplace,
                     String externalOrderId, Integer quantity, String notes) {
        jpaRepository.save(ProcessedEventEntity.builder()
                .eventId(eventId)
                .type(type)
                .status(status)
                .accountId(accountId)
                .sku(sku)
                .marketplace(marketplace)
                .externalOrderId(externalOrderId)
                .quantity(quantity)
                .occurredAt(Instant.now())
                .processedAt(Instant.now())
                .notes(notes)
                .build());
    }

    @Override
    public boolean existsCancellationFor(String marketplace, String accountId,
                                         String externalOrderId, String sku) {
        return jpaRepository.existsByTypesAndBusinessKey(
                List.of(StockEventType.ORDER_CANCELLED,
                        StockEventType.MARKETPLACE_STOCK_RESTORED),
                marketplace, accountId, externalOrderId, sku);
    }

    @Override
    public List<ProcessedEventSummary> findByStatus(EventProcessingStatus status) {
        return jpaRepository.findByStatus(status)
                .stream()
                .map(e -> new ProcessedEventSummary(
                        e.getEventId(), e.getType(), e.getStatus(),
                        e.getAccountId(), e.getSku(), e.getMarketplace(),
                        e.getExternalOrderId(), e.getQuantity(), e.getOccurredAt()))
                .toList();
    }

    @Override
    public void deletePendingByEventId(String eventId) {
        jpaRepository.deletePendingByEventId(eventId);
    }
}