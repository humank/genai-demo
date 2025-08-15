package solid.humank.genaidemo.infrastructure.inventory.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import solid.humank.genaidemo.application.inventory.service.InventoryApplicationService;

/** 庫存排程器 負責定期執行庫存相關的任務 */
@Component
public class InventoryScheduler {

    @SuppressWarnings("unused") // 將在未來的實現中使用
    private final InventoryApplicationService inventoryService;

    public InventoryScheduler(InventoryApplicationService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /** 每小時同步庫存 */
    @Scheduled(cron = "0 0 * * * *") // 每小時執行一次
    public void synchronizeInventory() {
        try {
            // 模擬庫存同步邏輯
            System.out.println("開始同步庫存數據...");

            // 這裡應該調用外部倉庫系統API
            // 簡化實現：模擬同步過程
            Thread.sleep(1000); // 模擬網路延遲

            System.out.println("庫存同步完成");
        } catch (Exception e) {
            System.err.println("庫存同步失敗: " + e.getMessage());
        }
    }

    /** 每天檢查並通知低庫存 */
    @Scheduled(cron = "0 0 9 * * *") // 每天上午9點執行
    public void checkAndNotifyLowStock() {
        try {
            // 模擬低庫存檢查邏輯
            System.out.println("開始檢查低庫存商品...");

            // 簡化實現：模擬檢查過程
            String[] lowStockProducts = {"PROD001", "PROD003"};

            for (String productId : lowStockProducts) {
                System.out.println("警告：商品 " + productId + " 庫存不足");
                // 這裡應該發送通知給相關人員
            }

            System.out.println("低庫存檢查完成");
        } catch (Exception e) {
            System.err.println("低庫存檢查失敗: " + e.getMessage());
        }
    }
}
