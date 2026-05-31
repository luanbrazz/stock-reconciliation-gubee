package com.gubee.stock.infrastructure.web.controller;

import com.gubee.stock.application.port.in.ProcessEventUseCase;
import com.gubee.stock.application.port.in.QueryInconsistenciesUseCase;
import com.gubee.stock.application.port.in.QueryStockUseCase;
import com.gubee.stock.application.port.out.EventPublisherPort;
import com.gubee.stock.application.port.out.ProcessedEventSummary;
import com.gubee.stock.domain.model.EventProcessingStatus;
import com.gubee.stock.infrastructure.util.MessageUtil;
import com.gubee.stock.infrastructure.web.dto.EventRequest;
import com.gubee.stock.infrastructure.web.dto.EventResponse;
import com.gubee.stock.infrastructure.web.dto.HistoryResponse;
import com.gubee.stock.infrastructure.web.dto.StockResponse;
import com.gubee.stock.infrastructure.web.mapper.StockWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Stock Reconciliation")
public class StockController {

    private final ProcessEventUseCase processEventUseCase;
    private final QueryStockUseCase queryStockUseCase;
    private final QueryInconsistenciesUseCase queryInconsistenciesUseCase;
    private final EventPublisherPort eventPublisherPort;
    private final StockWebMapper mapper;
    private final MessageUtil messageUtil;

    @PostMapping("/events")
    @Operation(summary = "Submit a stock event")
    public ResponseEntity<EventResponse> receiveEvent(
            @Valid @RequestBody EventRequest request) {

        var event = mapper.toDomain(request);
        var status = processEventUseCase.process(event);

        String messageKey = switch (status) {
            case PROCESSED -> "event.status.processed";
            case IGNORED -> "event.status.ignored";
            case PENDING -> "event.status.pending";
            case INCONSISTENT -> "event.status.inconsistent";
        };

        return ResponseEntity.ok(
                new EventResponse(request.eventId(), status, messageUtil.get(messageKey)));
    }

    @GetMapping("/stocks/{accountId}/{sku}")
    @Operation(summary = "Get current stock balance")
    public ResponseEntity<StockResponse> getStock(
            @PathVariable String accountId,
            @PathVariable String sku) {
        return ResponseEntity.ok(
                mapper.toResponse(queryStockUseCase.getCurrentStock(accountId, sku)));
    }

    @GetMapping("/stocks/{accountId}/{sku}/history")
    @Operation(summary = "Get stock history")
    public ResponseEntity<Page<HistoryResponse>> getHistory(
            @PathVariable String accountId,
            @PathVariable String sku,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(
                queryStockUseCase.getHistory(accountId, sku, pageable)
                        .map(mapper::toResponse));
    }

    @GetMapping("/events")
    @Operation(summary = "List events by status")
    public ResponseEntity<Page<ProcessedEventSummary>> listEventsByStatus(
            @RequestParam(defaultValue = "PENDING") String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                queryInconsistenciesUseCase.findByStatus(status, pageable));
    }

    @GetMapping("/inconsistencies")
    @Operation(summary = "List inconsistent events")
    public ResponseEntity<Page<ProcessedEventSummary>> listInconsistencies(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                queryInconsistenciesUseCase.findByStatus(
                        EventProcessingStatus.INCONSISTENT.name(), pageable));
    }
}