
# Guidelines

This document詳細說明了將專案從 Java 17 升級到 Java 21 時可以採用的代碼改進，重點關注如何利用 Java 21 的新特性來優化Domain-Driven Design（DDD）實現。

## 目錄

1. [Pattern Matching for instanceof](#1-pattern-matching-for-instanceof)
2. [增強型 Switch 表達式](#2-增強型-switch-表達式)
3. [String Templates](#3-string-templates)
4. [Record 類型](#4-record-類型)
5. [SequencedCollection 接口](#5-sequencedcollection-接口)
6. [虛擬線程](#6-虛擬線程)
7. [Pattern Matching for Switch](#7-pattern-matching-for-switch)
8. [函數式接口與延遲計算](#8-函數式接口與延遲計算)
9. [summary與 DDD 合規性](#9-summary與-ddd-合規性)

## 1. Pattern Matching for instanceof

### 改進前
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Money money = (Money) o;
    return Objects.equals(amount, money.amount) && Objects.equals(currency, money.currency);
}
```

### 改進後
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof Money money) {
        return Objects.equals(amount, money.amount) && Objects.equals(currency, money.currency);
    }
    return false;
}
```

### 好處
- **簡化代碼**：合併了類型檢查和轉換，減少了冗餘代碼
- **提高Security**：消除了顯式類型轉換的風險
- **提高可讀性**：代碼更加直觀，意圖更加明確
- **DDD 合規性**：完全符合Value Object（Value Object）的實現要求，不改變其不可變性和相等性比較邏輯

## 2. 增強型 Switch 表達式

### 改進前
```java
public boolean canTransitionTo(OrderStatus targetStatus) {
    switch (this) {
        case CREATED:
            return targetStatus == SUBMITTED || targetStatus == PENDING || targetStatus == CANCELLED;
        case SUBMITTED:
            return targetStatus == PENDING || targetStatus == PAID || targetStatus == REJECTED || targetStatus == CANCELLED;
        case PENDING:
            return targetStatus == CONFIRMED || targetStatus == REJECTED || targetStatus == CANCELLED;
        case CONFIRMED:
            return targetStatus == PAID || targetStatus == CANCELLED;
        case PAID:
            return targetStatus == PROCESSING || targetStatus == CANCELLED;
        case PROCESSING:
            return targetStatus == SHIPPING || targetStatus == SHIPPED || targetStatus == CANCELLED;
        case SHIPPING:
            return targetStatus == SHIPPED || targetStatus == DELIVERED;
        case SHIPPED:
            return targetStatus == DELIVERED;
        case DELIVERED:
            return targetStatus == COMPLETED;
        case COMPLETED:
            return false; // 終態
        case CANCELLED:
            return false; // 終態
        case REJECTED:
            return false; // 終態
        case PAYMENT_FAILED:
            return targetStatus == SUBMITTED || targetStatus == PENDING || targetStatus == CANCELLED;
        default:
            return false;
    }
}
```

### 改進後
```java
public boolean canTransitionTo(OrderStatus targetStatus) {
    return switch (this) {
        case CREATED -> targetStatus == SUBMITTED || targetStatus == PENDING || targetStatus == CANCELLED;
        case SUBMITTED -> targetStatus == PENDING || targetStatus == PAID || targetStatus == REJECTED || targetStatus == CANCELLED;
        case PENDING -> targetStatus == CONFIRMED || targetStatus == REJECTED || targetStatus == CANCELLED;
        case CONFIRMED -> targetStatus == PAID || targetStatus == CANCELLED;
        case PAID -> targetStatus == PROCESSING || targetStatus == CANCELLED;
        case PROCESSING -> targetStatus == SHIPPING || targetStatus == SHIPPED || targetStatus == CANCELLED;
        case SHIPPING -> targetStatus == SHIPPED || targetStatus == DELIVERED;
        case SHIPPED -> targetStatus == DELIVERED;
        case DELIVERED -> targetStatus == COMPLETED;
        case COMPLETED, CANCELLED, REJECTED -> false; // 終態
        case PAYMENT_FAILED -> targetStatus == SUBMITTED || targetStatus == PENDING || targetStatus == CANCELLED;
    };
}
```

### 好處
- **簡潔性**：使用箭頭語法（->）代替冗長的 case 和 break 語句
- **表達力**：可以將多個 case 標籤組合在一起（如 `case COMPLETED, CANCELLED, REJECTED -> false;`）
- **Security**：switch 表達式確保所有可能的枚舉值都被處理
- **DDD 合規性**：增強了Value Object（OrderStatus）的業務規則表達，使狀態轉換邏輯更加清晰

## 3. String Templates

### 新增功能
```java
// 使用 Java 21 的 String Templates
return STR."金額: \{formatter.format(amount)} (\{currencyCode})";

// 多行文本與模板結合
return STR."""
    交易摘要:
    說明: \{description}
    金額: \{formattedAmount}
    時間: \{java.time.LocalDateTime.now()}
    """;
```

### 好處
- **可讀性**：比傳統的字符串連接或 String.format() 更加直觀
- **Performance**：編譯時處理，避免了運行時的字符串連接開銷
- **表達力**：可以直接在字符串中嵌入表達式，減少代碼量
- **多行支持**：結合多行文本塊，可以創建格式化的複雜輸出
- **DDD 合規性**：增強了領域對象的表示層能力，使領域概念的展示更加直觀

### 實現示例：MoneyFormatter 類
```java
public class MoneyFormatter {
    public static String format(Money money, Locale locale) {
        var currencyCode = money.getCurrency().getCurrencyCode();
        var amount = money.getAmount();
        var formatter = NumberFormat.getCurrencyInstance(locale);
        formatter.setCurrency(money.getCurrency());
        
        // 使用 Java 21 的 String Templates
        return STR."金額: \{formatter.format(amount)} (\{currencyCode})";
    }
}
```

## 4. Record 類型

### 新增功能
```java
public record OrderSummary(
    String orderId,
    String customerId,
    OrderStatus status,
    Money totalAmount,
    Money effectiveAmount,
    int itemCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    // 緊湊建構子
    public OrderSummary {
        // 驗證參數
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("訂單ID不能為空");
        }
        // 其他驗證...
    }
    
    // 業務方法
    public boolean isCompleted() {
        return status == OrderStatus.COMPLETED || status == OrderStatus.DELIVERED;
    }
    
    public Money getDiscountAmount() {
        return totalAmount.subtract(effectiveAmount);
    }
}
```

### 好處
- **簡潔性**：自動生成 getter、equals、hashCode 和 toString 方法
- **不可變性**：默認創建不可變對象，符合Value Object的Design Principle
- **緊湊建構子**：可以在建構子中進行參數驗證，確保對象的完整性
- **DDD 合規性**：完美適合作為Value Object（Value Object）或 DTO（數據傳輸對象），保持不可變性和值相等性

## 5. SequencedCollection 接口

### 新增功能
```java
public static <T> T getFirst(Collection<T> collection) {
    if (collection == null || collection.isEmpty()) {
        return null;
    }
    
    // 使用 Java 21 的 SequencedCollection 接口
    if (collection instanceof List<T> list) {
        return list.getFirst();
    }
    
    return collection.iterator().next();
}

public static <T> List<T> reverse(List<T> list) {
    if (list == null || list.isEmpty()) {
        return List.of();
    }
    
    // 使用 Java 21 的 SequencedCollection 接口
    return list.reversed();
}
```

### 好處
- **語義清晰**：使用 `getFirst()`, `getLast()`, `reversed()` 等方法比迭代器或索引操作更直觀
- **代碼簡化**：減少了樣板代碼，使集合操作更加簡潔
- **Performance優化**：某些實現可能提供更高效的操作
- **DDD 合規性**：增強了Domain Service和倉儲實現的能力，使集合操作更加直觀

### 實現示例：CollectionUtils 類
```java
public final class CollectionUtils {
    public static <T> List<T> filter(Collection<T> collection, Predicate<T> predicate) {
        if (collection == null || collection.isEmpty()) {
            return List.of();
        }
        
        // 使用 Java 21 的 Stream API 增強功能
        return collection.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
}
```

## 6. 虛擬線程

### 新增功能
```java
// 使用虛擬線程執行器
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<?>> futures = new ArrayList<>();
    
    for (int i = 0; i < taskCount; i++) {
        futures.add(executor.submit(() -> {
            // 任務邏輯
            Thread.sleep(sleepMillis);
            return STR."虛擬線程任務完成: \{Thread.currentThread()}";
        }));
    }
    
    // 等待所有任務完成
    for (Future<?> future : futures) {
        future.get();
    }
}

// 直接創建虛擬線程
Thread thread = Thread.ofVirtual()
        .name(name)
        .uncaughtExceptionHandler((t, e) -> 
            System.err.println(STR."線程 \{t.getName()} 發生未捕獲異常: \{e.getMessage()}"))
        .start(runnable);
```

### 好處
- **高並發**：可以創建數百萬個虛擬線程，而不會耗盡系統Resource
- **簡化編程模型**：使用同步代碼風格編寫高效的異步程序
- **減少上下文切換開銷**：虛擬線程的切換成本遠低於平台線程
- **提高Resource利用率**：更有效地利用 CPU 和內存Resource
- **DDD 合規性**：增強了Domain Service和應用服務的實現能力，特別適合處理大量並發操作的場景

### 實現示例：VirtualThreadDemo 類
```java
public class VirtualThreadDemo {
    public static long runWithVirtualThreads(int taskCount, long sleepMillis) {
        Instant start = Instant.now();
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // 執行任務...
        }
        
        Instant end = Instant.now();
        return Duration.between(start, end).toMillis();
    }
}
```

## 7. Pattern Matching for Switch

### 新增功能
```java
public static String checkInputType(Object input) {
    return switch (input) {
        case null -> "輸入為 null";
        case String s when s.isBlank() -> "輸入為空字符串";
        case String s when isValidEmail(s) -> STR."輸入為有效的電子郵件地址: \{s}";
        case String s -> STR."輸入為普通字符串: \{s}";
        case Integer i -> STR."輸入為整數: \{i}";
        case Double d -> STR."輸入為浮點數: \{d}";
        case Boolean b -> STR."輸入為布爾值: \{b}";
        case Object[] arr -> STR."輸入為數組，長度為: \{arr.length}";
        case java.util.List<?> list -> STR."輸入為列表，大小為: \{list.size()}";
        default -> STR."輸入為其他類型: \{input.getClass().getSimpleName()}";
    };
}
```

### 好處
- **表達力**：可以在 switch 中直接進行類型匹配和條件判斷
- **簡潔性**：減少了冗長的 if-else 鏈和類型轉換
- **Security**：編譯時類型檢查，避免運行時類型錯誤
- **可讀性**：代碼結構更加清晰，邏輯分支一目了然
- **DDD 合規性**：增強了Domain Service和規格（Specification）的實現能力，使複雜的業務規則表達更加直觀

### 實現示例：StringPatternMatcher 類
```java
public class StringPatternMatcher {
    public static String checkInputType(Object input) {
        return switch (input) {
            case null -> "輸入為 null";
            case String s when s.isBlank() -> "輸入為空字符串";
            case String s when isValidEmail(s) -> STR."輸入為有效的電子郵件地址: \{s}";
            // 其他 case...
        };
    }
}
```

## 8. 函數式接口與延遲計算

### 改進前
```java
public static void checkArgument(boolean condition, String message) {
    if (!condition) {
        throw new IllegalArgumentException(message);
    }
}
```

### 改進後
```java
public static void checkArgument(boolean condition, java.util.function.Supplier<String> messageSupplier) {
    if (!condition) {
        throw new IllegalArgumentException(messageSupplier.get());
    }
}
```

### 好處
- **延遲計算**：只在需要時才計算錯誤消息，提高Performance
- **動態消息**：可以根據運行時狀態生成更具體的錯誤信息
- **代碼組織**：將錯誤消息生成邏輯與檢查邏輯分離
- **DDD 合規性**：增強了領域Entity和Value Object的參數驗證能力，提高了業務規則的表達能力

### 實現示例：Preconditions 類
```java
public final class Preconditions {
    public static void checkArgument(boolean condition, java.util.function.Supplier<String> messageSupplier) {
        if (!condition) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
    }
}
```

## 9. summary與 DDD 合規性

Java 21 的新特性不僅提高了代碼的簡潔性和可讀性，還能更好地支持Domain-Driven Design（DDD）的實現：

### Value Object（Value Objects）
- **Record 類型**：完美適合實現不可變的Value Object
- **Pattern Matching for instanceof**：簡化Value Object的相等性比較
- **String Templates**：增強Value Object的字符串表示

### Entity（Entities）
- **Pattern Matching for instanceof**：簡化Entity的相等性比較
- **函數式接口與延遲計算**：增強參數驗證和業務規則表達

### Aggregate Root（Aggregate Roots）
- **虛擬線程**：提高Aggregate Root處理並發操作的能力
- **SequencedCollection 接口**：簡化Aggregate Root管理集合的操作

### Domain Service（Domain Services）
- **Pattern Matching for Switch**：簡化複雜業務規則的表達
- **增強型 Switch 表達式**：使狀態轉換和業務邏輯更加清晰

### 規格（Specifications）
- **Pattern Matching for Switch**：增強規格的條件表達能力
- **函數式接口**：簡化規格的組合和重用

### 倉儲（Repositories）
- **虛擬線程**：提高數據訪問的並發Performance
- **SequencedCollection 接口**：簡化查詢結果的處理

### Domain Event（Domain Events）
- **虛擬線程**：提高事件處理的並發能力
- **String Templates**：增強事件Logging和調試信息的表達

## conclusion

升級到 Java 21 不僅是技術上的進步，也是提升領域模型表達能力的機會。通過合理利用新特性，可以使 DDD 實現更加簡潔、直觀和高效，同時保持領域模型的純粹性和業務表達力。

在升級過程中，應該注意保持 DDD 戰術Design Pattern的完整性，確保新特性的使用增強而不是破壞領域模型的表達能力。特別是對於Value Object和Entity這些核心 DDD 概念，應該確保其不可變性、相等性比較和業務規則的正確實現。
