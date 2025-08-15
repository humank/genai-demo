package solid.humank.genaidemo.testutils.bdd;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** BDD 測試上下文 用於在測試步驟之間共享狀態 */
@Component
public class BddTestContext {

    private String currentCustomerId;
    private String currentCustomerName;
    private List<Map<String, Object>> lastProductList;
    private List<Map<String, Object>> lastSearchResults;
    private Map<String, Object> lastProductDetail;
    private Map<String, Object> currentCart;
    private Map<String, Object> lastCheckoutSummary;
    private List<Map<String, Object>> lastPromotions;
    private Map<String, Object> lastPromotionResult;
    private Map<String, Object> lastCouponResult;
    private String lastError;

    // Getters and Setters
    public String getCurrentCustomerId() {
        return currentCustomerId;
    }

    public void setCurrentCustomerId(String currentCustomerId) {
        this.currentCustomerId = currentCustomerId;
    }

    public String getCurrentCustomerName() {
        return currentCustomerName;
    }

    public void setCurrentCustomerName(String currentCustomerName) {
        this.currentCustomerName = currentCustomerName;
    }

    public List<Map<String, Object>> getLastProductList() {
        return lastProductList;
    }

    public void setLastProductList(List<Map<String, Object>> lastProductList) {
        this.lastProductList = lastProductList;
    }

    public List<Map<String, Object>> getLastSearchResults() {
        return lastSearchResults;
    }

    public void setLastSearchResults(List<Map<String, Object>> lastSearchResults) {
        this.lastSearchResults = lastSearchResults;
    }

    public Map<String, Object> getLastProductDetail() {
        return lastProductDetail;
    }

    public void setLastProductDetail(Map<String, Object> lastProductDetail) {
        this.lastProductDetail = lastProductDetail;
    }

    public Map<String, Object> getCurrentCart() {
        return currentCart;
    }

    public void setCurrentCart(Map<String, Object> currentCart) {
        this.currentCart = currentCart;
    }

    public Map<String, Object> getLastCheckoutSummary() {
        return lastCheckoutSummary;
    }

    public void setLastCheckoutSummary(Map<String, Object> lastCheckoutSummary) {
        this.lastCheckoutSummary = lastCheckoutSummary;
    }

    public List<Map<String, Object>> getLastPromotions() {
        return lastPromotions;
    }

    public void setLastPromotions(List<Map<String, Object>> lastPromotions) {
        this.lastPromotions = lastPromotions;
    }

    public Map<String, Object> getLastPromotionResult() {
        return lastPromotionResult;
    }

    public void setLastPromotionResult(Map<String, Object> lastPromotionResult) {
        this.lastPromotionResult = lastPromotionResult;
    }

    public Map<String, Object> getLastCouponResult() {
        return lastCouponResult;
    }

    public void setLastCouponResult(Map<String, Object> lastCouponResult) {
        this.lastCouponResult = lastCouponResult;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    /** 清理測試上下文 */
    public void clear() {
        currentCustomerId = null;
        currentCustomerName = null;
        lastProductList = null;
        lastSearchResults = null;
        lastProductDetail = null;
        currentCart = null;
        lastCheckoutSummary = null;
        lastPromotions = null;
        lastPromotionResult = null;
        lastCouponResult = null;
        lastError = null;
    }
}
