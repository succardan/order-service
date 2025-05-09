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

/**
 * Repositório de pedidos otimizado para alta volumetria
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Busca um pedido pelo número
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Busca pedidos por status com paginação
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Busca pedidos recebidos que foram criados antes de um tempo limite
     * Útil para identificar pedidos que podem ter ficado presos
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt < :cutoffTime ORDER BY o.createdAt ASC")
    List<Order> findByStatusAndCreatedAtBefore(
            @Param("status") OrderStatus status,
            @Param("cutoffTime") LocalDateTime cutoffTime,
            Pageable pageable);

    /**
     * Versão com limite direto para o scheduler
     */
    default List<Order> findByStatusAndCreatedAtBefore(
            OrderStatus status, LocalDateTime cutoffTime, int limit) {
        return findByStatusAndCreatedAtBefore(status, cutoffTime,
                Pageable.ofSize(limit));
    }

    /**
     * Busca pedidos calculados mas não notificados
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.notifiedToExternalB = false ORDER BY o.processedAt ASC")
    List<Order> findByStatusAndNotifiedToExternalBFalse(
            @Param("status") OrderStatus status,
            Pageable pageable);

    /**
     * Versão com limite direto para o scheduler
     */
    default List<Order> findByStatusAndNotifiedToExternalBFalse(
            OrderStatus status, int limit) {
        return findByStatusAndNotifiedToExternalBFalse(status,
                Pageable.ofSize(limit));
    }

    /**
     * Busca pedidos com erro que não excederam o limite de tentativas
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.retryCount < :maxRetries ORDER BY o.processedAt ASC")
    List<Order> findByStatusAndRetryCountLessThan(
            @Param("status") OrderStatus status,
            @Param("maxRetries") int maxRetries,
            Pageable pageable);

    /**
     * Versão com limite direto para o scheduler
     */
    default List<Order> findByStatusAndRetryCountLessThan(
            OrderStatus status, int maxRetries, int limit) {
        return findByStatusAndRetryCountLessThan(status, maxRetries,
                Pageable.ofSize(limit));
    }

    /**
     * Conta pedidos criados em um intervalo de tempo
     */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Conta pedidos por status em um intervalo de tempo
     */
    long countByStatusAndCreatedAtBetween(OrderStatus status, LocalDateTime start, LocalDateTime end);

    /**
     * Remove pedidos completos mais antigos que uma data específica
     * Usado para limpezas periódicas de dados antigos
     */
    @Modifying
    @Query("DELETE FROM Order o WHERE o.status = 'COMPLETED' AND o.completedAt < :date")
    int deleteCompletedOrdersOlderThan(@Param("date") LocalDateTime date);

    /**
     * Consulta para estatísticas do dia atual
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE DATE(o.createdAt) = CURRENT_DATE")
    long countOrdersCreatedToday();

    /**
     * Consulta de pedidos agrupados por status
     */
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();

    /**
     * Busca pedidos com tempo de processamento excedido
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PROCESSING' AND o.createdAt < :cutoffTime")
    List<Order> findStuckInProcessing(@Param("cutoffTime") LocalDateTime cutoffTime, Pageable pageable);
}