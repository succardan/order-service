package com.orderservice.service;

import com.orderservice.dto.OrderDTO;
import com.orderservice.dto.OrderStatusDTO;
import com.orderservice.exception.DuplicateOrderException;
import com.orderservice.model.OrderStatus;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderDTO createOrder(OrderDTO orderDTO) throws DuplicateOrderException;

    OrderDTO processOrder(UUID id);

    OrderDTO notifyExternalSystem(UUID id);

    OrderDTO getOrderById(UUID id);

    OrderDTO getOrderByNumber(String orderNumber);

    List<OrderDTO> listOrders(int page, int size, OrderStatus status);

    OrderStatusDTO getOrderStatus(UUID id);
}
