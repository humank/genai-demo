package solid.humank.genaidemo.domain.inventory.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 庫存狀態值對象
 */
@ValueObject
public enum InventoryStatus {
    ACTIVE("活躍"),
    INACTIVE("非活躍"),
    DISCONTINUED("已停產");

    private final String description;

    InventoryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}