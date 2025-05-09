package solid.humank.genaidemo.domain.common.policy;

/**
 * 領域政策介面
 * 用於封裝可能會改變的業務規則
 * @param <T> 政策適用的領域物件類型
 * @param <R> 政策執行的結果類型
 */
public interface DomainPolicy<T, R> {
    /**
     * 評估並套用政策
     * @param target 要評估的領域物件
     * @return 政策執行的結果
     */
    R apply(T target);

    /**
     * 檢查政策是否適用於目標物件
     * @param target 要檢查的領域物件
     * @return 是否適用
     */
    boolean isApplicableTo(T target);
}
