package com.gubee.stock.infrastructure.web.mapper;

import com.gubee.stock.domain.model.StockBalance;
import com.gubee.stock.domain.model.StockEvent;
import com.gubee.stock.domain.model.StockEventType;
import com.gubee.stock.domain.model.StockHistory;
import com.gubee.stock.infrastructure.web.dto.EventRequest;
import com.gubee.stock.infrastructure.web.dto.HistoryResponse;
import com.gubee.stock.infrastructure.web.dto.StockResponse;
import org.springframework.stereotype.Component;

@Component
public class StockWebMapper {

    public StockEvent toDomain(EventRequest request) {
        return StockEvent.builder()
                .eventId(request.eventId())
                .type(StockEventType.valueOf(request.type()))
                .occurredAt(request.occurredAt())
                .accountId(request.accountId())
                .sku(request.sku())
                .marketplace(request.marketplace())
                .externalOrderId(request.externalOrderId())
                .quantity(request.quantity())
                .available(request.available())
                .reason(request.reason())
                .quantitySent(request.quantitySent())
                .build();
    }

    public StockResponse toResponse(StockBalance balance) {
        return new StockResponse(
                balance.getAccountId(),
                balance.getSku(),
                balance.getQuantity(),
                balance.getLastUpdatedAt());
    }

    public HistoryResponse toResponse(StockHistory history) {
        return new HistoryResponse(
                history.getEventId(),
                history.getEventType(),
                history.getQuantityBefore(),
                history.getQuantityAfter(),
                history.getDelta(),
                history.getMarketplace(),
                history.getExternalOrderId(),
                history.getReason(),
                history.getOccurredAt(),
                history.getProcessedAt());
    }
}