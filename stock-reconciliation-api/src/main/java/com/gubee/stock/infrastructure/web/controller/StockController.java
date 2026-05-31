package com.gubee.stock.infrastructure.web.controller;

import com.gubee.stock.application.port.in.ProcessEventUseCase;
import com.gubee.stock.application.port.in.QueryInconsistenciesUseCase;
import com.gubee.stock.application.port.in.QueryStockUseCase;
import com.gubee.stock.application.port.out.EventPublisherPort;
import com.gubee.stock.domain.model.EventProcessingStatus;
import com.gubee.stock.infrastructure.web.dto.EventRequest;
import com.gubee.stock.infrastructure.web.dto.EventResponse;
import com.gubee.stock.infrastructure.web.dto.HistoryResponse;
import com.gubee.stock.infrastructure.web.dto.StockResponse;
import com.gubee.stock.infrastructure.web.mapper.StockWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Stock Reconciliation")
public class StockController {

    private final ProcessEventUseCase processEventUseCase;
    private final QueryStockUseCase queryStockUseCase;
    private final QueryInconsistenciesUseCase queryInconsistenciesUseCase;
    private final EventPublisherPort eventPublisherPort;
    private final StockWebMapper mapper;

    @PostMapping("/events")
    @Operation(summary = "Submit a stock event")
    public ResponseEntity<EventResponse> receiveEvent(
            @Valid @RequestBody EventRequest request) {

        var event = mapper.toDomain(request);
        var status = processEventUseCase.process(event);

        String message = switch (status) {
            case PROCESSED -> "Event applied successfully";
            case IGNORED -> "Duplicate eventId — event ignored";
            case PENDING -> "Event pending: out-of-order, waiting for dependency";
            case INCONSISTENT -> "Event flagged as inconsistent — see /inconsistencies";
        };

        return ResponseEntity.ok(new EventResponse(request.eventId(), status, message));
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
    public ResponseEntity<List<HistoryResponse>> getHistory(
            @PathVariable String accountId,
            @PathVariable String sku) {
        var history = queryStockUseCase.getHistory(accountId, sku)
                .stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/events")
    @Operation(summary = "List events by status")
    public ResponseEntity<?> listEventsByStatus(
            @RequestParam(defaultValue = "PENDING") String status) {
        return ResponseEntity.ok(
                queryInconsistenciesUseCase.findByStatus(status));
    }

    @GetMapping("/inconsistencies")
    @Operation(summary = "List inconsistent events")
    public ResponseEntity<?> listInconsistencies() {
        return ResponseEntity.ok(
                queryInconsistenciesUseCase.findByStatus(
                        EventProcessingStatus.INCONSISTENT.name()));
    }
}