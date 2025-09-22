
<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 文件元資料標準格式

## 概覽

本文件定義了專案中所有文件的元資料 Front Matter 標準格式，確保文件的一致性、可追蹤性和自動化處理能力。

## Viewpoint 文件元資料標準

### 基本格式

```yaml
---
title: "文件標題"
viewpoint: "functional|information|concurrency|development|deployment|operational"
perspective: ["security", "performance", "availability", "evolution", "usability", "regulation", "location", "cost"]
stakeholders: ["architect", "developer", "operator", "security-engineer", "business-analyst", "product-manager", "end-user"]
related_viewpoints: ["viewpoint1", "viewpoint2"]
related_documents: ["doc1.md", "doc2.md"]
diagrams: ["diagram1.mmd"  # 注意：現在使用包含 Mermaid 代碼塊的 .md 文件, "diagram2.puml", "diagram3.excalidraw"]
last_updated: "YYYY-MM-DD"
version: "1.0"
author: "Architecture Team"
review_status: "draft|reviewed|approved"
complexity: "low|medium|high"
priority: "low|medium|high|critical"
tags: ["tag1", "tag2", "tag3"]
---
```

### 欄位說明

#### 必填欄位

- **title**: 文件標題，應該清楚描述文件內容
- **viewpoint**: 所屬的架構視點，必須是七大視點之一
- **last_updated**: 最後更新日期，格式為 YYYY-MM-DD
- **version**: 文件版本號，使用語意化版本控制
- **author**: 文件作者或負責團隊

#### 選填欄位

- **perspective**: 相關的架構觀點陣列，可以是多個
- **stakeholders**: 相關利害關係人陣列
- **related_viewpoints**: 相關的其他視點
- **related_documents**: 相關文件的相對路徑
- **diagrams**: 相關圖表檔案的相對路徑
- **review_status**: 文件審查狀態
- **complexity**: 文件複雜度
- **priority**: 文件優先級
- **tags**: 自由標籤，用於分類和搜尋

### 範例

```yaml
---
title: "功能視點 - 領域模型設計"
viewpoint: "functional"
perspective: ["security", "performance", "evolution"]
stakeholders: ["architect", "developer", "business-analyst"]
related_viewpoints: ["information", "development"]
related_documents: ["../information/domain-events.md", "../development/testing-strategy.md"]
diagrams: ["../diagrams/viewpoints/functional/domain-model.mmd"  # 注意：現在使用包含 Mermaid 代碼塊的 .md 文件, "../diagrams/viewpoints/functional/bounded-contexts.puml"]
last_updated: "2025-01-21"
version: "2.1"
author: "Architecture Team"
review_status: "approved"
complexity: "high"
priority: "critical"
tags: ["ddd", "domain-model", "aggregates", "bounded-context"]
---
```

## Perspective 文件元資料標準

### 基本格式

```yaml
---
title: "觀點標題"
perspective_type: "security|performance|availability|evolution|usability|regulation|location|cost"
applicable_viewpoints: ["functional", "information", "concurrency", "development", "deployment", "operational"]
quality_attributes: ["attribute1", "attribute2", "attribute3"]
stakeholders: ["architect", "developer", "operator", "security-engineer", "business-analyst"]
related_perspectives: ["perspective1", "perspective2"]
related_documents: ["doc1.md", "doc2.md"]
patterns: ["pattern1", "pattern2"]
tools: ["tool1", "tool2"]
metrics: ["metric1", "metric2"]
last_updated: "YYYY-MM-DD"
version: "1.0"
author: "Architecture Team"
review_status: "draft|reviewed|approved"
impact_level: "low|medium|high|critical"
implementation_difficulty: "easy|medium|hard|very-hard"
tags: ["tag1", "tag2", "tag3"]
---
```

### 欄位說明

#### 必填欄位

- **title**: 觀點標題
- **perspective_type**: 觀點類型，必須是八大觀點之一
- **applicable_viewpoints**: 適用的視點陣列
- **quality_attributes**: 相關的品質屬性
- **last_updated**: 最後更新日期
- **version**: 文件版本號
- **author**: 文件作者

#### 選填欄位

- **stakeholders**: 相關利害關係人
- **related_perspectives**: 相關的其他觀點
- **related_documents**: 相關文件
- **patterns**: 相關的設計模式
- **tools**: 相關的工具和技術
- **metrics**: 相關的度量指標
- **review_status**: 審查狀態
- **impact_level**: 影響程度
- **implementation_difficulty**: 實現難度
- **tags**: 標籤

### 範例

```yaml
---
title: "安全性觀點 - 認證與授權"
perspective_type: "security"
applicable_viewpoints: ["functional", "information", "development", "deployment", "operational"]
quality_attributes: ["confidentiality", "integrity", "availability", "accountability"]
stakeholders: ["security-engineer", "architect", "developer", "operator"]
related_perspectives: ["performance", "usability", "regulation"]
related_documents: ["../viewpoints/functional/user-management.md", "../viewpoints/deployment/security-configuration.md"]
patterns: ["oauth2", "jwt", "rbac", "zero-trust"]
tools: ["spring-security", "keycloak", "vault"]
metrics: ["authentication-success-rate", "authorization-latency", "security-incidents"]
last_updated: "2025-01-21"
version: "1.3"
author: "Security Team"
review_status: "approved"
impact_level: "critical"
implementation_difficulty: "medium"
tags: ["authentication", "authorization", "oauth2", "jwt", "security"]
---
```

## 圖表元資料標準

### 基本格式

```yaml
---
title: "圖表標題"
type: "mermaid|plantuml|excalidraw"
format: "mmd|puml|excalidraw|png|svg"
viewpoint: "functional|information|concurrency|development|deployment|operational"
perspective: ["security", "performance", "availability"]
diagram_level: "overview|detailed|conceptual"
target_audience: ["architect", "developer", "stakeholder"]
description: "圖表描述和用途"
related_documents: ["doc1.md", "doc2.md"]
source_file: "diagram-source.mmd"  # 注意：現在使用包含 Mermaid 代碼塊的 .md 文件
generated_files: ["diagram.svg"]
last_updated: "YYYY-MM-DD"
version: "1.0"
author: "Architecture Team"
auto_generated: true|false
generation_source: "code|manual|template"
update_frequency: "on-demand|weekly|monthly"
complexity: "low|medium|high"
maintenance_notes: "維護說明"
tags: ["tag1", "tag2"]
---
```

### 欄位說明

#### 必填欄位

- **title**: 圖表標題
- **type**: 圖表類型 (mermaid, plantuml, excalidraw)
- **format**: 檔案格式
- **description**: 圖表描述
- **last_updated**: 最後更新日期
- **version**: 版本號
- **author**: 作者

#### 選填欄位

- **viewpoint**: 所屬視點
- **perspective**: 相關觀點
- **diagram_level**: 圖表詳細程度
- **target_audience**: 目標受眾
- **related_documents**: 相關文件
- **source_file**: 原始檔案
- **generated_files**: 生成的檔案
- **auto_generated**: 是否自動生成
- **generation_source**: 生成來源
- **update_frequency**: 更新頻率
- **complexity**: 複雜度
- **maintenance_notes**: 維護說明
- **tags**: 標籤

### 範例

```yaml
---
title: "領域模型類圖 - 客戶聚合"
type: "plantuml"
format: "puml"
viewpoint: "functional"
perspective: ["security", "evolution"]
diagram_level: "detailed"
target_audience: ["architect", "developer"]
description: "展示客戶聚合根的詳細設計，包括實體、值對象和領域服務"
related_documents: ["../viewpoints/functional/domain-model.md", "../viewpoints/information/domain-events.md"]
source_file: "customer-aggregate.puml"
generated_files: ["customer-aggregate.svg"]
last_updated: "2025-01-21"
version: "2.0"
author: "Architecture Team"
auto_generated: false
generation_source: "manual"
update_frequency: "on-demand"
complexity: "high"
maintenance_notes: "當客戶聚合根發生變更時需要更新此圖表"
tags: ["domain-model", "aggregate", "customer", "ddd"]
---
```

## 一般文件元資料標準

### 基本格式

```yaml
---
title: "文件標題"
document_type: "guide|reference|tutorial|specification|report|template"
category: ["architecture", "development", "deployment", "operations"]
audience: ["developer", "architect", "operator", "business"]
difficulty_level: "beginner|intermediate|advanced|expert"
estimated_reading_time: "5 minutes"
prerequisites: ["prerequisite1", "prerequisite2"]
related_documents: ["doc1.md", "doc2.md"]
external_links: ["https://example.com"]
last_updated: "YYYY-MM-DD"
version: "1.0"
author: "Team Name"
reviewers: ["reviewer1", "reviewer2"]
review_status: "draft|reviewed|approved|deprecated"
language: "zh-TW|en"
translation_status: "original|translated|needs-update"
translation_source: "original-doc.md"
keywords: ["keyword1", "keyword2"]
tags: ["tag1", "tag2"]
---
```

### 欄位說明

#### 必填欄位

- **title**: 文件標題
- **document_type**: 文件類型
- **last_updated**: 最後更新日期
- **version**: 版本號
- **author**: 作者
- **language**: 語言

#### 選填欄位

- **category**: 文件分類
- **audience**: 目標受眾
- **difficulty_level**: 難度等級
- **estimated_reading_time**: 預估閱讀時間
- **prerequisites**: 前置條件
- **related_documents**: 相關文件
- **external_links**: 外部連結
- **reviewers**: 審查者
- **review_status**: 審查狀態
- **translation_status**: 翻譯狀態
- **translation_source**: 翻譯來源
- **keywords**: 關鍵字
- **tags**: 標籤

## 元資料驗證規則

### 必填欄位驗證

```yaml

<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 驗證規則範例
validation_rules:
  title:
    required: true
    type: string
    min_length: 5
    max_length: 100
  
  viewpoint:
    required: true
    type: string
    allowed_values: ["functional", "information", "concurrency", "development", "deployment", "operational"]
  
  last_updated:
    required: true
    type: date
    format: "YYYY-MM-DD"
  
  version:
    required: true
    type: string
    pattern: "^\\d+\\.\\d+(\\.\\d+)?$"
  
  perspective:
    required: false
    type: array
    allowed_values: ["security", "performance", "availability", "evolution", "usability", "regulation", "location", "cost"]
```

### 一致性檢查

```yaml
consistency_checks:
  # 檢查相關文件是否存在
  related_documents:
    check: file_exists
    base_path: "docs/"
  
  # 檢查圖表檔案是否存在
  diagrams:
    check: file_exists
    base_path: "docs/diagrams/"
  
  # 檢查視點和觀點的一致性
  viewpoint_perspective_consistency:
    check: logical_consistency
    rules:
      - if_viewpoint: "security"
        then_perspective_should_include: ["security"]
```

## 自動化處理

### 元資料提取

```bash
#!/bin/bash

<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 提取所有文件的元資料
find docs/ -name "*.md" -exec grep -l "^---$" {} \; | while read file; do
    echo "Processing: $file"
    # 提取 Front Matter
    sed -n '/^---$/,/^---$/p' "$file" | head -n -1 | tail -n +2
done
```

### 元資料驗證

```python

<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# Python 腳本範例
import yaml
import os
from pathlib import Path

def validate_metadata(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 提取 Front Matter
    if content.startswith('---\n'):
        end_index = content.find('\n---\n', 4)
        if end_index != -1:
            front_matter = content[4:end_index]
            try:
                metadata = yaml.safe_load(front_matter)
                return validate_fields(metadata)
            except yaml.YAMLError as e:
                return f"YAML parsing error: {e}"
    
    return "No valid front matter found"

def validate_fields(metadata):
    errors = []
    
    # 檢查必填欄位
    required_fields = ['title', 'last_updated', 'version', 'author']
    for field in required_fields:
        if field not in metadata:
            errors.append(f"Missing required field: {field}")
    
    # 檢查日期格式
    if 'last_updated' in metadata:
        try:
            from datetime import datetime
            datetime.strptime(metadata['last_updated'], '%Y-%m-%d')
        except ValueError:
            errors.append("Invalid date format for last_updated")
    
    return errors if errors else "Valid"
```

### 元資料索引生成

```python

<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 生成文件索引
def generate_document_index():
    index = {
        'viewpoints': {},
        'perspectives': {},
        'documents': [],
        'diagrams': []
    }
    
    for md_file in Path('docs').rglob('*.md'):
        metadata = extract_metadata(md_file)
        if metadata:
            doc_info = {
                'path': str(md_file),
                'metadata': metadata
            }
            
            if 'viewpoint' in metadata:
                viewpoint = metadata['viewpoint']
                if viewpoint not in index['viewpoints']:
                    index['viewpoints'][viewpoint] = []
                index['viewpoints'][viewpoint].append(doc_info)
            
            if 'perspective_type' in metadata:
                perspective = metadata['perspective_type']
                if perspective not in index['perspectives']:
                    index['perspectives'][perspective] = []
                index['perspectives'][perspective].append(doc_info)
            
            index['documents'].append(doc_info)
    
    return index
```

## 最佳實踐

### 元資料撰寫指南

1. **保持一致性**: 使用標準化的欄位名稱和值
2. **及時更新**: 文件變更時同步更新元資料
3. **詳細描述**: 提供足夠的描述資訊
4. **正確分類**: 準確設定視點、觀點和標籤
5. **關聯性**: 正確設定相關文件和圖表的連結

### 維護建議

1. **定期檢查**: 定期驗證元資料的正確性
2. **自動化驗證**: 使用 CI/CD 流程自動驗證元資料
3. **版本控制**: 追蹤元資料的變更歷史
4. **文件化**: 記錄元資料標準的變更

### 工具整合

1. **編輯器支援**: 配置編輯器的 YAML 語法高亮和驗證
2. **Git Hooks**: 使用 Git hooks 在提交前驗證元資料
3. **CI/CD 整合**: 在建置流程中包含元資料驗證
4. **文件生成**: 基於元資料自動生成索引和導航

這個標準格式確保了文件的一致性和可維護性，同時支援自動化處理和分析。