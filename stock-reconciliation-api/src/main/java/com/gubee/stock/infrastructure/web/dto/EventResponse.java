package com.gubee.stock.infrastructure.web.dto;

import com.gubee.stock.domain.model.EventProcessingStatus;

public record EventResponse(
        String eventId,
        EventProcessingStatus status,
        String message
) {
}