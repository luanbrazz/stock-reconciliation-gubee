package com.gubee.stock.application.port.out;

import com.gubee.stock.domain.model.EventProcessingStatus;
import com.gubee.stock.domain.model.StockEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProcessedEventRepository {
    boolean existsByEventId(String eventId);

    void save(String eventId, StockEventType type, EventProcessingStatus status, String accountId, String sku,
              String marketplace, String externalOrderId, Integer quantity, String notes);

    boolean existsCancellationFor(String marketplace, String accountId, String externalOrderId, String sku);


    void deletePendingByEventId(String eventId);

    List<ProcessedEventSummary> findByStatus(EventProcessingStatus status);

    Page<ProcessedEventSummary> findByStatus(EventProcessingStatus status, Pageable pageable);
}