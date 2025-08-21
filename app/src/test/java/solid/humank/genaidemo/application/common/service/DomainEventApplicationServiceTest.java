package solid.humank.genaidemo.application.common.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import solid.humank.genaidemo.domain.common.aggregate.AggregateRoot;
import solid.humank.genaidemo.domain.common.aggregate.AggregateRootInterface;
import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("DomainEventApplicationService 測試")
class DomainEventApplicationServiceTest {

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private AggregateRoot aggregateRoot;
    @Mock
    private DomainEvent domainEvent1;
    @Mock
    private DomainEvent domainEvent2;

    private DomainEventApplicationService domainEventApplicationService;

    @BeforeEach
    void setUp() {
        domainEventApplicationService = new DomainEventApplicationService(domainEventPublisher);
    }

    @Test
    @DisplayName("應該能夠從聚合根發布事件")
    void shouldPublishEventsFromAggregate() {
        // Given
        List<DomainEvent> events = Arrays.asList(domainEvent1, domainEvent2);
        when(aggregateRoot.hasUncommittedEvents()).thenReturn(true);
        when(aggregateRoot.getUncommittedEvents()).thenReturn(events);

        // When
        domainEventApplicationService.publishEventsFromAggregate(aggregateRoot);

        // Then
        verify(domainEventPublisher).publishAll(events);
        verify(aggregateRoot).markEventsAsCommitted();
    }

    @Test
    @DisplayName("當聚合根沒有未提交事件時不應該發布事件")
    void shouldNotPublishWhenNoUncommittedEvents() {
        // Given
        when(aggregateRoot.hasUncommittedEvents()).thenReturn(false);

        // When
        domainEventApplicationService.publishEventsFromAggregate(aggregateRoot);

        // Then
        verify(domainEventPublisher, never()).publishAll(anyList());
        verify(aggregateRoot, never()).markEventsAsCommitted();
    }

    @Test
    @DisplayName("當聚合根為null時不應該發布事件")
    void shouldNotPublishWhenAggregateRootIsNull() {
        // When
        domainEventApplicationService.publishEventsFromAggregate(null);

        // Then
        verify(domainEventPublisher, never()).publishAll(anyList());
    }

    @Test
    @DisplayName("應該能夠從多個聚合根發布事件")
    void shouldPublishEventsFromMultipleAggregates() {
        // Given
        AggregateRoot aggregateRoot2 = mock(AggregateRoot.class);
        List<AggregateRootInterface> aggregateRoots = Arrays.asList(aggregateRoot, aggregateRoot2);

        List<DomainEvent> events1 = Arrays.asList(domainEvent1);
        List<DomainEvent> events2 = Arrays.asList(domainEvent2);

        when(aggregateRoot.hasUncommittedEvents()).thenReturn(true);
        when(aggregateRoot.getUncommittedEvents()).thenReturn(events1);
        when(aggregateRoot2.hasUncommittedEvents()).thenReturn(true);
        when(aggregateRoot2.getUncommittedEvents()).thenReturn(events2);

        // When
        domainEventApplicationService.publishEventsFromAggregates(aggregateRoots);

        // Then
        verify(domainEventPublisher).publishAll(events1);
        verify(domainEventPublisher).publishAll(events2);
        verify(aggregateRoot).markEventsAsCommitted();
        verify(aggregateRoot2).markEventsAsCommitted();
    }

    @Test
    @DisplayName("當聚合根列表為空時不應該發布事件")
    void shouldNotPublishWhenAggregateRootsListIsEmpty() {
        // When
        domainEventApplicationService.publishEventsFromAggregates(Arrays.asList());

        // Then
        verify(domainEventPublisher, never()).publishAll(anyList());
    }

    @Test
    @DisplayName("當聚合根列表為null時不應該發布事件")
    void shouldNotPublishWhenAggregateRootsListIsNull() {
        // When
        domainEventApplicationService.publishEventsFromAggregates(null);

        // Then
        verify(domainEventPublisher, never()).publishAll(anyList());
    }

    @Test
    @DisplayName("應該能夠同步發布事件")
    void shouldPublishEventsFromAggregateSynchronously() {
        // Given
        List<DomainEvent> events = Arrays.asList(domainEvent1, domainEvent2);
        when(aggregateRoot.hasUncommittedEvents()).thenReturn(true);
        when(aggregateRoot.getUncommittedEvents()).thenReturn(events);

        // When
        domainEventApplicationService.publishEventsFromAggregateSync(aggregateRoot);

        // Then
        verify(domainEventPublisher).publishAll(events);
        verify(aggregateRoot).markEventsAsCommitted();
    }
}
