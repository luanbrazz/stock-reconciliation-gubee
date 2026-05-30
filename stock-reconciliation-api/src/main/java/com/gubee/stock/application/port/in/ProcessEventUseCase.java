package com.gubee.stock.application.port.in;

import com.gubee.stock.domain.model.EventProcessingStatus;
import com.gubee.stock.domain.model.StockEvent;

public interface ProcessEventUseCase {
    EventProcessingStatus process(StockEvent event);
}