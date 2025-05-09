package com.orderservice.service;

import com.orderservice.dto.OrderDTO;
import com.orderservice.dto.OrderItemDTO;
import com.orderservice.exception.DuplicateOrderException;
import com.orderservice.integration.ExternalProductAClient;
import com.orderservice.integration.ExternalProductBClient;
import com.orderservice.integration.dto.ExternalProductDTO;
import com.orderservice.metrics.OrderMetrics;
import com.orderservice.model.Order;
import com.orderservice.model.OrderItem;
import com.orderservice.model.OrderStatus;
import com.orderservice.repository.OrderRepository;
import com.orderservice.util.OrderDuplicateChecker;
import com.orderservice.util.OrderValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ExternalProductAClient externalProductAClient;

    @Mock
    private ExternalProductBClient externalProductBClient;

    @Mock
    private OrderValidator orderValidator;

    @Mock
    private OrderDuplicateChecker duplicateChecker;

    @Mock
    private OrderMetrics orderMetrics;

    @Mock
    private OrderService self;

    @Mock
    private Executor orderProcessingExecutor;

    @Mock
    private Executor notificationExecutor;

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderDTO orderDTO;
    private Order order;
    private UUID orderId;
    private List<ExternalProductDTO> externalProducts;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(orderProcessingExecutor).execute(any(Runnable.class));

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(notificationExecutor).execute(any(Runnable.class));

        OrderItemDTO itemDTO1 = new OrderItemDTO();
        itemDTO1.setProductId("PROD-001");
        itemDTO1.setQuantity(2);

        OrderItemDTO itemDTO2 = new OrderItemDTO();
        itemDTO2.setProductId("PROD-002");
        itemDTO2.setQuantity(1);

        orderDTO = new OrderDTO();
        orderDTO.setItems(Arrays.asList(itemDTO1, itemDTO2));

        order = new Order();
        order.setId(orderId);
        order.setOrderNumber("ORD-TEST-123");
        order.setStatus(OrderStatus.RECEIVED);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalAmount(BigDecimal.ZERO);
        order.setNotifiedToExternalB(false);

        OrderItem item1 = new OrderItem();
        item1.setProductId("PROD-001");
        item1.setQuantity(2);
        item1.setPrice(BigDecimal.valueOf(100));
        item1.setOrder(order);

        OrderItem item2 = new OrderItem();
        item2.setProductId("PROD-002");
        item2.setQuantity(1);
        item2.setPrice(BigDecimal.valueOf(200));
        item2.setOrder(order);

        order.setItems(Arrays.asList(item1, item2));

        ExternalProductDTO product1 = new ExternalProductDTO();
        product1.setId("PROD-001");
        product1.setName("Produto 1");
        product1.setPrice(BigDecimal.valueOf(100));

        ExternalProductDTO product2 = new ExternalProductDTO();
        product2.setId("PROD-002");
        product2.setName("Produto 2");
        product2.setPrice(BigDecimal.valueOf(200));

        externalProducts = Arrays.asList(product1, product2);

        ReflectionTestUtils.setField(orderService, "self", self);
    }

    @Test
    void createOrder_ShouldCreateAndProcessOrderSuccessfully() throws DuplicateOrderException {
        when(duplicateChecker.isOrderNumberDuplicate(anyString())).thenReturn(false);
        when(duplicateChecker.isOrderContentDuplicate(any(OrderDTO.class))).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderDTO result = orderService.createOrder(orderDTO);

        assertNotNull(result);
        assertEquals(order.getOrderNumber(), result.getOrderNumber());
        verify(orderValidator).validate(orderDTO);
        verify(orderRepository).save(any(Order.class));
        verify(orderProcessingExecutor).execute(any(Runnable.class));
    }

    @Test
    void createOrder_ShouldDetectDuplicateOrderNumber() {
        orderDTO.setOrderNumber("ORD-TEST-123");
        when(duplicateChecker.isOrderNumberDuplicate("ORD-TEST-123")).thenReturn(true);

        assertThrows(DuplicateOrderException.class, () -> orderService.createOrder(orderDTO));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldDetectDuplicateOrderContent() {
        when(duplicateChecker.isOrderNumberDuplicate(anyString())).thenReturn(false);
        when(duplicateChecker.isOrderContentDuplicate(orderDTO)).thenReturn(true);

        assertThrows(DuplicateOrderException.class, () -> orderService.createOrder(orderDTO));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void processOrder_ShouldProcessOrderSuccessfully() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(externalProductAClient.getProducts()).thenReturn(externalProducts);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderDTO result = orderService.processOrder(orderId);

        assertNotNull(result);
        assertEquals(OrderStatus.CALCULATED, result.getStatus());
        assertEquals(new BigDecimal("400.00"), order.getTotalAmount());
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void processOrder_ShouldHandleAlreadyProcessedOrder() {
        order.setStatus(OrderStatus.CALCULATED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderDTO result = orderService.processOrder(orderId);

        assertNotNull(result);
        assertEquals(OrderStatus.CALCULATED, result.getStatus());
        verify(externalProductAClient, never()).getProducts();
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void notifyExternalSystem_ShouldNotifySuccessfully() {
        order.setStatus(OrderStatus.CALCULATED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        doNothing().when(externalProductBClient).notifyOrder(any());

        OrderDTO result = orderService.notifyExternalSystem(orderId);

        assertNotNull(result);
        assertEquals(OrderStatus.NOTIFIED, result.getStatus());
        assertTrue(order.isNotifiedToExternalB());
        verify(externalProductBClient).notifyOrder(any());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void notifyExternalSystem_ShouldHandleAlreadyNotifiedOrder() {
        order.setStatus(OrderStatus.NOTIFIED);
        order.setNotifiedToExternalB(true);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderDTO result = orderService.notifyExternalSystem(orderId);

        assertNotNull(result);
        assertEquals(OrderStatus.NOTIFIED, result.getStatus());
        verify(externalProductBClient, never()).notifyOrder(any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrderById_ShouldReturnOrder() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(self.getOrderById(orderId)).thenReturn(orderService.mapToDto(order));

        OrderDTO result = orderService.getOrderById(orderId);

        assertNotNull(result);
        assertEquals(order.getOrderNumber(), result.getOrderNumber());
    }

    @Test
    void getOrderById_ShouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> orderService.getOrderById(orderId));
    }

    @Test
    void getOrderByNumber_ShouldReturnOrder() {
        String orderNumber = "ORD-TEST-123";
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));

        OrderDTO result = orderService.getOrderByNumber(orderNumber);

        assertNotNull(result);
        assertEquals(orderNumber, result.getOrderNumber());
    }

    @Test
    void getOrderByNumber_ShouldThrowExceptionWhenOrderNotFound() {
        String orderNumber = "NONEXISTENT";
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> orderService.getOrderByNumber(orderNumber));
    }
}