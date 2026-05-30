package com.gubee.stock.application.service;

import com.gubee.stock.application.port.in.ProcessEventUseCase;
import com.gubee.stock.application.port.out.*;
import com.gubee.stock.domain.exception.InsufficientStockException;
import com.gubee.stock.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessEventService implements ProcessEventUseCase {

    private final StockBalanceRepository balanceRepository;
    private final StockHistoryRepository historyRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final StockLockPort lockPort;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public EventProcessingStatus process(StockEvent event) {

        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.warn("Duplicate eventId={} ignored", event.getEventId());
            return EventProcessingStatus.IGNORED;
        }

        if (event.getType() == StockEventType.ORDER_CANCELLED
                && event.getExternalOrderId() != null
                && processedEventRepository.existsCancellationFor(
                event.getMarketplace(), event.getAccountId(),
                event.getExternalOrderId(), event.getSku())) {
            log.warn("Duplicate cancellation for order={}", event.getExternalOrderId());
            saveProcessedEvent(event, EventProcessingStatus.INCONSISTENT,
                    "Duplicate cancellation for same order");
            return EventProcessingStatus.INCONSISTENT;
        }

        if (event.getType() == StockEventType.ORDER_CANCELLED
                && !orderCreatedExists(event)) {
            log.warn("ORDER_CANCELLED arrived before ORDER_CREATED for order={}",
                    event.getExternalOrderId());
            saveProcessedEvent(event, EventProcessingStatus.PENDING,
                    "ORDER_CANCELLED arrived before ORDER_CREATED");
            return EventProcessingStatus.PENDING;
        }

        String lockToken = lockPort.acquire(event.getAccountId(), event.getSku());
        try {
            return applyEvent(event);
        } finally {
            lockPort.release(event.getAccountId(), event.getSku(), lockToken);
        }
    }

    private EventProcessingStatus applyEvent(StockEvent event) {

        StockBalance balance = balanceRepository
                .findByAccountIdAndSku(event.getAccountId(), event.getSku())
                .orElseGet(() -> StockBalance.create(event.getAccountId(), event.getSku()));

        int before = balance.getQuantity();

        try {
            switch (event.getType()) {
                case STOCK_ADJUSTED -> balance.applyAdjustment(event.getAvailable(), event.getOccurredAt());
                case ORDER_CREATED -> balance.reserve(event.getQuantity(), event.getOccurredAt());
                case ORDER_CANCELLED, MARKETPLACE_STOCK_RESTORED ->
                        balance.release(event.getQuantity(), event.getOccurredAt());
                case STOCK_SYNC_SENT -> {
                    log.info("STOCK_SYNC_SENT recorded, balance unchanged. sent={}",
                            event.getQuantitySent());
                    saveProcessedEvent(event, EventProcessingStatus.PROCESSED, null);
                    saveHistory(event, before, before, 0);
                    return EventProcessingStatus.PROCESSED;
                }
            }
        } catch (InsufficientStockException ex) {
            log.error("Insufficient stock for eventId={}: {}", event.getEventId(),
                    ex.getMessage());
            saveProcessedEvent(event, EventProcessingStatus.INCONSISTENT, ex.getMessage());
            return EventProcessingStatus.INCONSISTENT;
        }

        int after = balance.getQuantity();

        balanceRepository.save(balance);

        saveHistory(event, before, after, after - before);

        saveProcessedEvent(event, EventProcessingStatus.PROCESSED, null);

        log.info("Event {} applied — {}/{}: {} → {}",
                event.getEventId(), event.getAccountId(), event.getSku(), before, after);

        if (event.getType() == StockEventType.ORDER_CREATED) {
            retryPendingCancellations(event);
        }

        return EventProcessingStatus.PROCESSED;
    }

    private boolean orderCreatedExists(StockEvent cancelEvent) {
        return processedEventRepository.findByStatus(EventProcessingStatus.PROCESSED)
                .stream()
                .anyMatch(summary ->
                        summary.type() == StockEventType.ORDER_CREATED
                                && summary.externalOrderId() != null
                                && summary.externalOrderId().equals(cancelEvent.getExternalOrderId())
                                && summary.accountId().equals(cancelEvent.getAccountId())
                                && summary.sku().equals(cancelEvent.getSku()));
    }

    private void retryPendingCancellations(StockEvent createdEvent) {
        processedEventRepository.findByStatus(EventProcessingStatus.PENDING)
                .stream()
                .filter(summary ->
                        summary.type() == StockEventType.ORDER_CANCELLED
                                && createdEvent.getExternalOrderId() != null
                                && createdEvent.getExternalOrderId().equals(summary.externalOrderId())
                                && createdEvent.getAccountId().equals(summary.accountId())
                                && createdEvent.getSku().equals(summary.sku()))
                .forEach(summary -> {
                    log.info("Reprocessing PENDING cancellation eventId={}", summary.eventId());
                    processedEventRepository.deletePendingByEventId(summary.eventId());
                    applyEvent(StockEvent.builder()
                            .eventId(summary.eventId())
                            .type(summary.type())
                            .occurredAt(summary.occurredAt())
                            .accountId(summary.accountId())
                            .sku(summary.sku())
                            .marketplace(summary.marketplace())
                            .externalOrderId(summary.externalOrderId())
                            .quantity(summary.quantity())
                            .build());
                });
    }

    private void saveProcessedEvent(StockEvent event, EventProcessingStatus status,
                                    String notes) {
        processedEventRepository.save(
                event.getEventId(), event.getType(), status,
                event.getAccountId(), event.getSku(), event.getMarketplace(),
                event.getExternalOrderId(),
                event.getQuantity() != null ? event.getQuantity()
                        : event.getAvailable() != null ? event.getAvailable()
                          : event.getQuantitySent(),
                notes);
    }

    private void saveHistory(StockEvent event, int before, int after, int delta) {
        historyRepository.save(StockHistory.builder()
                .eventId(event.getEventId())
                .accountId(event.getAccountId())
                .sku(event.getSku())
                .eventType(event.getType())
                .quantityBefore(before)
                .quantityAfter(after)
                .delta(delta)
                .marketplace(event.getMarketplace())
                .externalOrderId(event.getExternalOrderId())
                .reason(event.getReason())
                .occurredAt(event.getOccurredAt())
                .processedAt(Instant.now())
                .build());
    }
}