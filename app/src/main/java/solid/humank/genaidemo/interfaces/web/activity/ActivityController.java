package solid.humank.genaidemo.interfaces.web.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 活動記錄控制器 提供系統活動記錄的 API 端點 */
@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    /** 獲取活動記錄列表 */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getActivities(
            @RequestParam(defaultValue = "10") int limit) {

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> activities = new ArrayList<>();

        // 創建模擬活動數據
        for (int i = 1; i <= Math.min(limit, 5); i++) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("id", "order-550e8400-e29b-41d4-a716-44665544000" + i);
            activity.put("type", "order");
            activity.put("title", "訂單狀態更新");
            activity.put("description", "客戶 王淑芬 的訂單 550e8400-e29b-41d4-a716-44665544000" + i);
            activity.put("timestamp", (586 + i) + " 天前");
            activity.put("status", "info");

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("orderId", "550e8400-e29b-41d4-a716-44665544000" + i);
            metadata.put("customerId", "660e8400-e29b-41d4-a716-44665544000" + i);
            metadata.put("amount", 5420.0 + i * 1000);
            activity.put("metadata", metadata);

            activities.add(activity);
        }

        response.put("success", true);
        response.put("data", activities);

        return ResponseEntity.ok(response);
    }
}
