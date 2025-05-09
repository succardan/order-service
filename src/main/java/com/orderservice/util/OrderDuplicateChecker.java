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

    @Cacheable(value = "orderNumbers", cacheManager = "duplicateCheckCacheManager", unless = "#result == null")
    public Boolean checkOrderNumberCache(String orderNumber) {
        return false;
    }

    @CachePut(value = "orderNumbers", cacheManager = "duplicateCheckCacheManager", key = "#orderNumber")
    public Boolean markOrderNumberAsProcessed(String orderNumber) {
        return true;
    }

    @Cacheable(value = "orderHashes", cacheManager = "duplicateCheckCacheManager", unless = "#result == null")
    public Boolean checkOrderHashCache(String hash) {
        return false;
    }

    @CachePut(value = "orderHashes", cacheManager = "duplicateCheckCacheManager", key = "#hash")
    public Boolean markOrderHashAsProcessed(String hash) {
        return true;
    }

    private void addToRecentOrderNumbers(String orderNumber) {
        if (recentOrderNumbers.size() > 9000) {
            log.info("Limpando buffer de números de pedido (tamanho: {})", recentOrderNumbers.size());
            recentOrderNumbers.clear();
        }
        recentOrderNumbers.put(orderNumber, Boolean.TRUE);
    }

    private void addToRecentOrderHashes(String hash) {
        if (recentOrderHashes.size() > 9000) {
            log.info("Limpando buffer de hashes de pedido (tamanho: {})", recentOrderHashes.size());
            recentOrderHashes.clear();
        }
        recentOrderHashes.put(hash, Boolean.TRUE);
    }

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
