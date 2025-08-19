package solid.humank.genaidemo.domain.pricing.model.entity;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.pricing.model.valueobject.ProductCategory;

/**
 * 佣金費率實體
 * 管理不同產品類別的佣金費率
 */
@Entity(name = "CommissionRate", description = "佣金費率實體，管理不同產品類別的佣金費率")
public class CommissionRate {
    private final UUID id;
    private ProductCategory category;
    private int normalRate; // 一般費率（百分比）
    private int eventRate; // 活動費率（百分比）

    public CommissionRate(ProductCategory category, int normalRate, int eventRate) {
        this.id = UUID.randomUUID();
        this.category = category;
        this.normalRate = normalRate;
        this.eventRate = eventRate;
    }

    /**
     * 建立佣金費率（不指定產品類別）
     *
     * @param normalRate 一般費率
     * @param eventRate  活動費率
     */
    public CommissionRate(int normalRate, int eventRate) {
        this.id = UUID.randomUUID();
        this.category = ProductCategory.GENERAL;
        this.normalRate = normalRate;
        this.eventRate = eventRate;
    }

    /**
     * 重建構造函數（用於從持久化層重建實體）
     */
    public CommissionRate(UUID id, ProductCategory category, int normalRate, int eventRate) {
        this.id = id;
        this.category = category;
        this.normalRate = normalRate;
        this.eventRate = eventRate;
    }

    public UUID getId() {
        return id;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public int getNormalRate() {
        return normalRate;
    }

    public void setNormalRate(int normalRate) {
        if (normalRate < 0 || normalRate > 100) {
            throw new IllegalArgumentException("佣金費率必須在 0-100 之間");
        }
        this.normalRate = normalRate;
    }

    public int getEventRate() {
        return eventRate;
    }

    public void setEventRate(int eventRate) {
        if (eventRate < 0 || eventRate > 100) {
            throw new IllegalArgumentException("佣金費率必須在 0-100 之間");
        }
        this.eventRate = eventRate;
    }

    /**
     * 獲取當前有效的佣金費率
     * 
     * @param isEventPeriod 是否為活動期間
     * @return 有效的佣金費率
     */
    public int getEffectiveRate(boolean isEventPeriod) {
        return isEventPeriod ? eventRate : normalRate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CommissionRate that = (CommissionRate) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CommissionRate{" +
                "id=" + id +
                ", category=" + category +
                ", normalRate=" + normalRate +
                ", eventRate=" + eventRate +
                '}';
    }
}
