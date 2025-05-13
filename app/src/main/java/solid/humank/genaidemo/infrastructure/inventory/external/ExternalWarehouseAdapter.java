package solid.humank.genaidemo.infrastructure.inventory.external;

import org.springframework.stereotype.Component;
import solid.humank.genaidemo.application.inventory.port.outgoing.ExternalWarehousePort;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 外部倉庫適配器
 * 實現外部倉庫端口，與外部倉庫系統交互
 */
@Component
public class ExternalWarehouseAdapter implements ExternalWarehousePort {

    private final Random random = new Random();

    @Override
    public int getProductStock(String productId) {
        // 模擬從外部系統獲取庫存數量
        // 實際應用中，這裡會調用外部API或服務
        return 10 + random.nextInt(90); // 返回10-99之間的隨機數
    }

    @Override
    public Map<String, Integer> getProductsStock(Iterable<String> productIds) {
        // 模擬從外部系統獲取多個產品的庫存數量
        Map<String, Integer> result = new HashMap<>();
        for (String productId : productIds) {
            result.put(productId, getProductStock(productId));
        }
        return result;
    }

    @Override
    public boolean synchronizeInventory() {
        // 模擬同步庫存數據
        // 實際應用中，這裡會調用外部API或服務
        try {
            Thread.sleep(100); // 模擬網絡延遲
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean notifyLowStock(String productId, int currentQuantity, int threshold) {
        // 模擬通知庫存低於閾值
        // 實際應用中，這裡會調用外部API或服務
        System.out.printf("低庫存警告: 產品 %s 當前庫存 %d，低於閾值 %d%n",
                productId, currentQuantity, threshold);
        return true;
    }
}