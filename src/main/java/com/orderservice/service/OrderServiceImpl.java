package com.orderservice.service;

import com.orderservice.dto.OrderDTO;
import com.orderservice.dto.OrderItemDTO;
import com.orderservice.dto.OrderStatusDTO;
import com.orderservice.exception.DuplicateOrderException;
import com.orderservice.integration.ExternalProductAClient;
import com.orderservice.integration.ExternalProductBClient;
import com.orderservice.integration.dto.ExternalOrderDTO;
import com.orderservice.integration.dto.ExternalProductDTO;
import com.orderservice.model.Order;
import com.orderservice.model.OrderItem;
import com.orderservice.model.OrderStatus;
import com.orderservice.repository.OrderRepository;
import com.orderservice.util.OrderDuplicateChecker;
import com.orderservice.util.OrderValidator;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementação otimizada do serviço de pedidos para alta volumetria
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ExternalProductAClient externalProductAClient;
    private final ExternalProductBClient externalProductBClient;
    private final OrderValidator orderValidator;
    private final OrderDuplicateChecker duplicateChecker;
    private final Executor orderProcessingExecutor;
    private final Executor notificationExecutor;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            ExternalProductAClient externalProductAClient,
            ExternalProductBClient externalProductBClient,
            OrderValidator orderValidator,
            OrderDuplicateChecker duplicateChecker,
            @Qualifier("orderProcessingExecutor") Executor orderProcessingExecutor,
            @Qualifier("notificationExecutor") Executor notificationExecutor) {
        this.orderRepository = orderRepository;
        this.externalProductAClient = externalProductAClient;
        this.externalProductBClient = externalProductBClient;
        this.orderValidator = orderValidator;
        this.duplicateChecker = duplicateChecker;
        this.orderProcessingExecutor = orderProcessingExecutor;
        this.notificationExecutor = notificationExecutor;
    }

    @Override
    @Transactional
    @RateLimiter(name = "default")
    public OrderDTO createOrder(OrderDTO orderDTO) throws DuplicateOrderException {
        if (orderDTO.getOrderNumber() != null &&
                duplicateChecker.isOrderNumberDuplicate(orderDTO.getOrderNumber())) {
            throw new DuplicateOrderException("Pedido com número " + orderDTO.getOrderNumber() + " já existe");
        }
        if (duplicateChecker.isOrderContentDuplicate(orderDTO)) {
            throw new DuplicateOrderException("Pedido com conteúdo duplicado detectado");
        }
        orderValidator.validate(orderDTO);

        Order order = mapToEntity(orderDTO);
        order.setStatus(OrderStatus.RECEIVED);

        Order savedOrder = orderRepository.save(order);
        log.info("Pedido criado com sucesso: {}", savedOrder.getOrderNumber());

        CompletableFuture.runAsync(() -> {
            try {
                processOrderAsync(savedOrder.getId());
            } catch (Exception e) {
                log.error("Erro no processamento assíncrono inicial do pedido {}: {}",
                        savedOrder.getId(), e.getMessage(), e);
            }
        }, orderProcessingExecutor);

        return mapToDto(savedOrder);
    }

    /**
     * Processamento assíncrono completo do pedido
     * Executa o processo completo: processamento, cálculo e notificação
     */
    @Async("orderProcessingExecutor")
    @Transactional
    protected void processOrderAsync(UUID orderId) {
        try {
            log.debug("Iniciando processamento assíncrono para pedido: {}", orderId);
            OrderDTO processedOrder = processOrder(orderId);

            if (processedOrder != null &&
                    (processedOrder.getStatus() == OrderStatus.CALCULATED ||
                            processedOrder.getStatus() == OrderStatus.NOTIFIED)) {

                CompletableFuture.runAsync(() -> {
                    try {
                        notifyExternalSystem(orderId);
                    } catch (Exception e) {
                        log.error("Erro na notificação assíncrona do pedido {}: {}",
                                orderId, e.getMessage(), e);
                        updateOrderStatus(orderId);
                    }
                }, notificationExecutor);
            }
        } catch (Exception e) {
            log.error("Erro no processamento assíncrono do pedido {}: {}",
                    orderId, e.getMessage(), e);
            updateOrderStatus(orderId);
        }
    }

    /**
     * Processamento do pedido com recursos de resiliência
     * Circuit breaker para isolamento de falhas
     * Retry para tentativas automáticas em caso de falhas transitórias
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = "orders", key = "#id")
    @CircuitBreaker(name = "externalServiceA", fallbackMethod = "processOrderFallback")
    @Retry(name = "default")
    public OrderDTO processOrder(UUID id) {
        Order order = findOrderEntityById(id);

        if (order.getStatus() != OrderStatus.RECEIVED) {
            log.info("Pedido {} já foi processado. Status atual: {}", id, order.getStatus());
            return mapToDto(order);
        }

        try {
            log.info("Iniciando processamento do pedido: {}", order.getOrderNumber());
            order.setStatus(OrderStatus.PROCESSING);
            order = orderRepository.save(order);

            List<String> productIds = order.getItems().stream()
                    .map(OrderItem::getProductId)
                    .collect(Collectors.toList());

            Map<String, ExternalProductDTO> productMap = fetchProductsInBatch(productIds);

            for (OrderItem item : order.getItems()) {
                ExternalProductDTO product = productMap.get(item.getProductId());
                if (product != null) {
                    item.setProductName(product.getName());
                    item.setPrice(product.getPrice());
                } else {
                    log.warn("Produto não encontrado: {}", item.getProductId());
                    if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                        item.setPrice(BigDecimal.ZERO);
                    }
                }
            }

            order.calculateTotal();
            order.setStatus(OrderStatus.CALCULATED);
            order.setProcessedAt(LocalDateTime.now());

            Order updatedOrder = orderRepository.save(order);
            log.info("Pedido processado com sucesso: {}", updatedOrder.getOrderNumber());

            return mapToDto(updatedOrder);
        } catch (Exception e) {
            log.error("Erro ao processar pedido {}: {}", id, e.getMessage(), e);
            order.setStatus(OrderStatus.ERROR);
            orderRepository.save(order);
            throw new RuntimeException("Erro ao processar pedido: " + e.getMessage(), e);
        }
    }

    /**
     * Busca produtos em lote para maior eficiência
     */
    private Map<String, ExternalProductDTO> fetchProductsInBatch(List<String> productIds) {
        try {
            List<ExternalProductDTO> products = externalProductAClient.getProducts();
            if (products != null && !products.isEmpty()) {
                return products.stream()
                        .filter(p -> productIds.contains(p.getId()))
                        .collect(Collectors.toMap(ExternalProductDTO::getId, Function.identity()));
            }
        } catch (Exception e) {
            log.warn("Falha ao buscar produtos em lote, tentando individualmente: {}", e.getMessage());
        }

        Map<String, ExternalProductDTO> resultMap = new HashMap<>();
        for (String productId : productIds) {
            try {
                ExternalProductDTO product = externalProductAClient.getProduct(productId);
                if (product != null) {
                    resultMap.put(productId, product);
                }
            } catch (Exception e) {
                log.warn("Erro ao buscar produto {}: {}", productId, e.getMessage());
            }
        }
        return resultMap;
    }

    /**
     * Metodo de fallback para processamento de pedido
     * Executado quando o circuit breaker é acionado
     */
    public OrderDTO processOrderFallback(UUID id, Exception ex) {
        log.warn("Executando fallback para processamento do pedido {}: {}", id, ex.getMessage());
        Order order = findOrderEntityById(id);

        if (order.getStatus() == OrderStatus.ERROR) {
            return mapToDto(order);
        }

        order.setStatus(OrderStatus.ERROR);
        orderRepository.save(order);
        return mapToDto(order);
    }

    /**
     * Notificação para o sistema externo com resiliência e bulkhead
     * Bulkhead para limitar chamadas paralelas ao sistema externo B
     * Circuit breaker para detectar falhas no serviço externo
     */
    @Override
    @Transactional
    @CacheEvict(value = "orders", key = "#id")
    @CircuitBreaker(name = "externalServiceB", fallbackMethod = "notifyExternalSystemFallback")
    @Bulkhead(name = "default")
    @Retry(name = "default")
    public OrderDTO notifyExternalSystem(UUID id) {
        Order order = findOrderEntityById(id);

        if (order.isNotifiedToExternalB()) {
            log.info("Pedido {} já foi notificado ao sistema externo B", id);
            return mapToDto(order);
        }

        if (order.getStatus() != OrderStatus.CALCULATED && order.getStatus() != OrderStatus.NOTIFIED) {
            log.warn("Pedido {} não está calculado. Status atual: {}", id, order.getStatus());
            return mapToDto(order);
        }

        try {
            log.info("Notificando sistema externo B sobre o pedido: {}", order.getOrderNumber());

            ExternalOrderDTO externalOrderDTO = new ExternalOrderDTO();
            externalOrderDTO.setOrderNumber(order.getOrderNumber());
            externalOrderDTO.setStatus("CALCULATED");
            externalOrderDTO.setTotalAmount(order.getTotalAmount());

            List<ExternalOrderDTO.ExternalOrderItemDTO> externalItems = order.getItems().stream()
                    .map(item -> {
                        ExternalOrderDTO.ExternalOrderItemDTO externalItem = new ExternalOrderDTO.ExternalOrderItemDTO();
                        externalItem.setProductId(item.getProductId());
                        externalItem.setProductName(item.getProductName());
                        externalItem.setQuantity(item.getQuantity());
                        externalItem.setPrice(item.getPrice());
                        return externalItem;
                    })
                    .collect(Collectors.toList());

            externalOrderDTO.setItems(externalItems);

            externalProductBClient.notifyOrder(externalOrderDTO);

            order.setNotifiedToExternalB(true);
            order.setStatus(OrderStatus.NOTIFIED);
            order.setCompletedAt(LocalDateTime.now());

            Order updatedOrder = orderRepository.save(order);
            log.info("Pedido notificado com sucesso: {}", updatedOrder.getOrderNumber());

            return mapToDto(updatedOrder);
        } catch (Exception e) {
            log.error("Erro ao notificar sistema externo sobre pedido {}: {}", id, e.getMessage(), e);

            if (order.getStatus() != OrderStatus.CALCULATED) {
                order.setStatus(OrderStatus.ERROR);
                orderRepository.save(order);
            }

            throw new RuntimeException("Erro ao notificar sistema externo: " + e.getMessage(), e);
        }
    }

    /**
     * Metodo de fallback para notificação
     * Executado quando o circuit breaker é acionado
     */
    @Transactional
    public OrderDTO notifyExternalSystemFallback(UUID id, Exception ex) {
        log.warn("Executando fallback para notificação do pedido {}: {}", id, ex.getMessage());
        return getOrderById(id);
    }

    /**
     * Busca de pedido por ID com cache
     */
    @Override
    @Transactional
    @Cacheable(value = "orders", key = "#id")
    public OrderDTO getOrderById(UUID id) {
        return mapToDto(findOrderEntityById(id));
    }

    /**
     * Busca de pedido por número com cache
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "#orderNumber")
    public OrderDTO getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + orderNumber));
    }

    /**
     * Listagem de pedidos com paginação e filtro opcional
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> listOrders(int page, int size, OrderStatus status) {
        int limitedSize = Math.min(size, 100);

        if (status != null) {
            return orderRepository.findByStatus(status, PageRequest.of(page, limitedSize))
                    .stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        } else {
            return orderRepository.findAll(PageRequest.of(page, limitedSize))
                    .stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Consulta de status com cache de curta duração
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orderStatuses", key = "#id")
    public OrderStatusDTO getOrderStatus(UUID id) {
        Order order = findOrderEntityById(id);

        OrderStatusDTO statusDTO = new OrderStatusDTO();
        statusDTO.setId(order.getId());
        statusDTO.setOrderNumber(order.getOrderNumber());
        statusDTO.setStatus(order.getStatus());
        statusDTO.setCreatedAt(order.getCreatedAt());
        statusDTO.setProcessedAt(order.getProcessedAt());
        statusDTO.setCompletedAt(order.getCompletedAt());
        statusDTO.setTotalAmount(order.getTotalAmount());

        return statusDTO;
    }

    /**
     * Busca entidade com otimização de cache
     */
    private Order findOrderEntityById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + id));
    }

    /**
     * Atualização de status com nova transação
     */
    @Transactional
    protected void updateOrderStatus(UUID id) {
        orderRepository.findById(id).ifPresent(order -> {
            order.setStatus(OrderStatus.ERROR);
            orderRepository.save(order);
        });
    }

    /**
     * Mapeamento DTO -> Entity otimizado
     */
    private Order mapToEntity(OrderDTO dto) {
        Order order = new Order();
        order.setOrderNumber(dto.getOrderNumber());

        if (dto.getItems() != null) {
            for (OrderItemDTO itemDto : dto.getItems()) {
                OrderItem item = new OrderItem();
                item.setProductId(itemDto.getProductId());
                item.setProductName(itemDto.getProductName());
                item.setQuantity(itemDto.getQuantity());
                item.setPrice(itemDto.getPrice() != null ? itemDto.getPrice() : BigDecimal.ZERO);
                order.addItem(item);
            }
        }

        return order;
    }

    /**
     * Mapeamento Entity -> DTO otimizado
     */
    public OrderDTO mapToDto(Order entity) {
        OrderDTO dto = new OrderDTO();
        dto.setId(entity.getId());
        dto.setOrderNumber(entity.getOrderNumber());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setProcessedAt(entity.getProcessedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setTotalAmount(entity.getTotalAmount());

        if (entity.getItems() != null) {
            dto.setItems(entity.getItems().stream()
                    .map(this::mapToItemDto)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    /**
     * Mapeamento de item da Entity -> DTO
     */
    private OrderItemDTO mapToItemDto(OrderItem entity) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(entity.getId());
        dto.setProductId(entity.getProductId());
        dto.setProductName(entity.getProductName());
        dto.setQuantity(entity.getQuantity());
        dto.setPrice(entity.getPrice());
        return dto;
    }
}