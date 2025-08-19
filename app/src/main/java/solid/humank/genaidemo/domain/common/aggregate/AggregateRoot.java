package solid.humank.genaidemo.domain.common.aggregate;

/**
 * 聚合根抽象基類
 * 
 * 提供零 override 的事件管理功能，實作者無需重寫任何方法即可獲得完整的事件管理能力。
 * 
 * 設計理念：
 * 1. 零 override：實作者無需重寫任何方法
 * 2. 註解驅動：通過 @AggregateRoot 註解提供元數據
 * 3. 自動驗證：自動檢查註解配置
 * 4. 純領域：不依賴任何基礎設施框架
 * 
 * 使用方式：
 * 
 * @AggregateRoot(name = "Customer", boundedContext = "Customer")
 *                     public class Customer extends AggregateRoot {
 *                     // 無需 override 任何方法！
 * 
 *                     public void updateProfile(String name) {
 *                     // 業務邏輯
 *                     this.name = name;
 * 
 *                     // 直接調用事件收集
 *                     collectEvent(new CustomerProfileUpdatedEvent(id, name));
 *                     }
 *                     }
 */
public abstract class AggregateRoot implements AggregateRootInterface {

    // 所有方法都由 AggregateRootInterface 的 default 方法提供
    // 實作者無需 override 任何方法

    /**
     * 獲取聚合根的唯一標識
     * 子類別可以選擇性地重寫此方法來提供特定的 ID 實作
     * 
     * @return 聚合根的唯一標識，預設返回 null（子類別應該重寫）
     */
    public Object getId() {
        return null; // 子類別可以重寫此方法
    }

    /**
     * 檢查兩個聚合根是否相等
     * 基於 ID 進行比較
     * 
     * @param obj 要比較的物件
     * @return 如果 ID 相同則返回 true
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        AggregateRoot that = (AggregateRoot) obj;
        Object thisId = getId();
        Object thatId = that.getId();

        return thisId != null && thisId.equals(thatId);
    }

    /**
     * 計算聚合根的雜湊碼
     * 基於 ID 計算
     * 
     * @return 雜湊碼
     */
    @Override
    public int hashCode() {
        Object id = getId();
        return id != null ? id.hashCode() : 0;
    }

    /**
     * 聚合根的字串表示
     * 
     * @return 包含類別名稱和 ID 的字串
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + getId() + "}";
    }
}