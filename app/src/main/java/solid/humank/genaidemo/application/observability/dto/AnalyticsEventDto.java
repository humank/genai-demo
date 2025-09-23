package solid.humank.genaidemo.application.observability.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 分析事件 DTO
 * 
 * 用於接收前端發送的用戶行為分析事件數據。
 * 包含事件的基本資訊和相關數據。
 * 
 * 需求: 1.1, 1.2, 2.1
 */
public record AnalyticsEventDto(
        @NotBlank(message = "Event ID cannot be blank") String eventId,

        @NotBlank(message = "Event type cannot be blank") String eventType,

        String userId, // 可選，支援匿名用戶

        @NotNull(message = "Event data cannot be null") Map<String, Object> data,

        Long timestamp // 前端時間戳
) {

    /**
     * 創建分析事件 DTO
     * 
     * @param eventId   事件 ID
     * @param eventType 事件類型
     * @param userId    用戶 ID（可選）
     * @param data      事件數據
     * @return 分析事件 DTO
     */
    public static AnalyticsEventDto create(String eventId, String eventType,
            String userId, Map<String, Object> data) {
        return new AnalyticsEventDto(eventId, eventType, userId, data, System.currentTimeMillis());
    }

    /**
     * 創建匿名用戶分析事件 DTO
     * 
     * @param eventId   事件 ID
     * @param eventType 事件類型
     * @param data      事件數據
     * @return 分析事件 DTO
     */
    public static AnalyticsEventDto createAnonymous(String eventId, String eventType,
            Map<String, Object> data) {
        return new AnalyticsEventDto(eventId, eventType, null, data, System.currentTimeMillis());
    }

    /**
     * 檢查是否為匿名用戶事件
     * 
     * @return 如果是匿名用戶返回 true
     */
    public boolean isAnonymous() {
        return userId == null || userId.trim().isEmpty();
    }

    /**
     * 獲取事件數據中的特定值
     * 
     * @param key 數據鍵
     * @return 數據值，如果不存在返回 null
     */
    public Object getDataValue(String key) {
        return data != null ? data.get(key) : null;
    }

    /**
     * 獲取頁面資訊（如果是頁面相關事件）
     * 
     * @return 頁面路徑
     */
    public String getPage() {
        Object page = getDataValue("page");
        return page != null ? page.toString() : null;
    }

    /**
     * 獲取用戶操作資訊（如果是操作事件）
     * 
     * @return 操作類型
     */
    public String getAction() {
        Object action = getDataValue("action");
        return action != null ? action.toString() : null;
    }

    /**
     * 檢查是否為頁面瀏覽事件
     * 
     * @return 如果是頁面瀏覽事件返回 true
     */
    public boolean isPageViewEvent() {
        return "page_view".equals(eventType);
    }

    /**
     * 檢查是否為用戶操作事件
     * 
     * @return 如果是用戶操作事件返回 true
     */
    public boolean isUserActionEvent() {
        return "user_action".equals(eventType);
    }

    /**
     * 檢查是否為業務事件
     * 
     * @return 如果是業務事件返回 true
     */
    public boolean isBusinessEvent() {
        return "business_event".equals(eventType);
    }
}