# 評價分析 Agent 設計文檔

> **Review Analysis Agent - 自動化評價分析與管理**

## 概述

評價分析 Agent 負責自動化分析商品評價，識別情感趨勢，發現產品問題，並生成可行的改進建議。

## 業務目標

- 評價處理效率提升 80%
- 負面評價響應時間 < 2 小時
- 產品問題識別準確率 > 85%
- 評價洞察報告自動化 100%

## 涉及 Bounded Contexts

| Context | 用途 | 整合方式 |
|---------|------|----------|
| Review | 評價管理 | ReviewApplicationService |
| Product | 產品資訊 | ProductApplicationService |
| Order | 訂單關聯 | OrderApplicationService |
| Customer | 客戶資訊 | CustomerApplicationService |
| Notification | 通知發送 | NotificationApplicationService |

## 核心功能

### 1. 情感分析

**分析維度**:
- 整體情感傾向 (正面/中性/負面)
- 情感強度評分
- 多維度情感 (產品品質、物流、服務等)
- 情感趨勢變化

**分析流程**:
```
評價文本 → 預處理 → 情感模型分析 → 多維度評分
                                        ↓
                                  情感標籤
                                        ↓
                                  趨勢統計
```

### 2. 問題識別

**識別類型**:
- 產品品質問題
- 物流配送問題
- 客服服務問題
- 描述不符問題
- 包裝問題

**識別流程**:
```
負面評價 → 關鍵詞提取 → 問題分類 → 嚴重度評估
                                        ↓
                              ┌─────────┴─────────┐
                              ↓                   ↓
                         一般問題            嚴重問題
                              ↓                   ↓
                         記錄統計            即時告警
```

### 3. 評價摘要生成

**摘要類型**:
- 單商品評價摘要
- 店鋪整體評價摘要
- 時間段評價報告
- 競品評價對比

### 4. 可疑評價檢測

**檢測特徵**:
- 異常評價模式
- 刷單行為識別
- 惡意差評識別
- 競爭對手攻擊

## AgentCore 整合設計

### Memory 配置

```yaml
memory:
  short_term:
    # 當前分析上下文
    max_items: 100
    ttl: 30m
    
  long_term:
    # 評價模式和歷史
    storage: dynamodb
    table: review-agent-memory
    attributes:
      - product_review_patterns
      - sentiment_baselines
      - issue_categories
      - suspicious_patterns
```

### Tools 定義

```python
# tools/sentiment_analysis_tools.py

@tool
def analyze_review_sentiment(review_id: str) -> SentimentResult:
    """分析單條評價情感"""
    pass

@tool
def batch_analyze_sentiment(review_ids: List[str]) -> List[SentimentResult]:
    """批量分析評價情感"""
    pass

@tool
def get_sentiment_distribution(product_id: str, time_range: str = "30d") -> SentimentDistribution:
    """獲取情感分布"""
    pass

@tool
def analyze_sentiment_trends(product_id: str, time_range: str = "90d") -> SentimentTrend:
    """分析情感趨勢"""
    pass

# tools/issue_detection_tools.py

@tool
def detect_product_issues(product_id: str, time_range: str = "30d") -> List[ProductIssue]:
    """檢測產品問題"""
    pass

@tool
def classify_review_issues(review_id: str) -> List[IssueClassification]:
    """分類評價問題"""
    pass

@tool
def get_issue_trends(product_id: str, issue_type: str = None) -> IssueTrend:
    """獲取問題趨勢"""
    pass

@tool
def prioritize_issues(product_id: str) -> List[PrioritizedIssue]:
    """問題優先級排序"""
    pass

# tools/summary_generation_tools.py

@tool
def generate_review_summary(product_id: str, time_range: str = "30d") -> ReviewSummary:
    """生成評價摘要"""
    pass

@tool
def generate_store_report(seller_id: str, time_range: str = "30d") -> StoreReviewReport:
    """生成店鋪評價報告"""
    pass

@tool
def extract_key_opinions(product_id: str, aspect: str = None) -> List[KeyOpinion]:
    """提取關鍵觀點"""
    pass

@tool
def compare_with_competitors(product_id: str, competitor_ids: List[str]) -> CompetitorComparison:
    """競品評價對比"""
    pass

# tools/fraud_detection_tools.py

@tool
def detect_suspicious_reviews(product_id: str, time_range: str = "30d") -> List[SuspiciousReview]:
    """檢測可疑評價"""
    pass

@tool
def analyze_reviewer_behavior(reviewer_id: str) -> ReviewerBehaviorAnalysis:
    """分析評價者行為"""
    pass

@tool
def flag_review_for_moderation(review_id: str, reason: str) -> bool:
    """標記評價待審核"""
    pass
```

### 事件處理

```python
# 評價事件處理

@event_handler("ReviewCreated")
async def handle_new_review(event: ReviewCreatedEvent):
    """處理新評價"""
    
    # 1. 情感分析
    sentiment = await analyze_review_sentiment(event.review_id)
    
    # 2. 問題檢測
    issues = await classify_review_issues(event.review_id)
    
    # 3. 可疑檢測
    if await is_suspicious(event):
        await flag_review_for_moderation(event.review_id, "suspicious_pattern")
    
    # 4. 負面評價告警
    if sentiment.score < 0.3:
        await notify_seller(event.seller_id, event.review_id, sentiment)

@scheduled("0 9 * * *")  # 每天早上 9 點
async def generate_daily_reports():
    """生成每日評價報告"""
    pass
```

## 情感分析模型

### 多維度情感評分

| 維度 | 權重 | 說明 |
|------|------|------|
| 產品品質 | 30% | 商品本身的品質評價 |
| 物流配送 | 20% | 配送速度和包裝 |
| 客戶服務 | 20% | 售前售後服務 |
| 性價比 | 15% | 價格與價值匹配 |
| 描述相符 | 15% | 實物與描述一致性 |

### 情感分類標準

```
情感分數範圍：
    │
    ├── 0.8 - 1.0: 非常正面 😊
    │   └── 特徵: 強烈推薦、讚美詞彙
    │
    ├── 0.6 - 0.8: 正面 🙂
    │   └── 特徵: 滿意、符合預期
    │
    ├── 0.4 - 0.6: 中性 😐
    │   └── 特徵: 一般、沒有明顯傾向
    │
    ├── 0.2 - 0.4: 負面 😕
    │   └── 特徵: 不滿意、有抱怨
    │
    └── 0.0 - 0.2: 非常負面 😠
        └── 特徵: 強烈不滿、要求退款
```

## 對話設計

### System Prompt

```
你是電商平台的評價分析助手。

你的職責是：
1. 分析商品和店鋪的評價數據
2. 識別產品問題和改進機會
3. 生成評價洞察報告
4. 檢測可疑評價行為

溝通原則：
- 用數據說話，提供具體統計
- 區分問題的嚴重程度
- 給出可行的改進建議
- 客觀中立地呈現分析結果
```

### 對話範例

```
用戶: 分析一下這個商品的評價

Agent: [調用 generate_review_summary, detect_product_issues]
       
       📊 商品評價分析報告
       
       商品：無線藍牙耳機 Pro
       分析期間：近 30 天
       評價總數：328 條
       
       📈 整體評分：4.2/5.0 ⭐
       
       😊 情感分布：
       ████████████░░░░ 正面 72%
       ████░░░░░░░░░░░░ 中性 18%
       ██░░░░░░░░░░░░░░ 負面 10%
       
       🎯 多維度評分：
       - 音質效果：4.5 ⭐ (優秀)
       - 續航時間：4.3 ⭐ (良好)
       - 佩戴舒適：4.0 ⭐ (良好)
       - 連接穩定：3.8 ⭐ (需關注)
       - 性價比：4.4 ⭐ (良好)
       
       💬 客戶怎麼說：
       
       ✅ 正面評價關鍵詞：
       「音質清晰」(89次)、「續航持久」(67次)、「外觀好看」(45次)
       
       ❌ 負面評價關鍵詞：
       「偶爾斷連」(23次)、「充電盒鬆」(12次)、「說明書不清楚」(8次)
       
       需要我深入分析哪個問題嗎？

用戶: 分析一下斷連的問題

Agent: [調用 get_issue_trends, extract_key_opinions]
       
       🔍 「連接斷連」問題深度分析
       
       📊 問題統計：
       - 提及次數：23 次 (佔負面評價 70%)
       - 趨勢：近 2 週增加 15%
       - 嚴重程度：中等 ⚠️
       
       📝 典型評價摘錄：
       
       1. "用了一週，偶爾會斷連，重新配對就好了"
          - 情感：輕微負面
          - 場景：日常使用
       
       2. "運動時容易斷連，可能是距離問題"
          - 情感：中度負面
          - 場景：運動場景
       
       3. "和 iPhone 連接穩定，但 Android 偶爾斷"
          - 情感：中性
          - 場景：跨設備使用
       
       🔎 根因分析：
       1. 藍牙 5.0 在複雜環境下穩定性不足 (40%)
       2. Android 設備兼容性問題 (35%)
       3. 距離超過 5 米時信號衰減 (25%)
       
       💡 改進建議：
       
       短期：
       1. 更新產品說明，標註最佳使用距離
       2. 提供 Android 設備配對指南
       3. 客服準備標準回覆話術
       
       中期：
       1. 考慮升級至藍牙 5.3 版本
       2. 優化天線設計
       3. 增加自動重連功能
       
       需要我生成完整的改進報告嗎？
```

## 效能指標

| 指標 | 目標 | 監控方式 |
|------|------|----------|
| 分析延遲 | < 1s (p95) | CloudWatch Metrics |
| 情感分類準確率 | > 90% | 人工抽檢 |
| 問題識別準確率 | > 85% | 人工驗證 |
| 可疑評價檢出率 | > 80% | 審核結果 |

## 測試計劃

### 單元測試
- 情感分析準確性
- 問題分類正確性
- 摘要生成品質

### 整合測試
- 評價服務整合
- 通知服務整合
- 報告生成流程

### 模型測試
- 多語言支援
- 邊界案例處理
- 新詞彙適應

## 部署計劃

### Phase 1: 基礎分析 (1 週)
- 情感分析功能
- 基本統計報告
- 負面評價告警

### Phase 2: 深度分析 (0.5 週)
- 問題識別
- 趨勢分析
- 摘要生成

### Phase 3: 進階功能 (0.5 週)
- 可疑評價檢測
- 競品對比
- 自動化報告

---

**文件版本**: 1.0  
**建立日期**: 2026-01-06  
**狀態**: 📋 規劃中
