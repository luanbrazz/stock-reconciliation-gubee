package com.gubee.stock.application.port.out;

import com.gubee.stock.domain.model.StockEvent;

public interface EventPublisherPort {
    void publish(StockEvent event);
}