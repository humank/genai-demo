# 賣家助手 Agent 設計文檔

> **Seller Assistant Agent - 智能賣家管理與營運支援**

## 概述

賣家助手 Agent 協助賣家管理店鋪和商品，提供庫存預警、銷售分析、評價管理等智能化支援服務。

## 業務目標

- 賣家營運效率提升 30%
- 庫存周轉率提升 20%
- 商品上架時間減少 50%
- 賣家滿意度 > 4.5/5.0

## 涉及 Bounded Contexts

| Context | 用途 | 整合方式 |
|---------|------|----------|
| Seller | 賣家管理 | SellerApplicationService |
| Product | 商品管理 | ProductApplicationService |
| Inventory | 庫存管理 | InventoryApplicationService |
| Review | 評價管理 | ReviewApplicationService |
| Order | 訂單統計 | OrderApplicationService |
| Pricing | 定價建議 | PricingApplicationService |

## 核心功能

### 1. 商品上架助手

**輔助功能**:
- 商品資訊填寫引導
- 類別推薦
- 標題和描述優化建議
- 圖片品質檢查
- 定價建議

**上架流程**:
```
賣家輸入 → 資訊補全建議 → 類別匹配 → 定價建議 → 審核提交
                                                    ↓
                                              上架成功
                                                    ↓
                                              優化建議
```

### 2. 庫存智能管理

**管理功能**:
- 庫存預警通知
- 補貨建議
- 滯銷品識別
- 庫存周轉分析

**預警邏輯**:
```
庫存監控 → 銷售速度分析 → 計算安全庫存 → 預警判斷
                                            ↓
                                  ┌─────────┴─────────┐
                                  ↓                   ↓
                             庫存充足            庫存不足
                                  ↓                   ↓
                             定期報告            發送預警
                                                      ↓
                                                補貨建議
```

### 3. 銷售數據分析

**分析維度**:
- 銷售趨勢
- 熱銷商品排行
- 客戶群體分析
- 競品價格對比
- 利潤分析

### 4. 評價管理助手

**管理功能**:
- 新評價通知
- 負面評價預警
- 回覆建議生成
- 評價趨勢分析

## AgentCore 整合設計

### Memory 配置

```yaml
memory:
  short_term:
    # 當前對話上下文
    max_turns: 30
    ttl: 1h
    
  long_term:
    # 賣家偏好和歷史數據
    storage: dynamodb
    table: seller-agent-memory
    attributes:
      - seller_id
      - store_preferences
      - product_templates
      - pricing_history
      - performance_metrics
```

### Tools 定義

```python
# tools/product_management_tools.py

@tool
def create_product_draft(product_info: dict) -> ProductDraft:
    """創建商品草稿"""
    pass

@tool
def suggest_product_category(product_name: str, description: str) -> List[CategorySuggestion]:
    """推薦商品類別"""
    pass

@tool
def optimize_product_title(title: str, category: str) -> TitleOptimization:
    """優化商品標題"""
    pass

@tool
def suggest_product_price(product_id: str, cost: float) -> PriceSuggestion:
    """建議商品定價"""
    pass

@tool
def check_product_compliance(product_id: str) -> ComplianceCheck:
    """檢查商品合規性"""
    pass

# tools/inventory_management_tools.py

@tool
def get_inventory_status(seller_id: str, product_id: str = None) -> InventoryStatus:
    """查詢庫存狀態"""
    pass

@tool
def get_low_stock_alerts(seller_id: str) -> List[LowStockAlert]:
    """獲取低庫存預警"""
    pass

@tool
def calculate_reorder_suggestion(product_id: str) -> ReorderSuggestion:
    """計算補貨建議"""
    pass

@tool
def identify_slow_moving_products(seller_id: str, days: int = 30) -> List[SlowMovingProduct]:
    """識別滯銷商品"""
    pass

# tools/sales_analytics_tools.py

@tool
def get_sales_summary(seller_id: str, time_range: str = "30d") -> SalesSummary:
    """獲取銷售摘要"""
    pass

@tool
def get_top_selling_products(seller_id: str, limit: int = 10) -> List[TopProduct]:
    """獲取熱銷商品"""
    pass

@tool
def analyze_sales_trends(seller_id: str, product_id: str = None) -> SalesTrend:
    """分析銷售趨勢"""
    pass

@tool
def compare_competitor_prices(product_id: str) -> List[CompetitorPrice]:
    """對比競品價格"""
    pass

@tool
def calculate_profit_margin(seller_id: str, time_range: str = "30d") -> ProfitAnalysis:
    """計算利潤率"""
    pass

# tools/review_management_tools.py

@tool
def get_recent_reviews(seller_id: str, days: int = 7) -> List[Review]:
    """獲取最近評價"""
    pass

@tool
def get_negative_reviews(seller_id: str, days: int = 30) -> List[Review]:
    """獲取負面評價"""
    pass

@tool
def generate_review_reply(review_id: str) -> ReviewReply:
    """生成評價回覆建議"""
    pass

@tool
def analyze_review_sentiment(seller_id: str, time_range: str = "30d") -> SentimentAnalysis:
    """分析評價情感趨勢"""
    pass
```

## 對話設計

### System Prompt

```
你是電商平台的賣家助手。

你的職責是：
1. 協助賣家管理商品和庫存
2. 提供銷售數據分析和建議
3. 幫助處理評價和客戶反饋
4. 提供營運優化建議

溝通原則：
- 使用賣家熟悉的業務術語
- 提供具體可行的建議
- 用數據支持你的建議
- 主動提醒重要事項
```

### 對話範例

```
賣家: 我想上架一個新商品

Agent: 好的！我來協助您上架新商品。請告訴我：
       
       1. 商品名稱是什麼？
       2. 大概是什麼類別的商品？
       
       我會幫您優化標題、推薦類別和建議定價。

賣家: 藍牙無線耳機，電子產品

Agent: [調用 suggest_product_category, optimize_product_title]
       
       📦 商品上架建議
       
       🏷️ 推薦類別：
       1. 電子產品 > 音響設備 > 耳機 > 藍牙耳機 (最佳匹配)
       2. 電子產品 > 手機配件 > 藍牙耳機
       
       ✏️ 標題優化建議：
       原標題：藍牙無線耳機
       建議標題：【2026新款】藍牙5.3無線耳機 降噪運動防水 超長續航
       
       優化原因：
       - 添加年份標籤增加新鮮感
       - 加入熱門搜尋關鍵字
       - 突出產品特點
       
       接下來請提供：
       - 商品成本價格（我會建議售價）
       - 商品圖片（我會檢查品質）
       
       需要我繼續協助嗎？

賣家: 成本 200 元

Agent: [調用 suggest_product_price, compare_competitor_prices]
       
       💰 定價建議
       
       成本：NT$200
       
       📊 市場分析：
       - 同類商品價格區間：NT$399 - NT$899
       - 平均售價：NT$549
       - 您店鋪同類商品平均價：NT$499
       
       🎯 建議定價方案：
       
       方案 A - 競爭定價：NT$449
       - 預估利潤率：55%
       - 預估月銷量：150-200 件
       - 適合：快速搶佔市場
       
       方案 B - 標準定價：NT$549
       - 預估利潤率：64%
       - 預估月銷量：80-120 件
       - 適合：平衡利潤和銷量
       
       方案 C - 高端定價：NT$699
       - 預估利潤率：71%
       - 預估月銷量：30-50 件
       - 適合：強調品質差異化
       
       根據您店鋪的定位，我建議選擇方案 B。
       要我幫您設定這個價格嗎？
```

### 主動通知範例

```
Agent: 📢 庫存預警通知
       
       您好！以下商品庫存即將不足：
       
       ⚠️ 緊急 (預計 3 天內售罄)：
       1. 無線藍牙耳機 A1
          - 當前庫存：15 件
          - 日均銷量：6 件
          - 建議補貨：100 件
       
       ⚡ 注意 (預計 7 天內售罄)：
       2. USB-C 充電線
          - 當前庫存：45 件
          - 日均銷量：8 件
          - 建議補貨：150 件
       
       需要我幫您生成補貨訂單嗎？
```

## 效能指標

| 指標 | 目標 | 監控方式 |
|------|------|----------|
| 回應延遲 | < 2s (p95) | CloudWatch Metrics |
| 建議採納率 | > 60% | 行為追蹤 |
| 賣家滿意度 | > 4.5/5.0 | 反饋調查 |
| 上架成功率 | > 95% | 操作統計 |

## 測試計劃

### 單元測試
- 定價演算法驗證
- 庫存預警邏輯
- 標題優化效果

### 整合測試
- 商品服務整合
- 庫存服務整合
- 評價服務整合

### 用戶測試
- 賣家體驗測試
- 功能完整性驗證
- 建議品質評估

## 部署計劃

### Phase 1: 基礎功能 (1.5 週)
- 商品上架助手
- 基本庫存查詢
- 銷售數據查看

### Phase 2: 智能分析 (1 週)
- 庫存預警
- 定價建議
- 銷售趨勢分析

### Phase 3: 評價管理 (0.5 週)
- 評價監控
- 回覆建議
- 情感分析

---

**文件版本**: 1.0  
**建立日期**: 2026-01-06  
**狀態**: 📋 規劃中
