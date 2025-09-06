package solid.humank.genaidemo.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** 集合工具類 展示 Java 21 的 SequencedCollection 和增強的 Stream API 功能 */
public final class CollectionUtils {

    private CollectionUtils() {
        // 防止實例化
    }

    /**
     * 過濾集合
     *
     * @param <T> 元素類型
     * @param collection 集合
     * @param predicate 過濾條件
     * @return 過濾後的列表
     */
    public static <T> List<T> filter(Collection<T> collection, Predicate<T> predicate) {
        if (collection == null || collection.isEmpty()) {
            return List.of();
        }

        // 使用 Java 21 的 Stream API 增強功能
        return collection.stream().filter(predicate).toList();
    }

    /**
     * 映射集合
     *
     * @param <T> 輸入元素類型
     * @param <R> 輸出元素類型
     * @param collection 集合
     * @param mapper 映射函數
     * @return 映射後的列表
     */
    public static <T, R> List<R> map(Collection<T> collection, Function<T, R> mapper) {
        if (collection == null || collection.isEmpty()) {
            return List.of();
        }

        // 使用 Java 21 的 Stream API 增強功能
        return collection.stream().map(mapper).toList();
    }

    /**
     * 獲取集合的第一個元素
     *
     * @param <T> 元素類型
     * @param collection 集合
     * @return 第一個元素，如果集合為空則返回 null
     */
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

    /**
     * 獲取集合的最後一個元素
     *
     * @param <T> 元素類型
     * @param collection 集合
     * @return 最後一個元素，如果集合為空則返回 null
     */
    public static <T> T getLast(Collection<T> collection) {
        if (collection == null || collection.isEmpty()) {
            return null;
        }

        // 使用 Java 21 的 SequencedCollection 接口
        if (collection instanceof List<T> list) {
            return list.getLast();
        }

        // 對於非 List 集合，轉換為 ArrayList 後獲取最後一個元素
        List<T> list = new ArrayList<>(collection);
        return list.getLast();
    }

    /**
     * 反轉列表
     *
     * @param <T> 元素類型
     * @param list 列表
     * @return 反轉後的列表
     */
    public static <T> List<T> reverse(List<T> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }

        // 使用 Java 21 的 SequencedCollection 接口
        return list.reversed();
    }

    /**
     * 分組計數
     *
     * @param <T> 元素類型
     * @param collection 集合
     * @param classifier 分類函數
     * @return 分組計數結果
     */
    public static <T, K> Map<K, Long> groupCount(
            Collection<T> collection, Function<T, K> classifier) {
        if (collection == null || collection.isEmpty()) {
            return Map.of();
        }

        // 使用 Java 21 的 Stream API 增強功能
        return collection.stream()
                .collect(Collectors.groupingBy(classifier, Collectors.counting()));
    }
}
