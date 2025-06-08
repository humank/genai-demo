package solid.humank.genaidemo.domain.product.model.entity;

import solid.humank.genaidemo.domain.product.model.valueobject.BundleDiscount;
import solid.humank.genaidemo.domain.product.model.valueobject.BundleType;

/**
 * 捆綁銷售實體
 */
public class Bundle {
    private String name;
    private BundleType type;
    private BundleDiscount discount;
    private int requiredItemCount; // 對於自選捆綁，需要選擇的產品數量

    public Bundle(String name, BundleType type, BundleDiscount discount) {
        this.name = name;
        this.type = type;
        this.discount = discount;
        this.requiredItemCount = 1; // 預設值
    }

    public Bundle(String name, BundleType type, BundleDiscount discount, int requiredItemCount) {
        this.name = name;
        this.type = type;
        this.discount = discount;
        this.requiredItemCount = requiredItemCount;
    }

    public String getName() {
        return name;
    }

    public BundleType getType() {
        return type;
    }

    public BundleDiscount getDiscount() {
        return discount;
    }

    public int getRequiredItemCount() {
        return requiredItemCount;
    }

    public void setRequiredItemCount(int requiredItemCount) {
        this.requiredItemCount = requiredItemCount;
    }

    public boolean isFixedBundle() {
        return type == BundleType.FIXED_BUNDLE;
    }

    public boolean isPickAnyBundle() {
        return type == BundleType.PICK_ANY_BUNDLE;
    }
}