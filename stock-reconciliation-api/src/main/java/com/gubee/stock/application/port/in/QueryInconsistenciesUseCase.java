package com.gubee.stock.application.port.in;

import com.gubee.stock.application.port.out.ProcessedEventSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QueryInconsistenciesUseCase {
    Page<ProcessedEventSummary> findByStatus(String status, Pageable pageable);
}