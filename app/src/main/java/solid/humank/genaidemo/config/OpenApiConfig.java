package solid.humank.genaidemo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI 配置類 配置 API 文檔的基本資訊、標籤和伺服器資訊 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:genai-demo}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("GenAI Demo API")
                                .description("基於 DDD 架構的電商平台 API，提供完整的消費者和商務功能")
                                .version("v2.0.0")
                                .contact(
                                        new Contact()
                                                .name("Development Team")
                                                .email("dev@genai-demo.com")
                                                .url("https://github.com/humank/genai-demo"))
                                .license(
                                        new License()
                                                .name("MIT License")
                                                .url("https://opensource.org/licenses/MIT")))
                .servers(
                        List.of(
                                new Server()
                                        .url("http://localhost:" + serverPort)
                                        .description("本地開發環境"),
                                new Server()
                                        .url("http://localhost:8080")
                                        .description("Docker 容器環境")))
                .tags(
                        List.of(
                                // 客戶端 API 標籤 (Customer-facing)
                                new Tag().name("產品瀏覽").description("客戶端商品瀏覽 API - 商品查詢、搜尋、詳情、分類"),
                                new Tag().name("購物車").description("購物車功能 API - 商品加入、移除、數量調整、結帳計算"),
                                new Tag().name("訂單查詢").description("客戶訂單 API - 個人訂單查詢、狀態追蹤、歷史記錄"),
                                new Tag().name("支付處理").description("客戶支付 API - 支付發起、狀態查詢、支付方式管理"),
                                new Tag().name("促銷活動").description("促銷查詢 API - 可用促銷、優惠券領取、使用規則"),
                                new Tag().name("商品評價").description("評價系統 API - 評價查詢、提交評價、評價統計"),
                                new Tag().name("個人化推薦").description("推薦系統 API - 個人化商品推薦、相關商品、熱門商品"),
                                new Tag().name("通知中心").description("客戶通知 API - 個人通知、狀態更新、偏好設定"),
                                new Tag().name("配送追蹤").description("配送狀態 API - 即時追蹤、配送資訊、簽收確認"),

                                // 運營管理 API 標籤 (Operator-facing)
                                new Tag().name("客戶管理").description("客戶管理 API - 客戶資料管理、會員等級、行為分析"),
                                new Tag().name("訂單管理").description("訂單管理 API - 全平台訂單管理、狀態更新、批次處理"),
                                new Tag().name("商品管理").description("商品管理 API - 商品 CRUD、上下架、分類管理"),
                                new Tag().name("庫存管理").description("庫存管理 API - 庫存查詢、調整、預留、盤點"),
                                new Tag().name("定價管理").description("定價策略 API - 價格規則、折扣設定、佣金管理"),
                                new Tag().name("支付管理").description("支付管理 API - 交易管理、退款處理、對帳報表"),
                                new Tag().name("統計報表").description("數據統計 API - 銷售統計、用戶分析、效能指標"),
                                new Tag().name("活動記錄").description("系統活動 API - 操作日誌、審計追蹤、活動查詢"),

                                // 系統管理 API 標籤 (System-facing)
                                new Tag().name("系統監控").description("系統管理 API - 健康檢查、效能監控、系統狀態"),
                                new Tag().name("內部整合").description("內部系統 API - 系統間整合、資料同步、內部服務")));
    }
}
