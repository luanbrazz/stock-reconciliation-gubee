package com.gubee.stock.application.port.in;

import java.util.List;

public interface QueryInconsistenciesUseCase {
    List<?> findByStatus(String status);
}