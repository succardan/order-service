package com.orderservice.service;

import com.orderservice.dto.OrderDTO;
import com.orderservice.dto.OrderStatusDTO;
import com.orderservice.exception.DuplicateOrderException;
import com.orderservice.model.OrderStatus;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    /**
     * Cria um novo pedido a partir dos dados recebidos
     * @param orderDTO dados do pedido
     * @return o pedido criado com ID e número gerados
     * @throws DuplicateOrderException se o pedido já existir
     */
    OrderDTO createOrder(OrderDTO orderDTO) throws DuplicateOrderException;

    /**
     * Processa um pedido, obtendo os preços dos produtos do sistema externo A
     * e calculando o total
     * @param id ID do pedido
     * @return o pedido processado e calculado
     */
    OrderDTO processOrder(UUID id);

    /**
     * Notifica o sistema externo B sobre o pedido calculado
     * @param id ID do pedido
     * @return o pedido notificado
     */
    OrderDTO notifyExternalSystem(UUID id);

    /**
     * Obtém um pedido pelo ID
     * @param id ID do pedido
     * @return o pedido com todos os detalhes
     */
    OrderDTO getOrderById(UUID id);

    /**
     * Obtém um pedido pelo número
     * @param orderNumber número do pedido
     * @return o pedido com todos os detalhes
     */
    OrderDTO getOrderByNumber(String orderNumber);

    /**
     * Lista os pedidos com paginação
     * @param page número da página
     * @param size tamanho da página
     * @param status filtro opcional por status
     * @return lista de pedidos
     */
    List<OrderDTO> listOrders(int page, int size, OrderStatus status);

    /**
     * Obtém o status de um pedido
     * @param id ID do pedido
     * @return status atual do pedido
     */
    OrderStatusDTO getOrderStatus(UUID id);
}