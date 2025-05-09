package com.orderservice.repository;

import com.orderservice.model.Order;
import com.orderservice.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {


    Optional<Order> findByOrderNumber(String orderNumber);


    Page<Order> findByStatus(OrderStatus status, Pageable pageable);


    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt < :cutoffTime ORDER BY o.createdAt ASC")
    List<Order> findByStatusAndCreatedAtBefore(
            @Param("status") OrderStatus status,
            @Param("cutoffTime") LocalDateTime cutoffTime,
            Pageable pageable);


    default List<Order> findByStatusAndCreatedAtBefore(
            OrderStatus status, LocalDateTime cutoffTime, int limit) {
        return findByStatusAndCreatedAtBefore(status, cutoffTime,
                Pageable.ofSize(limit));
    }


    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.notifiedToExternalB = false ORDER BY o.processedAt ASC")
    List<Order> findByStatusAndNotifiedToExternalBFalse(
            @Param("status") OrderStatus status,
            Pageable pageable);


    default List<Order> findByStatusAndNotifiedToExternalBFalse(
            OrderStatus status, int limit) {
        return findByStatusAndNotifiedToExternalBFalse(status,
                Pageable.ofSize(limit));
    }


    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.retryCount < :maxRetries ORDER BY o.processedAt ASC")
    List<Order> findByStatusAndRetryCountLessThan(
            @Param("status") OrderStatus status,
            @Param("maxRetries") int maxRetries,
            Pageable pageable);


    default List<Order> findByStatusAndRetryCountLessThan(
            OrderStatus status, int maxRetries, int limit) {
        return findByStatusAndRetryCountLessThan(status, maxRetries,
                Pageable.ofSize(limit));
    }


    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);


    long countByStatusAndCreatedAtBetween(OrderStatus status, LocalDateTime start, LocalDateTime end);


    @Modifying
    @Query("DELETE FROM Order o WHERE o.status = 'COMPLETED' AND o.completedAt < :date")
    int deleteCompletedOrdersOlderThan(@Param("date") LocalDateTime date);


    @Query("SELECT COUNT(o) FROM Order o WHERE FUNCTION('YEAR', o.createdAt) = FUNCTION('YEAR', CURRENT_TIMESTAMP) AND FUNCTION('MONTH', o.createdAt) = FUNCTION('MONTH', CURRENT_TIMESTAMP) AND FUNCTION('DAY', o.createdAt) = FUNCTION('DAY', CURRENT_TIMESTAMP)")
    long countOrdersCreatedToday();


    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();


    @Query("SELECT o FROM Order o WHERE o.status = 'PROCESSING' AND o.createdAt < :cutoffTime")
    List<Order> findStuckInProcessing(@Param("cutoffTime") LocalDateTime cutoffTime, Pageable pageable);
}
