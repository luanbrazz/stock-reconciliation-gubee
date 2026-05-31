package com.gubee.stock.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventRequest(
        @NotBlank String eventId,
        @NotBlank String type,
        @NotNull Instant occurredAt,
        @NotBlank String accountId,
        @NotBlank String sku,
        String marketplace,
        String externalOrderId,
        Integer quantity,
        Integer available,
        String reason,
        Integer quantitySent
) {
}