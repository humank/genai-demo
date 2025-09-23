# 文件維護指南 (Documentation Maintenance Guide)

> **基於 Rozanski & Woods 方法論的架構文件維護最佳實踐**

## 概覽

本指南提供維護基於 Rozanski & Woods Viewpoints & Perspectives 架構文件結構的完整指導，確保文件品質、一致性和可用性。

## 文件結構概覽

### 核心結構

```
docs/
├── README.md                    # 文件中心導航
├── viewpoints/                  # 七大架構視點
│   ├── functional/             # 功能視點
│   ├── information/            # 資訊視點
│   ├── concurrency/            # 並發視點
│   ├── development/            # 開發視點
│   ├── deployment/             # 部署視點
│   └── operational/            # 運營視點
├── perspectives/               # 八大架構觀點
│   ├── security/              # 安全性觀點
│   ├── performance/           # 性能觀點
│   ├── availability/          # 可用性觀點
│   ├── evolution/             # 演進性觀點
│   ├── usability/             # 使用性觀點
│   ├── regulation/            # 法規觀點
│   ├── location/              # 位置觀點
│   └── cost/                  # 成本觀點
├── diagrams/                   # 圖表資源
│   ├── viewpoints/            # 視點相關圖表
│   └── perspectives/          # 觀點相關圖表
├── templates/                  # 文件模板
└── en/                        # 英文版本 (自動生成)
```

## 維護工作流程

### 1. 日常維護任務

#### 每週檢查清單
- [ ] 檢查翻譯品質和一致性
- [ ] 驗證內部連結有效性
- [ ] 更新過時的技術資訊
- [ ] 檢查圖表和文件同步狀態

#### 每月檢查清單
- [ ] 執行完整的文件品質檢查
- [ ] 更新專業術語字典
- [ ] 檢查跨視點引用的準確性
- [ ] 驗證使用者體驗路徑

#### 每季檢查清單
- [ ] 評估文件結構的有效性
- [ ] 收集使用者反饋並改進
- [ ] 更新維護指南和最佳實踐
- [ ] 執行架構文件合規性審查

### 2. 自動化維護工具

#### 翻譯系統
```bash
# 測試翻譯系統狀態
./scripts/test-translation-system.sh

# 檢查翻譯品質
./scripts/check-translation-quality.sh

# 修復翻譯品質問題
python3 scripts/fix-translation-quality.py
```

#### 文件品質檢查
```bash
# 檢查文件品質
./scripts/check-documentation-quality.sh

# 驗證連結有效性
node scripts/check-links-advanced.js

# 驗證圖表元資料
python3 scripts/validate-diagrams.py
```

#### 使用者體驗測試
```bash
# 執行使用者體驗測試
python3 scripts/test-user-experience.py

# 檢視測試報告
cat reports-summaries/testing/user-experience-test-report.md
```

## 內容維護標準

### 1. Viewpoint 文件維護

#### 標準結構檢查
每個 Viewpoint 文件必須包含：
- [ ] **概覽**: 清楚定義視點的目的和範圍
- [ ] **利害關係人**: 明確列出主要和次要關注者
- [ ] **關注點**: 詳細說明此視點關注的架構問題
- [ ] **架構元素**: 描述相關的架構組件和關係
- [ ] **品質屬性考量**: 說明各個 Perspective 在此視點的應用
- [ ] **相關圖表**: 連結到相關的視覺化資源
- [ ] **與其他視點的關聯**: 描述跨視點的關係
- [ ] **實現指南**: 提供具體的實現建議
- [ ] **驗證標準**: 定義如何驗證此視點的實現品質

#### 品質檢查標準
- **一致性**: 術語使用符合專業字典定義
- **完整性**: 所有必要章節都已完成
- **準確性**: 技術資訊與實際實現一致
- **可讀性**: 結構清晰，語言簡潔明瞭
- **關聯性**: 與其他視點和觀點的引用準確

### 2. Perspective 文件維護

#### 標準結構檢查
每個 Perspective 文件必須包含：
- [ ] **概覽**: 定義品質屬性和重要性
- [ ] **品質屬性**: 明確定義主要和次要品質屬性
- [ ] **跨視點應用**: 說明如何在各個視點中體現
- [ ] **設計策略**: 實現此觀點的設計方法
- [ ] **實現技術**: 支援的技術和工具
- [ ] **測試和驗證**: 驗證方法和標準
- [ ] **監控和度量**: 相關的監控指標

#### 跨視點一致性檢查
- **功能視點**: 業務邏輯層面的品質屬性體現
- **資訊視點**: 資料和資訊流的品質考量
- **並發視點**: 並發處理的品質影響
- **開發視點**: 開發過程的品質保證
- **部署視點**: 部署環境的品質要求
- **運營視點**: 運營階段的品質監控

### 3. 圖表維護

#### 圖表類型和用途
- **Mermaid (.mmd)**: 系統架構圖、流程圖、時序圖
- **PlantUML (.puml)**: 詳細 UML 圖、複雜類圖、設計文件
- **Excalidraw (.excalidraw)**: 概念設計、腦力激盪、手繪風格圖

#### 圖表維護檢查
- [ ] **同步性**: 圖表內容與文件描述一致
- [ ] **可讀性**: 圖表清晰易懂，標籤完整
- [ ] **標準化**: 使用統一的顏色、字體和樣式
- [ ] **版本控制**: 圖表變更有適當的版本記錄
- [ ] **格式轉換**: SVG 版本與源文件同步

#### 自動化圖表生成
```bash
# 生成所有圖表
./scripts/generate-all-diagrams.sh

# 驗證圖表語法
python3 scripts/validate-diagrams.py

# 同步圖表和文件引用
python3 scripts/sync-diagram-references.py
```

## 翻譯維護

### 1. 術語一致性

#### 專業術語字典維護
位置：`docs/.terminology.json`

重要類別：
- **rozanski_woods_viewpoints**: Rozanski & Woods 視點術語
- **rozanski_woods_perspectives**: Rozanski & Woods 觀點術語
- **ddd_strategic_patterns**: DDD 戰略模式術語
- **ddd_tactical_patterns**: DDD 戰術模式術語
- **stakeholder_terminology**: 利害關係人術語
- **design_strategies**: 設計策略術語

#### 術語更新流程
1. **識別新術語**: 在文件中發現新的專業術語
2. **研究標準翻譯**: 查找業界標準翻譯
3. **更新字典**: 在 `.terminology.json` 中添加新術語
4. **驗證一致性**: 執行翻譯品質檢查
5. **應用修正**: 使用修正腳本更新所有文件

### 2. 翻譯品質保證

#### 自動翻譯觸發
- **Kiro Hook**: 文件變更時自動觸發翻譯
- **手動觸發**: 使用翻譯腳本進行批量翻譯
- **品質修正**: 使用品質修正腳本改善翻譯

#### 翻譯驗證流程
```bash
# 1. 檢查翻譯完整性
./scripts/check-translation-quality.sh

# 2. 修正術語一致性
python3 scripts/fix-translation-quality.py

# 3. 驗證連結有效性
node scripts/check-links-advanced.js docs/en/
```

## 品質保證流程

### 1. 內容審查標準

#### 技術準確性
- [ ] 程式碼範例可執行且正確
- [ ] 架構描述與實際實現一致
- [ ] 技術規格符合最新標準
- [ ] 外部連結有效且相關

#### 結構完整性
- [ ] 所有必要章節都已完成
- [ ] 章節間的邏輯關係清晰
- [ ] 交叉引用準確無誤
- [ ] 導航路徑完整可用

#### 語言品質
- [ ] 語法正確，表達清晰
- [ ] 術語使用一致
- [ ] 語調適合目標讀者
- [ ] 翻譯準確且自然

### 2. 自動化品質檢查

#### GitHub Actions 工作流程
位置：`.github/workflows/documentation-quality.yml`

檢查項目：
- 連結有效性驗證
- Markdown 語法檢查
- 術語一致性驗證
- 圖表同步檢查
- 翻譯完整性驗證

#### 本地品質檢查
```bash
# 執行完整品質檢查
./scripts/check-documentation-quality.sh

# 檢查特定類型問題
./scripts/check-translation-quality.sh
node scripts/check-links-advanced.js
python3 scripts/validate-diagrams.py
python3 scripts/validate-metadata.py
```

## 使用者體驗優化

### 1. 導航體驗

#### 多層次導航設計
- **主導航**: 文件中心提供總覽和快速入口
- **分類導航**: 視點和觀點各有專門的導航頁面
- **交叉導航**: 視點-觀點矩陣提供交叉引用
- **角色導航**: 針對不同角色提供專門的導航路徑

#### 導航優化檢查
- [ ] 所有主要入口點都可訪問
- [ ] 導航路徑邏輯清晰
- [ ] 麵包屑導航準確
- [ ] 相關內容推薦有效

### 2. 搜尋體驗

#### 內容可發現性
- **標題結構**: 使用清晰的標題層次
- **關鍵字優化**: 在重要位置使用關鍵術語
- **標籤系統**: 使用一致的標籤和分類
- **摘要描述**: 提供清晰的章節摘要

#### 搜尋優化建議
- 在文件開頭提供清晰的摘要
- 使用標準化的術語和關鍵字
- 提供多種表達方式的同義詞
- 建立完整的交叉引用系統

### 3. 可訪問性

#### 無障礙設計
- **結構化標題**: 使用正確的標題層次 (H1-H6)
- **替代文字**: 為所有圖片提供描述性文字
- **連結描述**: 連結文字清楚說明目標內容
- **顏色對比**: 確保足夠的顏色對比度

#### 多語言支援
- **平行結構**: 中英文版本保持相同的結構
- **文化適應**: 考慮不同文化背景的理解差異
- **本地化**: 適應不同地區的表達習慣

## 故障排除

### 1. 常見問題和解決方案

#### 翻譯問題
**問題**: 術語翻譯不一致
**解決方案**:
```bash
# 1. 更新術語字典
vim docs/.terminology.json

# 2. 執行術語修正
python3 scripts/fix-translation-quality.py

# 3. 驗證修正結果
./scripts/check-translation-quality.sh
```

**問題**: 自動翻譯品質差
**解決方案**:
- 檢查 Amazon Q CLI 配置
- 更新翻譯提示模板
- 手動調整重要文件的翻譯

#### 連結問題
**問題**: 內部連結失效
**解決方案**:
```bash
# 1. 檢查連結狀態
node scripts/check-links-advanced.js

# 2. 修復連結重定向
python3 scripts/create-link-redirects.py

# 3. 更新連結引用
# 手動修正或使用文字替換工具
```

#### 圖表問題
**問題**: 圖表與文件不同步
**解決方案**:
```bash
# 1. 重新生成圖表
./scripts/generate-all-diagrams.sh

# 2. 同步圖表引用
python3 scripts/sync-diagram-references.py

# 3. 驗證圖表語法
python3 scripts/validate-diagrams.py
```

### 2. 緊急修復流程

#### 嚴重問題處理
1. **立即評估**: 確定問題的影響範圍
2. **快速修復**: 實施臨時解決方案
3. **根本分析**: 找出問題的根本原因
4. **永久修復**: 實施長期解決方案
5. **預防措施**: 更新檢查流程防止再次發生

#### 回滾程序
```bash
# 1. 檢查 Git 歷史
git log --oneline docs/

# 2. 回滾到穩定版本
git checkout <stable-commit> -- docs/

# 3. 重新執行品質檢查
./scripts/check-documentation-quality.sh

# 4. 提交修復
git add docs/
git commit -m "Rollback documentation to stable state"
```

## 持續改進

### 1. 效能監控

#### 維護指標
- **翻譯完整性**: 英文版本覆蓋率
- **連結健康度**: 有效連結百分比
- **內容新鮮度**: 最後更新時間分佈
- **使用者滿意度**: 反饋和評分

#### 定期評估
- **月度報告**: 維護活動和問題統計
- **季度審查**: 文件結構和流程評估
- **年度規劃**: 重大改進和升級計劃

### 2. 工具和流程改進

#### 自動化增強
- 擴展自動化檢查範圍
- 改進翻譯品質和速度
- 增強圖表生成能力
- 優化使用者體驗測試

#### 流程優化
- 簡化維護工作流程
- 減少手動操作需求
- 提高問題檢測速度
- 改善修復效率

## 聯絡和支援

### 維護團隊
- **架構文件負責人**: 負責整體文件策略和品質
- **翻譯協調員**: 負責翻譯品質和術語一致性
- **技術寫作員**: 負責內容創作和編輯
- **工具維護員**: 負責自動化工具和腳本

### 報告問題
- **GitHub Issues**: 用於追蹤和管理文件問題
- **內部溝通**: 團隊內部的快速溝通和協調
- **使用者反饋**: 收集和處理使用者的建議和問題

---

**最後更新**: 2025-01-21  
**版本**: 1.0  
**維護者**: 架構文件團隊