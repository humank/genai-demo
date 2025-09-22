#!/usr/bin/env python3
"""
Development Viewpoint Link Fixing Script

This script fixes broken links in the development viewpoint by creating missing files
or updating references to existing files.
"""

import os
import json
from pathlib import Path
from typing import Dict, List, Set

class LinkFixer:
    def __init__(self, base_path: str = "."):
        self.base_path = Path(base_path)
        self.development_viewpoint_path = self.base_path / "docs" / "viewpoints" / "development"
        self.diagrams_path = self.base_path / "docs" / "diagrams" / "viewpoints" / "development"
        
    def load_validation_results(self) -> Dict:
        """Load the validation results from the JSON file."""
        json_path = Path("build/reports/development-viewpoint-link-validation.json")
        if not json_path.exists():
            raise FileNotFoundError("Please run the link validation script first")
        
        with open(json_path, 'r', encoding='utf-8') as f:
            return json.load(f)
    
    def create_missing_files(self, broken_links: List[Dict]) -> List[str]:
        """Create missing files with basic content."""
        created_files = []
        
        # Group broken links by file to create
        files_to_create = {}
        for link in broken_links:
            resolved_path = Path(link['resolved_path'])
            if not resolved_path.exists() and resolved_path.suffix == '.md':
                # Skip external references and anchor links
                if not link['url'].startswith(('http', '#')):
                    files_to_create[str(resolved_path)] = link
        
        # Create missing files
        for file_path, link_info in files_to_create.items():
            path = Path(file_path)
            
            # Create directory if it doesn't exist
            path.parent.mkdir(parents=True, exist_ok=True)
            
            # Generate basic content based on file name
            content = self.generate_file_content(path, link_info)
            
            with open(path, 'w', encoding='utf-8') as f:
                f.write(content)
            
            try:
                relative_path = path.relative_to(self.base_path)
                created_files.append(str(relative_path))
                print(f"Created: {relative_path}")
            except ValueError:
                # Handle absolute paths
                created_files.append(str(path))
                print(f"Created: {path}")
        
        return created_files
    
    def generate_file_content(self, file_path: Path, link_info: Dict) -> str:
        """Generate appropriate content for a missing file."""
        file_name = file_path.stem
        link_text = link_info.get('text', file_name)
        
        # Basic template
        content = [
            f"# {link_text}",
            "",
            f"本文檔描述 {link_text} 的相關內容。",
            "",
            "## 概覽",
            "",
            f"此部分涵蓋 {link_text} 的核心概念和實作指南。",
            "",
            "## 主要內容",
            "",
            "### 基本概念",
            "",
            f"介紹 {link_text} 的基本概念和原理。",
            "",
            "### 實作指南",
            "",
            f"提供 {link_text} 的具體實作步驟和最佳實踐。",
            "",
            "### 範例",
            "",
            f"展示 {link_text} 的實際應用範例。",
            "",
            "## 相關資源",
            "",
            "- [開發視點總覽](../README.md)",
            "- [架構指南](../architecture/README.md)",
            "",
            "---",
            "",
            "*本文檔是 Development Viewpoint 重組的一部分*"
        ]
        
        # Customize content based on file type
        if 'testing' in str(file_path):
            content = self.generate_testing_content(file_name, link_text)
        elif 'architecture' in str(file_path):
            content = self.generate_architecture_content(file_name, link_text)
        elif 'workflow' in str(file_path):
            content = self.generate_workflow_content(file_name, link_text)
        elif 'coding-standards' in str(file_path):
            content = self.generate_coding_standards_content(file_name, link_text)
        elif 'build-system' in str(file_path):
            content = self.generate_build_system_content(file_name, link_text)
        elif 'quality-assurance' in str(file_path):
            content = self.generate_quality_assurance_content(file_name, link_text)
        elif 'tools-and-environment' in str(file_path):
            content = self.generate_tools_content(file_name, link_text)
        elif 'getting-started' in str(file_path):
            content = self.generate_getting_started_content(file_name, link_text)
        
        return "\n".join(content)
    
    def generate_testing_content(self, file_name: str, link_text: str) -> List[str]:
        """Generate testing-specific content."""
        return [
            f"# {link_text}",
            "",
            f"本文檔描述 {link_text} 的測試策略和實作方法。",
            "",
            "## 測試原則",
            "",
            "### 測試金字塔",
            "",
            "- **單元測試 (80%)**：快速、隔離、專注",
            "- **整合測試 (15%)**：組件互動驗證", 
            "- **端到端測試 (5%)**：完整用戶旅程",
            "",
            "### 測試標準",
            "",
            "- 測試覆蓋率 > 80%",
            "- 單元測試執行時間 < 50ms",
            "- 整合測試執行時間 < 500ms",
            "",
            "## 實作指南",
            "",
            "### 測試結構",
            "",
            "```java",
            "// Given-When-Then 結構",
            "@Test",
            "void should_do_something_when_condition_met() {",
            "    // Given - 準備測試數據",
            "    // When - 執行被測試的行為", 
            "    // Then - 驗證結果",
            "}",
            "```",
            "",
            "### 最佳實踐",
            "",
            "- 使用描述性的測試名稱",
            "- 保持測試簡單和專注",
            "- 維護測試獨立性",
            "- 測試行為而非實作",
            "",
            "## 相關工具",
            "",
            "- JUnit 5：單元測試框架",
            "- Mockito：模擬框架", 
            "- AssertJ：斷言庫",
            "- Cucumber：BDD 測試框架",
            "",
            "## 相關文檔",
            "",
            "- [測試總覽](../README.md)",
            "- [TDD 實踐](../tdd-practices/README.md)",
            "- [BDD 實踐](../bdd-practices/README.md)",
            "",
            "---",
            "",
            "*本文檔遵循 [測試標準](../../../../.kiro/steering/test-performance-standards.md)*"
        ]
    
    def generate_architecture_content(self, file_name: str, link_text: str) -> List[str]:
        """Generate architecture-specific content."""
        return [
            f"# {link_text}",
            "",
            f"本文檔描述 {link_text} 的架構設計原則和實作方法。",
            "",
            "## 架構原則",
            "",
            "### 設計原則",
            "",
            "- **單一職責原則 (SRP)**：每個類別只有一個變更的理由",
            "- **開放封閉原則 (OCP)**：對擴展開放，對修改封閉",
            "- **依賴反轉原則 (DIP)**：依賴抽象而非具體實作",
            "",
            "### 架構模式",
            "",
            "- **六角架構**：清晰的邊界和依賴方向",
            "- **DDD 戰術模式**：聚合根、實體、值物件",
            "- **事件驅動架構**：鬆耦合的組件通訊",
            "",
            "## 實作指南",
            "",
            "### 程式碼結構",
            "",
            "```",
            "domain/",
            "├── model/          # 聚合根、實體、值物件",
            "├── events/         # 領域事件",
            "└── services/       # 領域服務",
            "",
            "application/",
            "├── commands/       # 命令處理",
            "├── queries/        # 查詢處理", 
            "└── services/       # 應用服務",
            "",
            "infrastructure/",
            "├── persistence/    # 資料持久化",
            "├── messaging/      # 訊息處理",
            "└── external/       # 外部服務整合",
            "```",
            "",
            "### 最佳實踐",
            "",
            "- 明確定義聚合邊界",
            "- 使用領域事件進行跨聚合通訊",
            "- 保持領域邏輯純淨",
            "- 實作適當的抽象層",
            "",
            "## 相關文檔",
            "",
            "- [架構總覽](../README.md)",
            "- [DDD 模式](../ddd-patterns/README.md)",
            "- [六角架構](../hexagonal-architecture/README.md)",
            "",
            "---",
            "",
            "*本文檔遵循 [Rozanski & Woods 架構方法論](../../../../.kiro/steering/rozanski-woods-architecture-methodology.md)*"
        ]
    
    def generate_workflow_content(self, file_name: str, link_text: str) -> List[str]:
        """Generate workflow-specific content."""
        return [
            f"# {link_text}",
            "",
            f"本文檔描述 {link_text} 的標準流程和最佳實踐。",
            "",
            "## 流程概覽",
            "",
            f"此流程定義了 {link_text} 的標準操作程序。",
            "",
            "## 主要步驟",
            "",
            "### 1. 準備階段",
            "",
            "- 確認前置條件",
            "- 準備必要資源",
            "- 設定環境配置",
            "",
            "### 2. 執行階段", 
            "",
            "- 按照標準程序執行",
            "- 監控執行狀態",
            "- 記錄重要資訊",
            "",
            "### 3. 驗證階段",
            "",
            "- 驗證執行結果",
            "- 確認品質標準",
            "- 完成必要文檔",
            "",
            "## 品質檢查",
            "",
            "### 檢查清單",
            "",
            "- [ ] 所有步驟已完成",
            "- [ ] 品質標準已達成",
            "- [ ] 文檔已更新",
            "- [ ] 相關人員已通知",
            "",
            "## 工具和資源",
            "",
            "### 必要工具",
            "",
            "- Git：版本控制",
            "- IDE：開發環境",
            "- 測試框架：品質保證",
            "",
            "## 相關文檔",
            "",
            "- [工作流程總覽](../README.md)",
            "- [開發標準](../coding-standards/README.md)",
            "- [品質保證](../quality-assurance/README.md)",
            "",
            "---",
            "",
            "*本文檔遵循 [開發標準](../../../../.kiro/steering/development-standards.md)*"
        ]
    
    def generate_coding_standards_content(self, file_name: str, link_text: str) -> List[str]:
        """Generate coding standards content."""
        return [
            f"# {link_text}",
            "",
            f"本文檔定義 {link_text} 的編碼規範和最佳實踐。",
            "",
            "## 編碼原則",
            "",
            "### 可讀性",
            "",
            "- 使用有意義的變數和方法名稱",
            "- 保持程式碼簡潔明瞭",
            "- 適當添加註解說明複雜邏輯",
            "",
            "### 一致性",
            "",
            "- 遵循統一的命名約定",
            "- 使用一致的程式碼格式",
            "- 保持架構模式的一致性",
            "",
            "## 編碼規範",
            "",
            "### 命名約定",
            "",
            "- **類別名稱**：使用 PascalCase",
            "- **方法名稱**：使用 camelCase",
            "- **常數**：使用 UPPER_SNAKE_CASE",
            "",
            "### 程式碼結構",
            "",
            "- 方法長度不超過 20 行",
            "- 類別職責單一且明確",
            "- 適當使用設計模式",
            "",
            "## 品質標準",
            "",
            "### 程式碼審查",
            "",
            "- 所有程式碼必須經過審查",
            "- 至少需要 2 位審查者",
            "- 修正所有審查意見後才能合併",
            "",
            "### 測試要求",
            "",
            "- 程式碼覆蓋率 > 80%",
            "- 所有公開方法都有測試",
            "- 包含邊界條件測試",
            "",
            "## 相關文檔",
            "",
            "- [編碼標準總覽](../README.md)",
            "- [程式碼審查指南](code-review-guidelines.md)",
            "- [測試標準](../testing/README.md)",
            "",
            "---",
            "",
            "*本文檔遵循 [開發標準](../../../../.kiro/steering/development-standards.md)*"
        ]
    
    def generate_build_system_content(self, file_name: str, link_text: str) -> List[str]:
        """Generate build system content."""
        return [
            f"# {link_text}",
            "",
            f"本文檔描述 {link_text} 的配置和使用方法。",
            "",
            "## 建置系統概覽",
            "",
            "### 技術棧",
            "",
            "- **Gradle 8.x**：建置工具",
            "- **Java 21**：開發語言",
            "- **Spring Boot 3.4.5**：應用框架",
            "",
            "### 建置目標",
            "",
            "- 快速建置和測試",
            "- 一致的開發環境",
            "- 自動化品質檢查",
            "",
            "## 配置指南",
            "",
            "### Gradle 配置",
            "",
            "```gradle",
            "plugins {",
            "    id 'java'",
            "    id 'org.springframework.boot' version '3.4.5'",
            "    id 'io.spring.dependency-management' version '1.1.4'",
            "}",
            "",
            "java {",
            "    sourceCompatibility = '21'",
            "}",
            "```",
            "",
            "### 依賴管理",
            "",
            "- 使用 Gradle 版本目錄",
            "- 統一管理依賴版本",
            "- 定期更新依賴",
            "",
            "## 建置任務",
            "",
            "### 常用命令",
            "",
            "```bash",
            "# 編譯專案",
            "./gradlew build",
            "",
            "# 執行測試",
            "./gradlew test",
            "",
            "# 執行應用",
            "./gradlew bootRun",
            "```",
            "",
            "## 相關文檔",
            "",
            "- [建置系統總覽](../README.md)",
            "- [開發環境設置](../getting-started/environment-setup.md)",
            "- [CI/CD 整合](ci-cd-integration.md)",
            "",
            "---",
            "",
            "*本文檔遵循 [開發標準](../../../../.kiro/steering/development-standards.md)*"
        ]
    
    def generate_quality_assurance_content(self, file_name: str, link_text: str) -> List[str]:
        """Generate quality assurance content."""
        return [
            f"# {link_text}",
            "",
            f"本文檔描述 {link_text} 的品質保證流程和標準。",
            "",
            "## 品質標準",
            "",
            "### 程式碼品質",
            "",
            "- 程式碼覆蓋率 > 80%",
            "- 複雜度 ≤ 10 每個方法",
            "- 無程式碼重複 > 5 行",
            "",
            "### 安全標準",
            "",
            "- 無高風險或關鍵安全漏洞",
            "- 所有輸入都經過驗證",
            "- 敏感資料加密處理",
            "",
            "## 品質流程",
            "",
            "### 自動化檢查",
            "",
            "- 靜態程式碼分析",
            "- 安全漏洞掃描",
            "- 效能基準測試",
            "",
            "### 人工審查",
            "",
            "- 程式碼審查",
            "- 架構審查",
            "- 安全審查",
            "",
            "## 品質工具",
            "",
            "### 分析工具",
            "",
            "- SonarQube：程式碼品質分析",
            "- SpotBugs：靜態分析",
            "- OWASP：安全掃描",
            "",
            "### 監控工具",
            "",
            "- Micrometer：效能監控",
            "- Spring Boot Actuator：健康檢查",
            "- AWS X-Ray：分散式追蹤",
            "",
            "## 相關文檔",
            "",
            "- [品質保證總覽](../README.md)",
            "- [程式碼審查](code-review.md)",
            "- [安全標準](../../../../.kiro/steering/security-standards.md)",
            "",
            "---",
            "",
            "*本文檔遵循 [品質標準](../../../../.kiro/steering/performance-standards.md)*"
        ]
    
    def generate_tools_content(self, file_name: str, link_text: str) -> List[str]:
        """Generate tools and environment content."""
        return [
            f"# {link_text}",
            "",
            f"本文檔描述 {link_text} 的配置和使用指南。",
            "",
            "## 技術棧概覽",
            "",
            "### 後端技術",
            "",
            "- **Spring Boot 3.4.5**：應用框架",
            "- **Java 21**：程式語言",
            "- **Gradle 8.x**：建置工具",
            "",
            "### 前端技術",
            "",
            "- **Next.js 14 + React 18**：CMC 管理介面",
            "- **Angular 18 + TypeScript**：消費者應用",
            "- **shadcn/ui + Radix UI**：UI 組件",
            "",
            "### 測試框架",
            "",
            "- **JUnit 5**：單元測試",
            "- **Mockito**：模擬框架",
            "- **Cucumber 7**：BDD 測試",
            "",
            "## 環境配置",
            "",
            "### 開發環境",
            "",
            "```bash",
            "# 安裝 Java 21",
            "sdk install java 21.0.1-tem",
            "",
            "# 設定環境變數",
            "export JAVA_HOME=$HOME/.sdkman/candidates/java/current",
            "```",
            "",
            "### IDE 配置",
            "",
            "- IntelliJ IDEA 推薦設定",
            "- VS Code 擴充套件",
            "- Eclipse 配置指南",
            "",
            "## 工具整合",
            "",
            "### 版本控制",
            "",
            "- Git 工作流程",
            "- 分支策略",
            "- 提交規範",
            "",
            "### CI/CD",
            "",
            "- GitHub Actions",
            "- 自動化測試",
            "- 部署流程",
            "",
            "## 相關文檔",
            "",
            "- [工具鏈總覽](../README.md)",
            "- [環境設置](../getting-started/environment-setup.md)",
            "- [技術棧詳細說明](technology-stack/README.md)",
            "",
            "---",
            "",
            "*本文檔遵循 [開發標準](../../../../.kiro/steering/development-standards.md)*"
        ]
    
    def generate_getting_started_content(self, file_name: str, link_text: str) -> List[str]:
        """Generate getting started content."""
        return [
            f"# {link_text}",
            "",
            f"本文檔提供 {link_text} 的詳細指南。",
            "",
            "## 快速開始",
            "",
            "### 前置需求",
            "",
            "- Java 21 或更高版本",
            "- Node.js 18 或更高版本",
            "- Git 版本控制",
            "- IDE (IntelliJ IDEA 推薦)",
            "",
            "### 環境設置",
            "",
            "```bash",
            "# 複製專案",
            "git clone <repository-url>",
            "cd genai-demo",
            "",
            "# 建置專案",
            "./gradlew build",
            "",
            "# 執行測試",
            "./gradlew test",
            "",
            "# 啟動應用",
            "./gradlew bootRun",
            "```",
            "",
            "## 開發流程",
            "",
            "### 1. 功能開發",
            "",
            "- 建立功能分支",
            "- 編寫 BDD 場景",
            "- TDD 實作功能",
            "- 執行測試驗證",
            "",
            "### 2. 程式碼審查",
            "",
            "- 建立 Pull Request",
            "- 同儕審查程式碼",
            "- 修正審查意見",
            "- 合併到主分支",
            "",
            "## 最佳實踐",
            "",
            "### 編碼規範",
            "",
            "- 遵循 Java 編碼標準",
            "- 使用有意義的命名",
            "- 保持程式碼簡潔",
            "",
            "### 測試策略",
            "",
            "- 單元測試優先",
            "- 整合測試驗證",
            "- BDD 場景覆蓋",
            "",
            "## 相關資源",
            "",
            "- [開發視點總覽](../README.md)",
            "- [架構指南](../architecture/README.md)",
            "- [測試指南](../testing/README.md)",
            "",
            "---",
            "",
            "*歡迎加入開發團隊！如有問題請參考相關文檔或聯繫團隊成員。*"
        ]
    
    def create_missing_diagrams(self, diagram_issues: Dict) -> List[str]:
        """Create missing diagram files."""
        created_diagrams = []
        
        if "missing_diagrams" in diagram_issues:
            for diagram in diagram_issues["missing_diagrams"]:
                diagram_path = self.diagrams_path / diagram
                
                # Create directory if it doesn't exist
                diagram_path.parent.mkdir(parents=True, exist_ok=True)
                
                # Generate diagram content
                content = self.generate_diagram_content(diagram)
                
                with open(diagram_path, 'w', encoding='utf-8') as f:
                    f.write(content)
                
                try:
                    relative_path = diagram_path.relative_to(self.base_path)
                    created_diagrams.append(str(relative_path))
                    print(f"Created diagram: {relative_path}")
                except ValueError:
                    created_diagrams.append(str(diagram_path))
                    print(f"Created diagram: {diagram_path}")
        
        return created_diagrams
    
    def generate_diagram_content(self, diagram_name: str) -> str:
        """Generate appropriate Mermaid diagram content."""
        if "microservices-overview" in diagram_name:
            return """graph TB
    subgraph "API Gateway"
        ALB[Application Load Balancer]
        AG[API Gateway]
    end
    
    subgraph "Microservices"
        CS[Customer Service]
        OS[Order Service]
        PS[Payment Service]
        IS[Inventory Service]
    end
    
    subgraph "Data Layer"
        DB[(PostgreSQL)]
        CACHE[(Redis Cache)]
    end
    
    subgraph "Messaging"
        MSK[Amazon MSK]
    end
    
    ALB --> AG
    AG --> CS
    AG --> OS
    AG --> PS
    AG --> IS
    
    CS --> DB
    OS --> DB
    PS --> DB
    IS --> DB
    
    CS --> CACHE
    OS --> CACHE
    
    CS --> MSK
    OS --> MSK
    PS --> MSK
    IS --> MSK
"""
        elif "saga-orchestration" in diagram_name:
            return """sequenceDiagram
    participant Client
    participant OrderService
    participant PaymentService
    participant InventoryService
    participant FulfillmentService
    
    Client->>OrderService: Create Order
    OrderService->>OrderService: Validate Order
    OrderService->>InventoryService: Reserve Items
    InventoryService-->>OrderService: Items Reserved
    
    OrderService->>PaymentService: Process Payment
    PaymentService-->>OrderService: Payment Processed
    
    OrderService->>FulfillmentService: Fulfill Order
    FulfillmentService-->>OrderService: Order Fulfilled
    
    OrderService-->>Client: Order Confirmed
    
    Note over OrderService: Saga Orchestrator
    Note over PaymentService,FulfillmentService: Saga Participants
"""
        elif "distributed-system" in diagram_name:
            return """graph TB
    subgraph "Load Balancer"
        ALB[Application Load Balancer]
    end
    
    subgraph "Service Discovery"
        SD[EKS Service Discovery]
        R53[Route 53]
    end
    
    subgraph "Circuit Breaker"
        CB[Circuit Breaker Pattern]
    end
    
    subgraph "Distributed Tracing"
        XRAY[AWS X-Ray]
        TRACE[Distributed Tracing]
    end
    
    subgraph "Config Management"
        CM[Config Server]
        SECRETS[AWS Secrets Manager]
    end
    
    ALB --> SD
    SD --> R53
    SD --> CB
    CB --> XRAY
    XRAY --> TRACE
    CM --> SECRETS
"""
        elif "circuit-breaker-pattern" in diagram_name:
            return """stateDiagram-v2
    [*] --> Closed
    Closed --> Open : Failure threshold reached
    Open --> HalfOpen : Timeout period elapsed
    HalfOpen --> Closed : Success
    HalfOpen --> Open : Failure
    
    state Closed {
        [*] --> Normal
        Normal --> Monitoring : Request
        Monitoring --> Success : Success
        Monitoring --> Failure : Failure
        Success --> [*]
        Failure --> [*] : Increment failure count
    }
    
    state Open {
        [*] --> Blocking
        Blocking --> FailFast : Request
        FailFast --> [*] : Return cached response
    }
    
    state HalfOpen {
        [*] --> Testing
        Testing --> Evaluate : Limited requests
        Evaluate --> [*]
    }
"""
        elif "development-workflow" in diagram_name:
            return """flowchart TD
    A[Feature Request] --> B[Create Feature Branch]
    B --> C[Write BDD Scenarios]
    C --> D[TDD Implementation]
    D --> E[Run Tests]
    E --> F{Tests Pass?}
    F -->|No| D
    F -->|Yes| G[Code Review]
    G --> H{Review Approved?}
    H -->|No| I[Address Feedback]
    I --> D
    H -->|Yes| J[Merge to Main]
    J --> K[Deploy to Staging]
    K --> L[Integration Tests]
    L --> M{Tests Pass?}
    M -->|No| N[Fix Issues]
    N --> D
    M -->|Yes| O[Deploy to Production]
    O --> P[Monitor & Validate]
"""
        elif "tdd-cycle" in diagram_name:
            return """flowchart LR
    A[Red: Write Failing Test] --> B[Green: Make Test Pass]
    B --> C[Refactor: Improve Code]
    C --> A
    
    style A fill:#ffcccc
    style B fill:#ccffcc
    style C fill:#ccccff
"""
        elif "bdd-process" in diagram_name:
            return """flowchart TD
    A[Business Requirement] --> B[Write Gherkin Scenario]
    B --> C[Review with Stakeholders]
    C --> D{Scenario Approved?}
    D -->|No| B
    D -->|Yes| E[Implement Step Definitions]
    E --> F[Run Cucumber Tests]
    F --> G{Tests Pass?}
    G -->|No| H[Implement Feature]
    H --> F
    G -->|Yes| I[Feature Complete]
"""
        elif "code-review-process" in diagram_name:
            return """flowchart TD
    A[Create Pull Request] --> B[Automated Checks]
    B --> C{Checks Pass?}
    C -->|No| D[Fix Issues]
    D --> A
    C -->|Yes| E[Assign Reviewers]
    E --> F[Code Review]
    F --> G{Review Approved?}
    G -->|No| H[Address Feedback]
    H --> D
    G -->|Yes| I[Merge to Main]
    I --> J[Deploy Pipeline]
"""
        elif "test-pyramid" in diagram_name:
            return """graph TB
    subgraph "Test Pyramid"
        E2E[End-to-End Tests<br/>5% - Slow, Expensive]
        INT[Integration Tests<br/>15% - Medium Speed]
        UNIT[Unit Tests<br/>80% - Fast, Cheap]
    end
    
    E2E --> INT
    INT --> UNIT
    
    style E2E fill:#ffcccc
    style INT fill:#ffffcc
    style UNIT fill:#ccffcc
"""
        elif "performance-testing" in diagram_name:
            return """graph TB
    subgraph "Performance Testing Architecture"
        LG[Load Generator]
        APP[Application Under Test]
        DB[(Database)]
        CACHE[(Cache)]
        MON[Monitoring]
    end
    
    LG --> APP
    APP --> DB
    APP --> CACHE
    APP --> MON
    
    MON --> METRICS[Performance Metrics]
    METRICS --> REPORT[Performance Report]
"""
        elif "ci-cd-pipeline" in diagram_name:
            return """flowchart LR
    A[Code Commit] --> B[Build]
    B --> C[Unit Tests]
    C --> D[Integration Tests]
    D --> E[Security Scan]
    E --> F[Deploy to Staging]
    F --> G[E2E Tests]
    G --> H[Deploy to Production]
    H --> I[Monitor]
    
    style A fill:#e1f5fe
    style H fill:#c8e6c9
    style I fill:#fff3e0
"""
        elif "monitoring-architecture" in diagram_name:
            return """graph TB
    subgraph "Application Layer"
        APP[Spring Boot Application]
        ACTUATOR[Spring Boot Actuator]
    end
    
    subgraph "Metrics Collection"
        MICROMETER[Micrometer]
        PROMETHEUS[Prometheus]
    end
    
    subgraph "Tracing"
        XRAY[AWS X-Ray]
        JAEGER[Jaeger]
    end
    
    subgraph "Visualization"
        GRAFANA[Grafana]
        CLOUDWATCH[CloudWatch]
    end
    
    APP --> ACTUATOR
    ACTUATOR --> MICROMETER
    MICROMETER --> PROMETHEUS
    APP --> XRAY
    XRAY --> JAEGER
    PROMETHEUS --> GRAFANA
    XRAY --> CLOUDWATCH
"""
        else:
            return f"""graph TB
    A[{diagram_name.replace('-', ' ').title()}] --> B[Component 1]
    A --> C[Component 2]
    B --> D[Output 1]
    C --> E[Output 2]
"""
    
    def fix_links(self) -> Dict[str, List[str]]:
        """Fix all broken links by creating missing files and diagrams."""
        print("🔧 Starting Development Viewpoint Link Fixing...")
        print("=" * 60)
        
        # Load validation results
        try:
            results = self.load_validation_results()
        except FileNotFoundError:
            print("❌ Please run the link validation script first!")
            return {"error": ["Validation results not found"]}
        
        # Create missing files
        print("\n📄 Creating missing files...")
        created_files = self.create_missing_files(results["broken_links"])
        
        # Create missing diagrams
        print("\n📊 Creating missing diagrams...")
        created_diagrams = self.create_missing_diagrams(results["diagram_issues"])
        
        # Summary
        print("\n" + "=" * 60)
        print("✅ LINK FIXING COMPLETED")
        print("=" * 60)
        print(f"Created Files: {len(created_files)}")
        print(f"Created Diagrams: {len(created_diagrams)}")
        
        if created_files:
            print("\n📄 Created Files:")
            for file in created_files:
                print(f"  - {file}")
        
        if created_diagrams:
            print("\n📊 Created Diagrams:")
            for diagram in created_diagrams:
                print(f"  - {diagram}")
        
        return {
            "created_files": created_files,
            "created_diagrams": created_diagrams
        }

def main():
    """Main function to fix development viewpoint links."""
    fixer = LinkFixer()
    results = fixer.fix_links()
    
    if "error" in results:
        return 1
    
    print(f"\n🎉 Successfully created {len(results['created_files'])} files and {len(results['created_diagrams'])} diagrams!")
    print("\n💡 Next steps:")
    print("  1. Run the link validation script again to verify fixes")
    print("  2. Review and customize the generated content")
    print("  3. Update any remaining broken links manually")
    
    return 0

if __name__ == "__main__":
    import sys
    sys.exit(main())