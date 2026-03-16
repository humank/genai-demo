# 促銷推薦 Agent 設計文檔

> **Promotion Recommendation Agent - 個性化促銷與定價建議**

## 概述

促銷推薦 Agent 負責分析客戶行為和偏好，提供個性化的促銷活動推薦和動態定價建議，提升轉換率和客戶滿意度。

## 業務目標

- 促銷轉換率提升 25%+
- 客戶參與度提升 30%
- 平均訂單金額 (AOV) 提升 15%
- 促銷 ROI 提升 20%

## 涉及 Bounded Contexts

| Context | 用途 | 整合方式 |
|---------|------|----------|
| Promotion | 促銷活動管理 | PromotionApplicationService |
| Pricing | 定價策略 | PricingApplicationService |
| Product | 產品資訊 | ProductApplicationService |
| Customer | 客戶偏好 | CustomerApplicationService |
| ShoppingCart | 購物車分析 | ShoppingCartApplicationService |
| Order | 購買歷史 | OrderApplicationService |

## 核心功能

### 1. 個性化促銷推薦

**推薦場景**:
- 首頁個性化促銷展示
- 商品詳情頁相關促銷
- 購物車結算頁優惠提示
- 訂單完成後追加推薦

**推薦因素**:
```
客戶偏好分析
    │
    ├── 瀏覽歷史 → 興趣類別
    ├── 購買歷史 → 消費習慣
    ├── 收藏清單 → 潛在需求
    └── 會員等級 → 專屬優惠
           │
           ▼
    促銷匹配引擎
           │
           ▼
    個性化推薦列表
```

### 2. 動態定價建議

**定價因素**:
- 市場競爭價格
- 庫存水平
- 需求預測
- 客戶價值分層
- 時間敏感性

**定價策略**:
```
定價請求 → 收集市場數據 → 分析客戶價值 → 計算建議價格
                                              ↓
                                    ┌─────────┴─────────┐
                                    ↓                   ↓
                               標準定價            個性化定價
                                    ↓                   ↓
                               基礎價格            會員折扣
                                                  限時優惠
                                                  組合優惠
```

### 3. 購物車優化建議

**優化類型**:
- 加購推薦（「再買 X 元免運費」）
- 組合優惠（「搭配購買省 Y 元」）
- 替代商品（「這個更划算」）
- 限時提醒（「優惠即將結束」）

### 4. 促銷效果預測

**預測維度**:
- 預期參與人數
- 預期銷售額
- 預期利潤影響
- 庫存消耗預測

## AgentCore 整合設計

### Memory 配置

```yaml
memory:
  short_term:
    # 當前會話上下文
    max_items: 100
    ttl: 30m
    
  long_term:
    # 客戶偏好和行為模式
    storage: dynamodb
    table: promotion-agent-memory
    attributes:
      - customer_id
      - preference_profile
      - purchase_patterns
      - promotion_response_history
      - price_sensitivity
```

### Tools 定義

```python
# tools/promotion_tools.py

@tool
def get_active_promotions(category: str = None, customer_tier: str = None) -> List[Promotion]:
    """查詢當前有效促銷活動"""
    pass

@tool
def get_customer_eligible_promotions(customer_id: str) -> List[Promotion]:
    """查詢客戶可用促銷"""
    pass

@tool
def calculate_promotion_benefit(promotion_id: str, cart_items: List[dict]) -> PromotionBenefit:
    """計算促銷優惠金額"""
    pass

@tool
def generate_personalized_coupon(customer_id: str, promotion_type: str) -> Coupon:
    """生成個性化優惠券"""
    pass

# tools/pricing_tools.py

@tool
def get_dynamic_price(product_id: str, customer_id: str = None) -> PriceInfo:
    """獲取動態定價"""
    pass

@tool
def calculate_bundle_price(product_ids: List[str]) -> BundlePriceInfo:
    """計算組合價格"""
    pass

@tool
def get_price_history(product_id: str, days: int = 30) -> List[PricePoint]:
    """查詢價格歷史"""
    pass

# tools/recommendation_tools.py

@tool
def get_product_recommendations(customer_id: str, context: str) -> List[ProductRecommendation]:
    """獲取產品推薦"""
    pass

@tool
def get_cart_optimization_suggestions(cart_id: str) -> List[CartSuggestion]:
    """獲取購物車優化建議"""
    pass

@tool
def predict_promotion_effectiveness(promotion_config: dict) -> PromotionPrediction:
    """預測促銷效果"""
    pass

# tools/customer_analysis_tools.py

@tool
def get_customer_preference_profile(customer_id: str) -> PreferenceProfile:
    """獲取客戶偏好檔案"""
    pass

@tool
def analyze_purchase_patterns(customer_id: str) -> PurchasePatterns:
    """分析購買模式"""
    pass

@tool
def calculate_customer_lifetime_value(customer_id: str) -> CLVInfo:
    """計算客戶終身價值"""
    pass
```

### 推薦引擎整合

```python
# 推薦請求處理流程

async def get_personalized_promotions(customer_id: str, context: dict) -> List[Promotion]:
    """
    獲取個性化促銷推薦
    
    Args:
        customer_id: 客戶 ID
        context: 上下文資訊 (頁面、購物車等)
    
    Returns:
        排序後的促銷推薦列表
    """
    # 1. 獲取客戶偏好
    profile = await get_customer_preference_profile(customer_id)
    
    # 2. 獲取可用促銷
    promotions = await get_customer_eligible_promotions(customer_id)
    
    # 3. 計算匹配分數
    scored_promotions = []
    for promo in promotions:
        score = calculate_relevance_score(promo, profile, context)
        scored_promotions.append((promo, score))
    
    # 4. 排序並返回
    scored_promotions.sort(key=lambda x: x[1], reverse=True)
    return [p[0] for p in scored_promotions[:10]]
```

## 推薦演算法

### 客戶分群

```
客戶分群模型
    │
    ├── 價格敏感型
    │   └── 優先推薦: 折扣促銷、滿減活動
    │
    ├── 品質導向型
    │   └── 優先推薦: 新品首發、限量商品
    │
    ├── 便利優先型
    │   └── 優先推薦: 免運費、快速配送
    │
    └── 忠誠會員型
        └── 優先推薦: 會員專屬、積分加倍
```

### 推薦分數計算

| 因素 | 權重 | 說明 |
|------|------|------|
| 類別匹配度 | 25% | 促銷商品與客戶興趣類別匹配 |
| 價格區間匹配 | 20% | 促銷價格與客戶消費習慣匹配 |
| 歷史響應率 | 20% | 客戶對類似促銷的歷史響應 |
| 時效性 | 15% | 促銷剩餘時間緊迫度 |
| 庫存狀況 | 10% | 促銷商品庫存充足度 |
| 利潤貢獻 | 10% | 促銷對平台利潤的貢獻 |

## 對話設計

### System Prompt

```
你是電商平台的促銷推薦助手。

你的職責是：
1. 根據客戶偏好推薦合適的促銷活動
2. 提供購物車優化建議
3. 解答促銷規則相關問題
4. 幫助客戶找到最優惠的購買方案

溝通原則：
- 推薦要有針對性，避免過度推銷
- 清楚說明優惠條件和限制
- 主動計算並展示節省金額
- 尊重客戶選擇，不強迫推銷
```

### 對話範例

```
用戶: 有什麼優惠活動嗎？

Agent: [調用 get_customer_eligible_promotions]
       
       根據您的購物偏好，為您推薦以下優惠：
       
       🔥 最適合您的優惠：
       1. 【會員專屬】電子產品滿 3000 減 300
          - 您常購買電子產品，這個很適合！
       
       2. 【限時特惠】全場滿 1000 免運費
          - 還有 2 天結束
       
       3. 【新品嘗鮮】指定新品 85 折
          - 包含您收藏的藍牙耳機
       
       需要我幫您看看購物車能用哪些優惠嗎？

用戶: 幫我看看購物車

Agent: [調用 get_cart_optimization_suggestions]
       
       📦 您的購物車分析：
       
       目前商品：
       - 無線滑鼠 NT$890
       - USB Hub NT$450
       小計：NT$1,340
       
       💡 優化建議：
       1. 再加購 NT$660 可享「滿 2000 減 200」
          推薦加購：滑鼠墊 NT$299 + 螢幕清潔組 NT$399
          → 總價 NT$2,038，折後 NT$1,838，省 NT$502！
       
       2. 使用「電子產品滿 1000 減 100」
          → 現省 NT$100
       
       要幫您套用哪個優惠呢？
```

## 效能指標

| 指標 | 目標 | 監控方式 |
|------|------|----------|
| 推薦響應時間 | < 500ms (p95) | CloudWatch Metrics |
| 推薦點擊率 | > 15% | 行為追蹤 |
| 推薦轉換率 | > 5% | 訂單歸因 |
| 客戶滿意度 | > 4.0/5.0 | 反饋調查 |
| 推薦多樣性 | > 0.7 | 推薦分析 |

## 測試計劃

### 單元測試
- 推薦演算法正確性
- 分數計算驗證
- Tool 函數測試

### A/B 測試
- 推薦策略對比
- 展示方式優化
- 文案效果測試

### 效能測試
- 高併發推薦請求
- 大量客戶數據處理
- 快取效果驗證

## 部署計劃

### Phase 1: 基礎推薦 (1.5 週)
- 促銷查詢功能
- 基本推薦邏輯
- 購物車分析

### Phase 2: 個性化 (1 週)
- 客戶偏好分析
- 個性化推薦
- Memory 整合

### Phase 3: 優化迭代 (0.5 週)
- A/B 測試框架
- 效果追蹤
- 持續優化

---

**文件版本**: 1.0  
**建立日期**: 2026-01-06  
**狀態**: 📋 規劃中
