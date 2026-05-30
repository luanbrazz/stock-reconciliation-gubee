package com.gubee.stock.application.port.in;

import com.gubee.stock.application.port.out.ProcessedEventSummary;

import java.util.List;

public interface QueryInconsistenciesUseCase {
    List<ProcessedEventSummary> findByStatus(String status);
}