package com.orderservice.model;

/**
 * Enum que representa os possíveis estados de um pedido no sistema.
 */
public enum OrderStatus {
    /**
     * Pedido recebido, ainda não processado
     */
    RECEIVED,

    /**
     * Pedido em processamento
     */
    PROCESSING,

    /**
     * Pedido com produtos calculados
     */
    CALCULATED,

    /**
     * Pedido notificado ao sistema externo B
     */
    NOTIFIED,

    /**
     * Pedido completamente processado
     */
    COMPLETED,

    /**
     * Pedido com erro durante o processamento
     */
    ERROR
}