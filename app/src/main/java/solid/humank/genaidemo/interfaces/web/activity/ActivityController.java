package solid.humank.genaidemo.interfaces.web.activity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import solid.humank.genaidemo.application.common.dto.StandardErrorResponse;

/** 活動記錄控制器 提供系統活動記錄的 API 端點 */
@RestController
@RequestMapping("/api/activities")
@Tag(name = "活動記錄", description = "系統活動記錄管理 API，提供查詢和篩選系統中各種業務活動的功能")
public class ActivityController {

    /** 獲取活動記錄列表 */
    @GetMapping
    @Operation(
            summary = "獲取活動記錄列表",
            description = "查詢系統中的活動記錄，支援限制返回數量。活動記錄包含訂單狀態更新、支付處理、庫存變動等各種業務操作的歷史記錄。")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "成功獲取活動記錄列表",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        example =
                                                                """
                    {
                      "success": true,
                      "data": [
                        {
                          "id": "order-550e8400-e29b-41d4-a716-446655440001",
                          "type": "order",
                          "title": "訂單狀態更新",
                          "description": "客戶 王淑芬 的訂單 550e8400-e29b-41d4-a716-446655440001",
                          "timestamp": "587 天前",
                          "status": "info",
                          "metadata": {
                            "orderId": "550e8400-e29b-41d4-a716-446655440001",
                            "customerId": "660e8400-e29b-41d4-a716-446655440001",
                            "amount": 6420.0
                          }
                        }
                      ]
                    }
                    """))),
                @ApiResponse(
                        responseCode = "400",
                        description = "請求參數無效",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "系統內部錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    public ResponseEntity<Map<String, Object>> getActivities(
            @Parameter(
                            description = "限制返回的活動記錄數量，用於分頁控制",
                            example = "10",
                            schema = @Schema(minimum = "1", maximum = "100"))
                    @RequestParam(defaultValue = "10")
                    int limit) {

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
