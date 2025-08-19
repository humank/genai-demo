package solid.humank.genaidemo.domain.common.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 簡單的領域事件收集器實作 純 POJO 實作，不依賴任何框架 用於聚合根內部收集領域事件 */
public class SimpleDomainEventCollector implements DomainEventCollector {

    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    @Override
    public void collectEvent(DomainEvent event) {
        Objects.requireNonNull(event, "Event cannot be null");
        uncommittedEvents.add(event);
    }

    @Override
    public List<DomainEvent> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }

    @Override
    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }

    @Override
    public boolean hasUncommittedEvents() {
        return !uncommittedEvents.isEmpty();
    }
}
