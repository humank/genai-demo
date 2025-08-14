package solid.humank.genaidemo.domain.workflow.model.valueobject;

import java.util.Objects;
import java.util.UUID;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 工作流ID值對象 */
@ValueObject
public class WorkflowId {
    private final UUID id;

    private WorkflowId(UUID id) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
    }

    /**
     * 生成新的工作流ID
     *
     * @return 新的工作流ID
     */
    public static WorkflowId generate() {
        return new WorkflowId(UUID.randomUUID());
    }

    /**
     * 從UUID創建工作流ID
     *
     * @param id UUID
     * @return 工作流ID
     */
    public static WorkflowId fromUUID(UUID id) {
        return new WorkflowId(id);
    }

    /**
     * 從字符串創建工作流ID
     *
     * @param id 字符串ID
     * @return 工作流ID
     */
    public static WorkflowId fromString(String id) {
        return new WorkflowId(UUID.fromString(id));
    }

    /**
     * 獲取UUID
     *
     * @return UUID
     */
    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowId that = (WorkflowId) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
