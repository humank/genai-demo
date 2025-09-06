package solid.humank.genaidemo.interfaces.web.stats;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import solid.humank.genaidemo.application.common.dto.StandardErrorResponse;
import solid.humank.genaidemo.application.stats.OrderStatusStatsDto;
import solid.humank.genaidemo.application.stats.PaymentMethodStatsDto;
import solid.humank.genaidemo.application.stats.StatsDto;
import solid.humank.genaidemo.application.stats.service.StatisticsApplicationService;

/** 數據統計控制器 用於驗證數據庫中的數據量 */
@RestController
@RequestMapping("/api/stats")
@Tag(name = "統計報表", description = "提供系統各項統計數據和報表功能，包括數據庫統計、訂單狀態分布、支付方式分析等")
public class StatsController {

        private final StatisticsApplicationService statisticsApplicationService;

        public StatsController(StatisticsApplicationService statisticsApplicationService) {
                this.statisticsApplicationService = statisticsApplicationService;
        }

        /** 獲取數據庫統計信息 */
        @GetMapping
        @Operation(summary = "獲取系統統計數據", description = "獲取系統整體統計數據，包括各個數據表的記錄數量、系統狀態等基礎統計信息")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功獲取統計數據", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class), examples = @ExampleObject(name = "統計數據範例", value = """
                                        {
                                            "customers": 150,
                                            "orders": 1250,
                                            "products": 85,
                                            "payments": 1180,
                                            "inventory_items": 320,
                                            "status": "success",
                                            "message": "統計數據獲取成功"
                                        }
                                        """))),
                        @ApiResponse(responseCode = "500", description = "系統內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardErrorResponse.class)))
        })
        public ResponseEntity<Map<String, Object>> getStats() {
                StatsDto statsDto = statisticsApplicationService.getOverallStatistics();

                Map<String, Object> response = new HashMap<>(statsDto.stats());
                response.put("status", statsDto.status());
                response.put("message", statsDto.message());

                return ResponseEntity.ok(response);
        }

        /** 獲取訂單狀態分布 */
        @GetMapping("/order-status")
        @Operation(summary = "獲取訂單狀態分布統計", description = "獲取系統中所有訂單按狀態分布的統計數據，用於分析訂單處理情況和業務狀態")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功獲取訂單狀態統計", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class), examples = @ExampleObject(name = "訂單狀態統計範例", value = """
                                        {
                                            "statusDistribution": {
                                                "PENDING": 45,
                                                "CONFIRMED": 120,
                                                "PROCESSING": 85,
                                                "SHIPPED": 200,
                                                "DELIVERED": 350,
                                                "CANCELLED": 25
                                            },
                                            "status": "success",
                                            "message": "訂單狀態統計獲取成功"
                                        }
                                        """))),
                        @ApiResponse(responseCode = "500", description = "系統內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardErrorResponse.class)))
        })
        public ResponseEntity<Map<String, Object>> getOrderStatusStats() {
                OrderStatusStatsDto statsDto = statisticsApplicationService.getOrderStatusStatistics();

                var result = new HashMap<String, Object>();
                result.put("statusDistribution", statsDto.statusDistribution());
                result.put("status", statsDto.status());
                if (statsDto.message() != null) {
                        result.put("message", statsDto.message());
                }

                return ResponseEntity.ok(result);
        }

        /** 獲取支付方式分布 */
        @GetMapping("/payment-methods")
        @Operation(summary = "獲取支付方式分布統計", description = "獲取系統中所有支付交易按支付方式分布的統計數據，用於分析客戶支付偏好和支付渠道效果")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功獲取支付方式統計", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class), examples = @ExampleObject(name = "支付方式統計範例", value = """
                                        {
                                            "paymentMethodDistribution": {
                                                "CREDIT_CARD": 450,
                                                "DEBIT_CARD": 280,
                                                "BANK_TRANSFER": 150,
                                                "DIGITAL_WALLET": 320,
                                                "CASH_ON_DELIVERY": 95
                                            },
                                            "status": "success",
                                            "message": "支付方式統計獲取成功"
                                        }
                                        """))),
                        @ApiResponse(responseCode = "500", description = "系統內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardErrorResponse.class)))
        })
        public ResponseEntity<Map<String, Object>> getPaymentMethodStats() {
                PaymentMethodStatsDto statsDto = statisticsApplicationService.getPaymentMethodStatistics();

                var result = new HashMap<String, Object>();
                result.put("paymentMethodDistribution", statsDto.paymentMethodDistribution());
                result.put("status", statsDto.status());
                if (statsDto.message() != null) {
                        result.put("message", statsDto.message());
                }

                return ResponseEntity.ok(result);
        }
}
