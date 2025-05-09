package com.orderservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Configuração de cache otimizada para alta performance
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cache principal para objetos de negócio
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(
                Arrays.asList("orders", "products", "productPrices", "orderStatuses", "dailyStats"));
        cacheManager.setCaffeine(Caffeine.<Object, Object>newBuilder()
                .initialCapacity(200)
                .maximumSize(10000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }

    /**
     * Cache de curta duração para dados muito voláteis
     */
    @Bean
    public CacheManager shortLivedCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(
                Arrays.asList("externalServiceResponses", "validationResults"));
        cacheManager.setCaffeine(Caffeine.<Object, Object>newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }

    /**
     * Cache persistente de longa duração para dados estáveis
     */
    @Bean
    public CacheManager longLivedCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(
                Arrays.asList("productCatalog", "systemConfigs"));
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(50)
                .maximumSize(500)
                .expireAfterWrite(12, TimeUnit.HOURS)
                .recordStats());
        return cacheManager;
    }

    /**
     * Cache específico para verificação de duplicidade
     * Otimizado para rápida verificação e evitar processamento duplicado
     */
    @Bean
    public CacheManager duplicateCheckCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(
                Arrays.asList("orderNumbers", "orderHashes"));
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(10000)
                .maximumSize(200000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats());
        return cacheManager;
    }
}