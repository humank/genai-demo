package solid.humank.genaidemo.domain.order.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 工作流ID值對象 - 使用 Record 實作 */
@ValueObject
public record WorkflowId(UUID value) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public WorkflowId {
        Objects.requireNonNull(value, "ID cannot be null");
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
     * 從字符串創建工作流ID
     *
     * @param id 字符串ID
     * @return 工作流ID
     */
    public static WorkflowId of(String id) {
        return new WorkflowId(UUID.fromString(id));
    }

    /**
     * 獲取UUID（向後相容方法）
     *
     * @return UUID
     */
    public UUID getId() {
        return value;
    }

    /**
     * 獲取ID值
     *
     * @return ID值
     */
    public String getValue() {
        return value.toString();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
