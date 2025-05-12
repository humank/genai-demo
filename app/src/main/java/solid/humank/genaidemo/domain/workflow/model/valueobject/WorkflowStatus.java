package solid.humank.genaidemo.domain.workflow.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 工作流狀態值對象
 */
@ValueObject
public enum WorkflowStatus {
    CREATED("已創建"),
    PENDING_PAYMENT("待支付"),
    CONFIRMED("已確認"),
    PROCESSING("處理中"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    FAILED("失敗");

    private final String description;

    WorkflowStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}