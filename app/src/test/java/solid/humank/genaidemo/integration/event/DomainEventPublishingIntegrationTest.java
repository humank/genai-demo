package solid.humank.genaidemo.integration.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.events.OrderCreatedEvent;
import solid.humank.genaidemo.domain.order.model.events.OrderItemAddedEvent;
import solid.humank.genaidemo.domain.order.model.events.OrderSubmittedEvent;
import solid.humank.genaidemo.domain.order.service.OrderEventHandler;
import solid.humank.genaidemo.infrastructure.event.DomainEventPublisherAdapter;
import solid.humank.genaidemo.testutils.annotations.IntegrationTest;
import solid.humank.genaidemo.testutils.fixtures.TestConstants;

/** 領域事件發布整合測試 重構後使用測試輔助工具，改善測試結構和可讀性 */
@SpringBootTest
@IntegrationTest
public class DomainEventPublishingIntegrationTest {

    @Autowired private DomainEventPublisherAdapter eventPublisherAdapter;

    @Autowired private OrderEventHandler orderEventHandler;

    private List<DomainEvent> capturedEvents;

    @BeforeEach
    public void setup() {
        capturedEvents = new ArrayList<>();
        setupEventCapture();
    }

    private void setupEventCapture() {
        doAnswer(
                        invocation -> {
                            DomainEvent event = invocation.getArgument(0);
                            capturedEvents.add(event);
                            return invocation.callRealMethod();
                        })
                .when(eventPublisherAdapter)
                .publish(any(DomainEvent.class));
    }

    @Test
    @DisplayName("應該正確發布和處理OrderCreatedEvent")
    public void shouldPublishAndHandleOrderCreatedEvent() {
        // Arrange
        OrderCreatedEvent event = createOrderCreatedEvent();

        // Act
        eventPublisherAdapter.publish(event);

        // Assert
        assertEquals(1, capturedEvents.size(), "Should capture exactly one event");
        assertTrue(
                capturedEvents.get(0) instanceof OrderCreatedEvent,
                "Captured event should be OrderCreatedEvent");
        verify(orderEventHandler, timeout(1000)).handleOrderCreated(any(OrderCreatedEvent.class));
    }

    @Test
    @DisplayName("應該正確發布和處理多個事件")
    public void shouldPublishAndHandleMultipleEvents() {
        // Arrange
        OrderId orderId = OrderId.generate();
        OrderCreatedEvent createdEvent = createOrderCreatedEvent(orderId);
        OrderItemAddedEvent itemAddedEvent = createOrderItemAddedEvent(orderId);

        // Act
        eventPublisherAdapter.publish(createdEvent);
        eventPublisherAdapter.publish(itemAddedEvent);

        // Assert
        assertEquals(2, capturedEvents.size(), "Should capture exactly two events");
        verify(orderEventHandler, timeout(1000)).handleOrderCreated(any(OrderCreatedEvent.class));
        verify(orderEventHandler, timeout(1000))
                .handleOrderItemAdded(any(OrderItemAddedEvent.class));
    }

    @Test
    @DisplayName("應該按順序處理事件")
    public void shouldProcessEventsInOrder() {
        // Arrange
        OrderId orderId = OrderId.generate();
        OrderCreatedEvent createdEvent = createOrderCreatedEvent(orderId);
        OrderItemAddedEvent itemAddedEvent = createOrderItemAddedEvent(orderId);
        OrderSubmittedEvent submittedEvent = createOrderSubmittedEvent(orderId);

        // Act
        eventPublisherAdapter.publish(createdEvent);
        eventPublisherAdapter.publish(itemAddedEvent);
        eventPublisherAdapter.publish(submittedEvent);

        // Assert
        verifyEventHandlersCalledInOrder();
        verifyEventsCapturedInCorrectOrder();
    }

    // 輔助方法

    private OrderCreatedEvent createOrderCreatedEvent() {
        return createOrderCreatedEvent(OrderId.generate());
    }

    private OrderCreatedEvent createOrderCreatedEvent(OrderId orderId) {
        return new OrderCreatedEvent(
                orderId,
                TestConstants.Customer.DEFAULT_ID,
                TestConstants.MoneyAmounts.MEDIUM_AMOUNT,
                Collections.emptyList());
    }

    private OrderItemAddedEvent createOrderItemAddedEvent(OrderId orderId) {
        return new OrderItemAddedEvent(
                orderId,
                TestConstants.Product.DEFAULT_ID,
                TestConstants.Order.DEFAULT_QUANTITY,
                Money.of(TestConstants.Product.DEFAULT_PRICE));
    }

    private OrderSubmittedEvent createOrderSubmittedEvent(OrderId orderId) {
        return new OrderSubmittedEvent(
                orderId,
                TestConstants.Customer.DEFAULT_ID,
                Money.of(TestConstants.Product.DEFAULT_PRICE),
                TestConstants.Order.DEFAULT_QUANTITY);
    }

    private void verifyEventHandlersCalledInOrder() {
        verify(orderEventHandler, timeout(1000)).handleOrderCreated(any(OrderCreatedEvent.class));
        verify(orderEventHandler, timeout(1000))
                .handleOrderItemAdded(any(OrderItemAddedEvent.class));
        verify(orderEventHandler, timeout(1000))
                .handleOrderSubmitted(any(OrderSubmittedEvent.class));
    }

    private void verifyEventsCapturedInCorrectOrder() {
        assertEquals(3, capturedEvents.size(), "Should capture exactly three events");
        assertTrue(
                capturedEvents.get(0) instanceof OrderCreatedEvent,
                "First event should be OrderCreatedEvent");
        assertTrue(
                capturedEvents.get(1) instanceof OrderItemAddedEvent,
                "Second event should be OrderItemAddedEvent");
        assertTrue(
                capturedEvents.get(2) instanceof OrderSubmittedEvent,
                "Third event should be OrderSubmittedEvent");
    }
}
