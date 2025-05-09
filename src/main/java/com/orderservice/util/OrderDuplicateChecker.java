package com.orderservice.util;

import com.orderservice.dto.OrderDTO;
import com.orderservice.dto.OrderItemDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Sistema otimizado para detecção de pedidos duplicados em ambiente de alta volumetria.
 * Combina multiple estratégias:
 * 1. Cache para verificação rápida
 * 2. Hashing de conteúdo para identificar duplicações semânticas
 * 3. In-memory buffer para detecção em tempo real durante bursts
 */
@Component
@Slf4j
public class OrderDuplicateChecker {

    private final ConcurrentMap<String, Boolean> recentOrderNumbers = new ConcurrentHashMap<>(10000);
    private final ConcurrentMap<String, Boolean> recentOrderHashes = new ConcurrentHashMap<>(10000);

    private final OrderDuplicateChecker self;

    @Autowired
    public OrderDuplicateChecker(@Lazy OrderDuplicateChecker self) {
        this.self = self;
    }

    /**
     * Verifica se o número do pedido já foi processado recentemente
     * Primeiro verifica in-memory buffer, depois o cache
     *
     * @param orderNumber número do pedido
     * @return true se o pedido for duplicado
     */
    public boolean isOrderNumberDuplicate(String orderNumber) {
        if (recentOrderNumbers.containsKey(orderNumber)) {
            log.info("Pedido duplicado detectado (buffer em memória): {}", orderNumber);
            return true;
        }

        Boolean exists = self.checkOrderNumberCache(orderNumber);
        if (exists != null && exists) {
            addToRecentOrderNumbers(orderNumber);
            log.info("Pedido duplicado detectado (cache): {}", orderNumber);
            return true;
        }

        addToRecentOrderNumbers(orderNumber);
        self.markOrderNumberAsProcessed(orderNumber);
        return false;
    }

    /**
     * Verifica se o conteúdo do pedido é duplicado, mesmo que o número seja diferente.
     * Útil para detecção de reenvios com números diferentes.
     *
     * @param orderDTO dados do pedido
     * @return true se for detectado conteúdo duplicado
     */
    public boolean isOrderContentDuplicate(OrderDTO orderDTO) {
        if (orderDTO == null || orderDTO.getItems() == null || orderDTO.getItems().isEmpty()) {
            return false;
        }

        String hash = calculateOrderHash(orderDTO);
        if (recentOrderHashes.containsKey(hash)) {
            log.info("Conteúdo de pedido duplicado detectado (buffer em memória)");
            return true;
        }

        Boolean exists = self.checkOrderHashCache(hash);
        if (exists != null && exists) {
            addToRecentOrderHashes(hash);
            log.info("Conteúdo de pedido duplicado detectado (cache)");
            return true;
        }

        addToRecentOrderHashes(hash);
        self.markOrderHashAsProcessed(hash);
        return false;
    }

    /**
     * Verifica número de pedido no cache
     */
    @Cacheable(value = "orderNumbers", unless = "#result == null")
    public Boolean checkOrderNumberCache(String orderNumber) {
        return false;
    }

    /**
     * Registra número de pedido no cache
     */
    @CachePut(value = "orderNumbers", key = "#orderNumber")
    public Boolean markOrderNumberAsProcessed(String orderNumber) {
        return true;
    }

    /**
     * Verifica hash de conteúdo de pedido no cache
     */
    @Cacheable(value = "orderHashes", unless = "#result == null")
    public Boolean checkOrderHashCache(String hash) {
        return false;
    }

    /**
     * Registra hash de conteúdo no cache
     */
    @CachePut(value = "orderHashes", key = "#hash")
    public Boolean markOrderHashAsProcessed(String hash) {
        return true;
    }

    /**
     * Adiciona ao buffer em memória com limpeza para evitar memory leaks
     */
    private void addToRecentOrderNumbers(String orderNumber) {
        if (recentOrderNumbers.size() > 9000) {
            log.info("Limpando buffer de números de pedido (tamanho: {})", recentOrderNumbers.size());
            recentOrderNumbers.clear();
        }
        recentOrderNumbers.put(orderNumber, Boolean.TRUE);
    }

    /**
     * Adiciona ao buffer em memória com limpeza para evitar memory leaks
     */
    private void addToRecentOrderHashes(String hash) {
        if (recentOrderHashes.size() > 9000) {
            log.info("Limpando buffer de hashes de pedido (tamanho: {})", recentOrderHashes.size());
            recentOrderHashes.clear();
        }
        recentOrderHashes.put(hash, Boolean.TRUE);
    }

    /**
     * Calcula um hash determinístico do conteúdo do pedido
     * Considera apenas os itens, quantidades e produtos - não considera valores que
     * podem variar, como preços
     */
    private String calculateOrderHash(OrderDTO orderDTO) {
        try {
            StringBuilder sb = new StringBuilder();
            orderDTO.getItems().stream()
                    .sorted(Comparator.comparing(OrderItemDTO::getProductId))
                    .forEach(item -> sb.append(item.getProductId())
                            .append(':')
                            .append(item.getQuantity())
                            .append(';'));

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("Erro ao calcular hash do pedido", e);
            return String.valueOf(orderDTO.getItems().hashCode());
        }
    }
}