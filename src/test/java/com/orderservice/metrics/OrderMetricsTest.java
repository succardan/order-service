package com.orderservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderMetricsTest {

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter ordersReceivedCounter;

    @Mock
    private Counter ordersProcessedCounter;

    @Mock
    private Counter ordersNotifiedCounter;

    @Mock
    private Counter ordersErrorCounter;

    @Mock
    private Counter duplicateOrdersCounter;

    @Mock
    private Timer orderCreationTimer;

    @Mock
    private Timer orderProcessingTimer;

    @Mock
    private Timer orderNotificationTimer;

    @Mock
    private Timer externalServiceATimer;

    @Mock
    private Timer externalServiceBTimer;

    @InjectMocks
    private OrderMetrics orderMetrics;

    @BeforeEach
    void setUp() {
        orderMetrics = new OrderMetrics(meterRegistry);

        orderMetrics.ordersReceivedCounter = ordersReceivedCounter;
        orderMetrics.ordersProcessedCounter = ordersProcessedCounter;
        orderMetrics.ordersNotifiedCounter = ordersNotifiedCounter;
        orderMetrics.ordersErrorCounter = ordersErrorCounter;
        orderMetrics.duplicateOrdersCounter = duplicateOrdersCounter;
        orderMetrics.orderCreationTimer = orderCreationTimer;
        orderMetrics.orderProcessingTimer = orderProcessingTimer;
        orderMetrics.orderNotificationTimer = orderNotificationTimer;
        orderMetrics.externalServiceATimer = externalServiceATimer;
        orderMetrics.externalServiceBTimer = externalServiceBTimer;
    }

    @Test
    void incrementOrdersReceived_ShouldIncrementCounter() {
        orderMetrics.incrementOrdersReceived();

        verify(ordersReceivedCounter).increment();
    }

    @Test
    void incrementOrdersProcessed_ShouldIncrementCounter() {
        orderMetrics.incrementOrdersProcessed();

        verify(ordersProcessedCounter).increment();
    }

    @Test
    void incrementOrdersNotified_ShouldIncrementCounter() {
        orderMetrics.incrementOrdersNotified();

        verify(ordersNotifiedCounter).increment();
    }

    @Test
    void incrementOrdersError_ShouldIncrementCounter() {
        orderMetrics.incrementOrdersError();

        verify(ordersErrorCounter).increment();
    }

    @Test
    void incrementDuplicateOrders_ShouldIncrementCounter() {
        orderMetrics.incrementDuplicateOrders();

        verify(duplicateOrdersCounter).increment();
    }

    @Test
    void recordOrderCreationTime_ShouldRecordTime() {
        orderMetrics.recordOrderCreationTime(500);

        verify(orderCreationTimer).record(500, TimeUnit.MILLISECONDS);
    }

    @Test
    void recordOrderProcessingTime_ShouldRecordTime() {
        orderMetrics.recordOrderProcessingTime(1000);

        verify(orderProcessingTimer).record(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    void recordOrderNotificationTime_ShouldRecordTime() {
        orderMetrics.recordOrderNotificationTime(300);

        verify(orderNotificationTimer).record(300, TimeUnit.MILLISECONDS);
    }

    @Test
    void recordExternalServiceATime_ShouldRecordTime() {
        orderMetrics.recordExternalServiceATime(200);

        verify(externalServiceATimer).record(200, TimeUnit.MILLISECONDS);
    }

    @Test
    void recordExternalServiceBTime_ShouldRecordTime() {
        orderMetrics.recordExternalServiceBTime(150);

        verify(externalServiceBTimer).record(150, TimeUnit.MILLISECONDS);
    }

    @Test
    void measureOrderCreationTime_ShouldMeasureAndReturnResult() {
        doAnswer(invocation -> {
            long timeInMs = invocation.getArgument(0);
            TimeUnit timeUnit = invocation.getArgument(1);
            assertEquals(TimeUnit.MILLISECONDS, timeUnit);
            return null;
        }).when(orderCreationTimer).record(anyLong(), any(TimeUnit.class));

        String result = orderMetrics.measureOrderCreationTime(() -> "test-result");

        assertEquals("test-result", result);
        verify(orderCreationTimer).record(anyLong(), any(TimeUnit.class));
    }

    @Test
    void measureOrderProcessingTime_ShouldMeasureAndReturnResult() {
        doAnswer(invocation -> {
            long timeInMs = invocation.getArgument(0);
            TimeUnit timeUnit = invocation.getArgument(1);
            assertEquals(TimeUnit.MILLISECONDS, timeUnit);
            return null;
        }).when(orderProcessingTimer).record(anyLong(), any(TimeUnit.class));

        Integer result = orderMetrics.measureOrderProcessingTime(() -> 42);

        assertEquals(42, result);
        verify(orderProcessingTimer).record(anyLong(), any(TimeUnit.class));
    }

    @Test
    void measureOrderNotificationTime_ShouldMeasureAndReturnResult() {
        doAnswer(invocation -> {
            long timeInMs = invocation.getArgument(0);
            TimeUnit timeUnit = invocation.getArgument(1);
            assertEquals(TimeUnit.MILLISECONDS, timeUnit);
            return null;
        }).when(orderNotificationTimer).record(anyLong(), any(TimeUnit.class));

        Boolean result = orderMetrics.measureOrderNotificationTime(() -> true);

        assertTrue(result);
        verify(orderNotificationTimer).record(anyLong(), any(TimeUnit.class));
    }

    @Test
    void measureExternalServiceATime_ShouldMeasureAndReturnResult() {
        doAnswer(invocation -> {
            long timeInMs = invocation.getArgument(0);
            TimeUnit timeUnit = invocation.getArgument(1);
            assertEquals(TimeUnit.MILLISECONDS, timeUnit);
            return null;
        }).when(externalServiceATimer).record(anyLong(), any(TimeUnit.class));

        String result = orderMetrics.measureExternalServiceATime(() -> "service-a-response");

        assertEquals("service-a-response", result);
        verify(externalServiceATimer).record(anyLong(), any(TimeUnit.class));
    }

    @Test
    void measureExternalServiceBTime_ShouldMeasureAndReturnResult() {
        doAnswer(invocation -> {
            long timeInMs = invocation.getArgument(0);
            TimeUnit timeUnit = invocation.getArgument(1);
            assertEquals(TimeUnit.MILLISECONDS, timeUnit);
            return null;
        }).when(externalServiceBTimer).record(anyLong(), any(TimeUnit.class));

        String result = orderMetrics.measureExternalServiceBTime(() -> "service-b-response");

        assertEquals("service-b-response", result);
        verify(externalServiceBTimer).record(anyLong(), any(TimeUnit.class));
    }

    @Test
    void measureOrderCreationTime_ShouldHandleExceptions() {
        doAnswer(invocation -> {
            long timeInMs = invocation.getArgument(0);
            TimeUnit timeUnit = invocation.getArgument(1);
            assertEquals(TimeUnit.MILLISECONDS, timeUnit);
            return null;
        }).when(orderCreationTimer).record(anyLong(), any(TimeUnit.class));

        RuntimeException expectedException = new RuntimeException("Test exception");

        assertThrows(RuntimeException.class, () -> {
            orderMetrics.measureOrderCreationTime(() -> {
                throw expectedException;
            });
        });

        verify(orderCreationTimer).record(anyLong(), any(TimeUnit.class));
    }
}
