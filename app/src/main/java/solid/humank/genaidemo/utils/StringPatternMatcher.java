package solid.humank.genaidemo.utils;

import java.util.regex.Pattern;

/** 字符串模式匹配工具類 展示 Java 21 的 Pattern Matching 功能 */
public class StringPatternMatcher {

    /**
     * 檢查字符串是否為有效的電子郵件地址
     *
     * @param input 輸入字符串
     * @return 是否為有效的電子郵件地址
     */
    public static boolean isValidEmail(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }

        // 簡單的電子郵件正則表達式
        Pattern emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
        return emailPattern.matcher(input).matches();
    }

    /**
     * 檢查字符串是否為有效的電話號碼
     *
     * @param input 輸入字符串
     * @return 是否為有效的電話號碼
     */
    public static boolean isValidPhoneNumber(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }

        // 簡單的電話號碼正則表達式
        Pattern phonePattern = Pattern.compile("^\\d{10}$");
        return phonePattern.matcher(input).matches();
    }

    /**
     * 檢查字符串是否為有效的身份證號碼
     *
     * @param input 輸入字符串
     * @return 是否為有效的身份證號碼
     */
    public static boolean isValidIdNumber(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }

        // 簡單的身份證號碼正則表達式
        Pattern idPattern = Pattern.compile("^[A-Z]\\d{9}$");
        return idPattern.matcher(input).matches();
    }

    /**
     * 檢查字符串是否為有效的 URL
     *
     * @param input 輸入字符串
     * @return 是否為有效的 URL
     */
    public static boolean isValidUrl(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }

        // 簡單的 URL 正則表達式
        Pattern urlPattern = Pattern.compile("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$");
        return urlPattern.matcher(input).matches();
    }

    /**
     * 使用 Java 21 的 Pattern Matching 檢查輸入類型
     *
     * @param input 輸入對象
     * @return 描述輸入類型的字符串
     */
    public static String checkInputType(Object input) {
        return switch (input) {
            case null -> "輸入為 null";
            case String s when s.isBlank() -> "輸入為空字符串";
            case String s when isValidEmail(s) -> "輸入為有效的電子郵件地址: " + s;
            case String s when isValidPhoneNumber(s) -> "輸入為有效的電話號碼: " + s;
            case String s when isValidIdNumber(s) -> "輸入為有效的身份證號碼: " + s;
            case String s when isValidUrl(s) -> "輸入為有效的 URL: " + s;
            case String s -> "輸入為普通字符串: " + s;
            case Integer i -> "輸入為整數: " + i;
            case Double d -> "輸入為浮點數: " + d;
            case Boolean b -> "輸入為布爾值: " + b;
            case Object[] arr -> "輸入為數組，長度為: " + arr.length;
            case java.util.List<?> list -> "輸入為列表，大小為: " + list.size();
            case java.util.Map<?, ?> map -> "輸入為映射，大小為: " + map.size();
            default -> "輸入為其他類型: " + input.getClass().getSimpleName();
        };
    }

    /**
     * 使用 Java 21 的 Pattern Matching 解析輸入
     *
     * @param input 輸入對象
     * @return 解析結果
     */
    public static String parseInput(Object input) {
        if (input instanceof String text) {
            if (text.contains("@")) {
                return "可能是電子郵件: " + text;
            } else if (text.matches("\\d+")) {
                return "可能是數字字符串: " + text;
            } else {
                return "普通文本: " + text;
            }
        } else if (input instanceof Number n) {
            if (n instanceof Integer i) {
                return "整數值: " + i;
            } else if (n instanceof Double d) {
                return "浮點數值: " + d;
            } else {
                return "其他數值類型: " + n;
            }
        } else if (input instanceof Boolean b) {
            return "布爾值: " + b;
        } else {
            return "不支持的類型: " + (input != null ? input.getClass().getSimpleName() : "null");
        }
    }
}
