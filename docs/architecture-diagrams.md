# GenAI Demo - 架構圖文檔

## 📋 概述

本文檔包含 GenAI Demo 項目的完整架構圖，這些圖表是基於 CDK 代碼自動生成的，展示了系統的 AWS 基礎設施和領域驅動設計架構。

## 🎨 架構圖列表

### 📁 完整架構圖對應表

| 圖表名稱 | 檔案名稱 | 描述 |
|---------|---------|------|
| 系統架構圖 | `storage/1758271388722-qh0vw5v.json` | 基本系統架構圖 |
| 用戶註冊流程圖 | `storage/1758271452950-pqpa620.json` | 用戶註冊業務流程 |
| GenAI Demo - AWS CDK 架構圖 | `storage/1758272821927-c24lg7z.json` | 原始 CDK 架構圖 |
| GenAI Demo - 領域事件架構流程圖 | `storage/1758272891082-z23qvhs.json` | 領域事件處理流程 |
| **AWS CDK Unified Architecture Diagram** | `storage/aws-cdk-unified-architecture-diagram.json` | **統一完整架構圖** ⭐ |
| Architecture Compliance Check - ArchUnit Rules | `storage/architecture-compliance-check-archunit-rules.json` | ArchUnit 規則檢查 |
| Observability Requirements - Monitoring & Tracing | `storage/observability-requirements-monitoring-tracing.json` | 監控追蹤架構 |
| Four Architecture Perspectives Checklist | `storage/four-architecture-perspectives-checklist.json` | 四大觀點檢查 |
| Concurrency Strategy & Resilience Patterns | `storage/concurrency-strategy-resilience-patterns.json` | 並發韌性模式 |
| Technology Evolution Standards & Version Management | `storage/technology-evolution-standards-version-management.json` | 技術演進管理 |
| Rozanski & Woods Seven Viewpoints & Stakeholder Mapping | `storage/rozanski-woods-seven-viewpoints-stakeholder-mapping.json` | 七大視點利害關係人 |
| Seven Architecture Viewpoints Detailed Focus Areas | `storage/seven-viewpoints-detailed-focus-areas.json` | 七大視點關注重點 |
| GenAI Demo Project Maturity Assessment & Recommendations | `storage/genai-demo-maturity-assessment-recommendations.json` | 專案成熟度評估 |
| GenAI Demo Architecture Improvement Action Plan | `storage/genai-demo-improvement-action-plan.json` | 改進行動計畫 |
| GenAI Demo Seven Viewpoints Analysis & Roadmap | `storage/genai-demo-seven-viewpoints-analysis-roadmap.json` | 深度分析路線圖 |
| GenAI Demo Technical Implementation Plan | `storage/genai-demo-technical-implementation-plan.json` | 技術實施計畫 |

### 🎯 推薦使用

**主要架構圖**: `AWS CDK Unified Architecture Diagram` (`aws-cdk-unified-architecture-diagram.json`)

- 這是最完整的架構圖，整合了所有 CDK stack 組件
- 包含網路層、應用層、資料庫層、安全層等完整架構
- 展示了組件間的連接關係和資料流向

### 📋 重要說明

⚠️ **檔案命名限制**: 由於 Excalidraw MCP 工具的限制，檔案必須保持原始的 ID 格式命名（如 `1758273710520-jghech8.json`），不能使用自定義的檔案名稱。如果重新命名檔案，會導致 "Failed to load Document" 錯誤。

## 🏗️ CDK Stack 架構

### 核心基礎設施 Stacks

1. **NetworkStack** - VPC、子網、安全組配置
2. **SecurityStack** - KMS 加密、IAM 角色和策略
3. **CoreInfrastructureStack** - Application Load Balancer、目標組

### 可觀測性和監控 Stacks

4. **ObservabilityStack** - CloudWatch 日誌、儀表板
5. **AlertingStack** - SNS 主題、告警配置
6. **CostOptimizationStack** - AWS Budgets、成本告警
7. **CrossRegionObservabilityStack** - 跨區域監控和日誌複製

### 數據和分析 Stacks

8. **AnalyticsStack** - S3 Data Lake、Kinesis Firehose、Glue、QuickSight
9. **MSKStack** - Apache Kafka 集群、配置、監控

### 高可用性 Stacks

10. **Route53FailoverStack** - DNS 故障轉移、健康檢查

## 🌐 如何查看架構圖

### 方法 1: 在 Excalidraw 中查看

1. 打開 [Excalidraw.com](https://excalidraw.com)
2. 點擊 "File" > "Open"
3. 複製下面的 JSON 內容並貼上

### 方法 2: 使用 MCP 工具導出

```bash
# 導出為 JSON 格式
mcp_excalidraw_export_to_json --id 1758272821927-c24lg7z

# 導出為 SVG 格式
mcp_excalidraw_export_to_svg --id 1758272821927-c24lg7z
```

## 📊 架構特色

### 🏛️ 基礎設施特色

- **多層網絡架構**: Public、Private、Database 三層子網
- **全面安全防護**: KMS、IAM、Security Groups、WAF、CloudTrail、GuardDuty
- **完整可觀測性**: CloudWatch、X-Ray、SNS 告警、成本監控
- **數據分析管道**: S3 Data Lake、Kinesis Firehose、Glue、QuickSight

### 🔄 領域驅動設計特色

- **聚合根模式**: 負責收集和管理領域事件
- **事件驅動架構**: 使用 MSK (Apache Kafka) 發布領域事件
- **CQRS 模式**: 命令查詢責任分離
- **事件溯源**: 完整的業務歷史追蹤
- **跨聚合通信**: 通過領域事件實現松耦合

### 🌍 多區域支援

- **災難恢復**: 跨區域數據複製和故障轉移
- **Route 53 故障轉移**: 自動 DNS 切換
- **跨區域可觀測性**: 統一監控和日誌管理

### 💰 成本優化

- **AWS Budgets**: 自動預算監控
- **生命週期管理**: S3 數據自動歸檔
- **資源標籤**: 完整的成本分配追蹤

## 🔧 技術棧

### 後端技術

- **Spring Boot 3.4.5** + **Java 21**
- **Spring Data JPA** + **Hibernate**
- **PostgreSQL** (生產) / **H2** (開發測試)
- **Apache Kafka** (MSK) 用於領域事件

### 前端技術

- **Consumer App**: Angular 18 + TypeScript
- **CMC Management**: Next.js 14 + React 18 + TypeScript

### AWS 服務

- **計算**: ECS/EKS、Lambda
- **網絡**: VPC、ALB、Route 53
- **存儲**: S3、RDS
- **消息**: MSK (Apache Kafka)
- **監控**: CloudWatch、X-Ray
- **安全**: KMS、IAM、WAF、GuardDuty
- **分析**: Kinesis Firehose、Glue、Athena、QuickSight

## 📝 更新說明

這些架構圖是基於以下 CDK 代碼自動生成的：

- 最後更新時間: 2025-09-19
- CDK 版本: AWS CDK v2
- 基於實際的 TypeScript CDK 代碼

如需更新架構圖，請在 CDK 代碼變更後重新生成。

## 🏗️ **架構方法論圖表**

### Rozanski & Woods 架構方法論視覺化

基於 `.kiro/steering/rozanski-woods-architecture-methodology.md` 文件，我們創建了完整的方法論圖表集：

#### 📊 **新增方法論圖表詳細說明**

5. **架構合規性檢查流程** (`1758275485504-dbwdpv7`)
   - 展示 ArchUnit 規則的四大檢查類別
   - 領域層依賴規則、聚合根規則、事件處理器規則、值物件規則
   - 合規性監控指標：100% 覆蓋率要求

6. **可觀測性要求架構** (`1758275565208-3velqgl`)
   - 業務指標監控（每個聚合根必須）
   - 用例追蹤（每個應用服務必須）
   - 領域事件指標（每個事件類型必須）
   - 結構化日誌標準和警報配置

7. **四大架構觀點檢查清單** (`1758275636927-mu9pbco`)
   - 安全觀點：零信任架構、最小權限原則
   - 效能與可擴展性觀點：< 2s 回應時間、≥ 1000 req/s 吞吐量
   - 可用性與韌性觀點：≥ 99.9% 可用性、≤ 5 分鐘 RTO
   - 演進觀點：向後相容性、版本管理策略

8. **並發策略與韌性模式** (`1758275706782-36zkf1x`)
   - 並發策略要求：事件處理順序、交易邊界、衝突處理
   - 斷路器模式：CLOSED/OPEN/HALF_OPEN 狀態管理
   - 重試機制：最多 3 次、指數退避、抖動算法
   - 降級策略和死信佇列處理

9. **技術演進標準與版本管理** (`1758275777304-9a6tabo`)
   - 新技術引入標準：成熟度評估、團隊能力、風險控制
   - 版本升級要求：自動化測試、測試環境驗證
   - 風險評估矩陣：學習曲線、效能影響、整合複雜度
   - 遷移策略和回滾計畫：≤ 15 分鐘回滾時間

10. **七大視點與利害關係人對應圖** (`1758276726986-maiv8ad`)
    - 展示每個架構視點對應的主要利害關係人
    - 從軟體開發與商務交付角度標註相關角色
    - 包含完整的七大視點：功能、資訊、並發、開發、部署、操作、上下文
    - 幫助團隊理解不同視點的責任歸屬

11. **七大架構視點關注重點詳細圖** (`1758276802309-2o9w387`)
    - 基於當前 steering 文件的詳細檢查清單
    - 每個視點的具體關注重點和檢查項目
    - 包含相應的工具和方法建議
    - 提供完整的架構設計指導方針

#### 🔄 **方法論應用流程**

```
新功能開發 → 架構合規檢查 → 可觀測性設計 → 四大觀點驗證 → 並發韌性設計 → 技術演進評估
     ↑                                                                                    ↓
     ←←←←←←←←←←←←←←←←←←← 持續改進和回饋 ←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←
```

#### 🎯 **方法論圖表用途**

- **架構合規性檢查**: 確保代碼符合 DDD 和六角架構原則
- **可觀測性設計**: 建立完整的監控、追蹤和警報體系
- **四大觀點驗證**: 從安全、效能、可用性、演進四個維度評估架構
- **並發韌性設計**: 實施斷路器、重試、降級等韌性模式
- **技術演進管理**: 標準化的技術選型和版本升級流程

## 🔗 相關文檔

- [CDK 部署指南](../infrastructure/README.md)
- [多區域架構文檔](../infrastructure/MULTI_REGION_ARCHITECTURE.md)
- [安全實施指南](../infrastructure/SECURITY_IMPLEMENTATION.md)
- [測試指南](../infrastructure/TESTING_GUIDE.md)
- [Rozanski & Woods 架構方法論](../.kiro/steering/rozanski-woods-architecture-methodology.md)

## 🔍 **最新架構分析圖表**

### 📈 **專案深度評估系列**

14. **GenAI Demo 專案七大視點深度分析與改進路線圖** (`1758278799092-ft2juf7`)
    - 基於實際專案代碼的深度分析 (13個界限上下文、143個Java測試檔案、103個基礎設施測試)
    - 七大視點的詳細現狀評估和成熟度評分 (整體4.1/5.0)
    - 12週詳細改進路線圖，從4.1提升至4.7成熟度
    - 優先級分類：🚨 Context (2.0→4.0)、🔥 Concurrency (3.0→4.5)、⚡ Information & Operational (4.0→4.5)

15. **GenAI Demo 架構改進技術實施詳細計畫** (`1758278894457-ehw2saj`)
    - 四個階段的詳細技術實施計畫 (每階段2-4週)
    - 每週具體任務和負責團隊分工
    - 技術工具和實施方法指導 (EventStore、斷路器、監控等)
    - 成功指標和里程碑檢查點

### 🎯 **評估結果摘要**

**專案優勢** (⭐⭐⭐⭐⭐ 優秀等級)：

- **Functional Viewpoint**: 完整的DDD架構，13個界限上下文
- **Development Viewpoint**: 六角架構、143個測試檔案、ArchUnit合規
- **Deployment Viewpoint**: AWS CDK v2、6個協調堆疊、103個測試通過

**需要改進** (急需處理)：

- **Context Viewpoint** (⭐⭐): 系統邊界和外部依賴映射
- **Concurrency Viewpoint** (⭐⭐⭐): 並發策略和韌性模式

**改進計畫**: 12週實施計畫，預期將整體成熟度從4.1提升至4.7 (優秀等級)
