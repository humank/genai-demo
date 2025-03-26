package solid.humank.genaidemo.utils;

import java.util.Objects;

/**
 * 參數驗證工具類
 * 集中共用的參數驗證邏輯，減少重複代碼
 */
public final class Preconditions {
    
    private Preconditions() {
        // 防止實例化
    }
    
    /**
     * 確保字符串不為空（null 或空字符串）
     * 
     * @param value 要檢查的字符串
     * @param message 錯誤信息
     * @return 輸入的字符串，方便鏈式調用
     * @throws IllegalArgumentException 如果字符串為空
     */
    public static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
    
    /**
     * 確保對象不為 null
     * 
     * @param <T> 對象類型
     * @param value 要檢查的對象
     * @param message 錯誤信息
     * @return 輸入的對象，方便鏈式調用
     * @throws IllegalArgumentException 如果對象為 null
     */
    public static <T> T requireNonNull(T value, String message) {
        return Objects.requireNonNull(value, message);
    }
    
    /**
     * 確保條件為真
     * 
     * @param condition 要檢查的條件
     * @param message 錯誤信息
     * @throws IllegalArgumentException 如果條件為假
     */
    public static void checkArgument(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * 確保數值符合要求（大於 0）
     * 
     * @param value 要檢查的數值
     * @param message 錯誤信息
     * @return 輸入的數值，方便鏈式調用
     * @throws IllegalArgumentException 如果數值不大於 0
     */
    public static int requirePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
