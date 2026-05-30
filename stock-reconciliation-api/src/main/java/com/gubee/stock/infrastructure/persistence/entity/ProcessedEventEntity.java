package com.gubee.stock.infrastructure.persistence.entity;

import com.gubee.stock.domain.model.EventProcessingStatus;
import com.gubee.stock.domain.model.StockEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEventEntity {

    @Id
    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockEventType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventProcessingStatus status;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String sku;

    private String marketplace;

    @Column(name = "external_order_id")
    private String externalOrderId;

    private Integer quantity;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    private String notes;
}