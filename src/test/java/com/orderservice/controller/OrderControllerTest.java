package com.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderservice.dto.OrderDTO;
import com.orderservice.dto.OrderItemDTO;
import com.orderservice.dto.OrderStatusDTO;
import com.orderservice.exception.DuplicateOrderException;
import com.orderservice.model.OrderStatus;
import com.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private OrderDTO orderDTO;
    private UUID orderId;
    private String orderNumber;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        orderNumber = "ORD-TEST-123";

        OrderItemDTO itemDTO1 = new OrderItemDTO();
        itemDTO1.setProductId("PROD-001");
        itemDTO1.setQuantity(2);
        itemDTO1.setPrice(new BigDecimal("100.00"));

        OrderItemDTO itemDTO2 = new OrderItemDTO();
        itemDTO2.setProductId("PROD-002");
        itemDTO2.setQuantity(1);
        itemDTO2.setPrice(new BigDecimal("200.00"));

        orderDTO = new OrderDTO();
        orderDTO.setId(orderId);
        orderDTO.setOrderNumber(orderNumber);
        orderDTO.setStatus(OrderStatus.RECEIVED);
        orderDTO.setCreatedAt(LocalDateTime.now());
        orderDTO.setTotalAmount(new BigDecimal("400.00"));
        orderDTO.setItems(Arrays.asList(itemDTO1, itemDTO2));
    }

    @Test
    void createOrder_ShouldCreateAndReturnOrder() throws Exception {
        when(orderService.createOrder(any(OrderDTO.class))).thenReturn(orderDTO);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber", is(orderNumber)))
                .andExpect(jsonPath("$.status", is("RECEIVED")))
                .andExpect(jsonPath("$.totalAmount", is(400.0)))
                .andExpect(jsonPath("$.items", hasSize(2)));

        verify(orderService).createOrder(any(OrderDTO.class));
    }

    @Test
    void createOrder_ShouldReturn409WhenDuplicate() throws Exception {
        when(orderService.createOrder(any(OrderDTO.class)))
                .thenThrow(new DuplicateOrderException("Pedido duplicado"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO)))
                .andExpect(status().isConflict());

        verify(orderService).createOrder(any(OrderDTO.class));
    }

    @Test
    void getOrderById_ShouldReturnOrder() throws Exception {
        when(orderService.getOrderById(orderId)).thenReturn(orderDTO);

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(orderId.toString())))
                .andExpect(jsonPath("$.orderNumber", is(orderNumber)))
                .andExpect(jsonPath("$.status", is("RECEIVED")));

        verify(orderService).getOrderById(orderId);
    }

    @Test
    void getOrderByNumber_ShouldReturnOrder() throws Exception {
        when(orderService.getOrderByNumber(orderNumber)).thenReturn(orderDTO);

        mockMvc.perform(get("/api/orders/number/" + orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber", is(orderNumber)));

        verify(orderService).getOrderByNumber(orderNumber);
    }

    @Test
    void getOrderStatus_ShouldReturnStatus() throws Exception {
        OrderStatusDTO statusDTO = new OrderStatusDTO();
        statusDTO.setId(orderId);
        statusDTO.setOrderNumber(orderNumber);
        statusDTO.setStatus(OrderStatus.RECEIVED);
        statusDTO.setCreatedAt(LocalDateTime.now());

        when(orderService.getOrderStatus(orderId)).thenReturn(statusDTO);

        mockMvc.perform(get("/api/orders/" + orderId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber", is(orderNumber)))
                .andExpect(jsonPath("$.status", is("RECEIVED")));

        verify(orderService).getOrderStatus(orderId);
    }

    @Test
    void listOrders_ShouldReturnOrders() throws Exception {
        List<OrderDTO> orders = Collections.singletonList(orderDTO);
        when(orderService.listOrders(0, 20, null)).thenReturn(orders);

        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderNumber", is(orderNumber)));

        verify(orderService).listOrders(0, 20, null);
    }

    @Test
    void processOrder_ShouldProcessAndReturnOrder() throws Exception {
        orderDTO.setStatus(OrderStatus.CALCULATED);
        when(orderService.processOrder(orderId)).thenReturn(orderDTO);

        mockMvc.perform(post("/api/orders/" + orderId + "/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CALCULATED")));

        verify(orderService).processOrder(orderId);
    }

    @Test
    void notifyExternalSystem_ShouldNotifyAndReturnOrder() throws Exception {
        orderDTO.setStatus(OrderStatus.NOTIFIED);
        when(orderService.notifyExternalSystem(orderId)).thenReturn(orderDTO);

        mockMvc.perform(post("/api/orders/" + orderId + "/notify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("NOTIFIED")));

        verify(orderService).notifyExternalSystem(orderId);
    }
}