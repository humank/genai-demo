package solid.humank.genaidemo.domain.common.valueobject;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * 金錢格式化工具類
 * 使用 Java 21 的 String Templates 功能
 */
public class MoneyFormatter {
    
    /**
     * 格式化金錢為本地化字符串
     * 
     * @param money 金錢值對象
     * @param locale 地區設置
     * @return 格式化後的字符串
     */
    public static String format(Money money, Locale locale) {
        var currencyCode = money.getCurrency().getCurrencyCode();
        var amount = money.getAmount();
        var formatter = NumberFormat.getCurrencyInstance(locale);
        formatter.setCurrency(money.getCurrency());
        
        // 使用 Java 21 的 String Templates
        return STR."金額: \{formatter.format(amount)} (\{currencyCode})";
    }
    
    /**
     * 格式化金錢為本地化字符串，使用默認地區
     * 
     * @param money 金錢值對象
     * @return 格式化後的字符串
     */
    public static String format(Money money) {
        return format(money, Locale.getDefault());
    }
    
    /**
     * 格式化金錢為台灣地區格式
     * 
     * @param money 金錢值對象
     * @return 格式化後的字符串
     */
    public static String formatTW(Money money) {
        return format(money, Locale.TAIWAN);
    }
    
    /**
     * 創建金錢摘要信息
     * 
     * @param description 描述
     * @param money 金錢值對象
     * @return 摘要信息
     */
    public static String summarize(String description, Money money) {
        var formattedAmount = format(money);
        // 使用 Java 21 的 String Templates 和多行字符串
        return STR."""
            交易摘要:
            說明: \{description}
            金額: \{formattedAmount}
            時間: \{java.time.LocalDateTime.now()}
            """;
    }
}