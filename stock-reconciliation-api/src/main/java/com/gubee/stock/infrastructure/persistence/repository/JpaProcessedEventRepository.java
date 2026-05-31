package com.gubee.stock.infrastructure.persistence.repository;

import com.gubee.stock.domain.model.EventProcessingStatus;
import com.gubee.stock.domain.model.StockEventType;
import com.gubee.stock.infrastructure.persistence.entity.ProcessedEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {

    boolean existsByEventId(String eventId);

    java.util.List<ProcessedEventEntity> findByStatus(EventProcessingStatus status);

    Page<ProcessedEventEntity> findByStatus(EventProcessingStatus status, Pageable pageable);

    @Query("""
            SELECT COUNT(e) > 0 FROM ProcessedEventEntity e
            WHERE e.type = :type
              AND e.marketplace = :marketplace
              AND e.accountId = :accountId
              AND e.externalOrderId = :externalOrderId
              AND e.sku = :sku
              AND e.status = 'PROCESSED'
            """)
    boolean existsByTypeAndBusinessKey(
            @Param("type") StockEventType type,
            @Param("marketplace") String marketplace,
            @Param("accountId") String accountId,
            @Param("externalOrderId") String externalOrderId,
            @Param("sku") String sku);

    @Query("""
            SELECT COUNT(e) > 0 FROM ProcessedEventEntity e
            WHERE e.type IN (:types)
              AND e.marketplace = :marketplace
              AND e.accountId = :accountId
              AND e.externalOrderId = :externalOrderId
              AND e.sku = :sku
              AND e.status = 'PROCESSED'
            """)
    boolean existsByTypesAndBusinessKey(
            @Param("types") java.util.List<StockEventType> types,
            @Param("marketplace") String marketplace,
            @Param("accountId") String accountId,
            @Param("externalOrderId") String externalOrderId,
            @Param("sku") String sku);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ProcessedEventEntity e WHERE e.eventId = :eventId AND e.status = 'PENDING'")
    void deletePendingByEventId(@Param("eventId") String eventId);
}