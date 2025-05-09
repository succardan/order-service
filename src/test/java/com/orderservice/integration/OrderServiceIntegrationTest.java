package com.orderservice.integration;

import com.orderservice.dto.OrderDTO;
import com.orderservice.dto.OrderItemDTO;
import com.orderservice.exception.DuplicateOrderException;
import com.orderservice.integration.mock.MockExternalProductAClient;
import com.orderservice.integration.mock.MockExternalProductBClient;
import com.orderservice.model.OrderStatus;
import com.orderservice.repository.OrderRepository;
import com.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("mock")
@Transactional
public class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ExternalProductAClient productAClient;

    @Autowired
    private ExternalProductBClient productBClient;

    @Test
    public void testCreateOrder() throws DuplicateOrderException {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderNumber("INT-TEST-" + System.currentTimeMillis());
        
        OrderItemDTO item1 = new OrderItemDTO();
        item1.setProductId("PROD-1");
        item1.setQuantity(2);
        
        OrderItemDTO item2 = new OrderItemDTO();
        item2.setProductId("PROD-2");
        item2.setQuantity(1);
        
        orderDTO.setItems(Arrays.asList(item1, item2));
        
        OrderDTO createdOrder = orderService.createOrder(orderDTO);
        assertNotNull(createdOrder);
        assertNotNull(createdOrder.getId());
        assertEquals(OrderStatus.RECEIVED, createdOrder.getStatus());
    }
    
    @Test
    public void testGetOrderById() throws DuplicateOrderException {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderNumber("INT-TEST-GET-" + System.currentTimeMillis());
        
        OrderItemDTO item = new OrderItemDTO();
        item.setProductId("PROD-1");
        item.setQuantity(2);
        
        orderDTO.setItems(Arrays.asList(item));
        
        OrderDTO createdOrder = orderService.createOrder(orderDTO);
        UUID orderId = createdOrder.getId();
        
        OrderDTO retrievedOrder = orderService.getOrderById(orderId);
        assertNotNull(retrievedOrder);
        assertEquals(createdOrder.getOrderNumber(), retrievedOrder.getOrderNumber());
    }
    

}
