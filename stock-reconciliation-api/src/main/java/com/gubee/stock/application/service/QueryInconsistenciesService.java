package com.gubee.stock.application.service;

import com.gubee.stock.application.port.in.QueryInconsistenciesUseCase;
import com.gubee.stock.application.port.out.ProcessedEventRepository;
import com.gubee.stock.application.port.out.ProcessedEventSummary;
import com.gubee.stock.domain.model.EventProcessingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QueryInconsistenciesService implements QueryInconsistenciesUseCase {

    private final ProcessedEventRepository processedEventRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProcessedEventSummary> findByStatus(String status, Pageable pageable) {
        return processedEventRepository.findByStatus(
                EventProcessingStatus.valueOf(status), pageable);
    }
}