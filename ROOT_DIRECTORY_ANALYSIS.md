# 根目錄結構分析與優化建議

**分析日期**: 2025-11-23
**目的**: 優化專案根目錄結構，提升可維護性和組織性

---

## 📊 當前目錄結構概覽

```
.
├── .git/                    # Git 版本控制
├── .github/                 # GitHub Actions 和配置
├── .gradle/                 # Gradle 快取
├── .idea/                   # IntelliJ IDEA 配置
├── .kiro/                   # Kiro IDE 配置 (1.5M)
├── .settings/               # Eclipse 配置
├── .vscode/                 # VS Code 配置
├── app/                     # 主要應用程式碼
├── build/                   # 構建輸出
├── cmc-frontend/            # CMC 前端
├── config/                  # 配置檔案
├── consumer-frontend/       # 消費者前端
├── deployment/              # 部署腳本 (40K)
├── docs/                    # 文檔 (13M)
├── gradle/                  # Gradle wrapper
├── infrastructure/          # AWS CDK 基礎設施 (352M)
├── logs/                    # 日誌檔案
├── node_modules/            # Node.js 依賴
├── reports-summaries/       # 報告摘要 (2.8M)
├── scripts/                 # 工具腳本 (404K)
├── e2e-tests/              # E2E 和整合測試 (1.1M)
├── tools/                   # 工具 (plantuml.jar)
└── [配置檔案]               # 根目錄配置檔案
```

---

## 🔍 問題分析

### 1. 根目錄檔案過多 (18 個檔案)

**當前根目錄檔案**:
```
.DS_Store                          # ❌ macOS 系統檔案
.editorconfig                      # ✅ 編輯器配置
.env.example                       # ✅ 環境變數範例
.gitattributes                     # ✅ Git 屬性
.gitignore                         # ✅ Git 忽略
.markdownlint.json                 # ✅ Markdown 檢查配置
build.gradle                       # ✅ Gradle 主配置
CONTRIBUTING.md                    # ✅ 貢獻指南
docker-compose-redis-dev.yml       # ⚠️ 可移動
docker-compose-redis-ha.yml        # ⚠️ 可移動
docker-compose.yml                 # ✅ 主要 Docker Compose
Dockerfile                         # ✅ Docker 映像
excalidraw.log                     # ❌ 日誌檔案
gradle.properties                  # ✅ Gradle 屬性
gradlew                            # ✅ Gradle wrapper
gradlew.bat                        # ✅ Gradle wrapper (Windows)
LICENSE                            # ✅ 授權
Makefile                           # ✅ Make 命令
package-lock.json                  # ✅ npm 鎖定檔案
package.json                       # ✅ npm 配置
README.md                          # ✅ 專案說明
settings.gradle                    # ✅ Gradle 設定
sonar-project.properties           # ✅ SonarQube 配置
suppress-unused-warnings.txt       # ⚠️ 臨時檔案
```

### 2. 日誌檔案散落

**問題**:
- `excalidraw.log` 在根目錄
- `logs/` 目錄存在但未統一使用
- MCP 日誌在 `logs/` 目錄

### 3. Docker Compose 檔案組織

**問題**:
- 3 個 docker-compose 檔案在根目錄
- Redis 相關的應該分組

### 4. 臨時/開發檔案

**問題**:
- `suppress-unused-warnings.txt` - 臨時追蹤檔案
- `.DS_Store` - macOS 系統檔案

### 5. 目錄用途不明確

**問題**:
- `config/` 只有一個 `sentinel.conf`
- `tools/` 只有 `plantuml.jar`
- ✅ `staging-tests/` 已重命名為 `e2e-tests/`（更清楚表達用途）

---

## 💡 優化建議

### 建議 1: 清理根目錄檔案

#### 1.1 刪除不需要的檔案
```bash
# 刪除系統檔案
rm .DS_Store

# 刪除日誌檔案（應該在 logs/ 目錄）
rm excalidraw.log

# 刪除或移動臨時檔案
rm suppress-unused-warnings.txt  # 或移到 docs/development/
```

#### 1.2 移動 Docker Compose 檔案
```bash
# 建議結構
deployment/docker/
├── docker-compose.yml              # 主要配置（符號連結到根目錄）
├── docker-compose-redis-dev.yml
└── docker-compose-redis-ha.yml
```

**理由**:
- 集中管理部署相關檔案
- 根目錄保持簡潔
- 保留主要 docker-compose.yml 在根目錄（或符號連結）

---

### 建議 2: 統一日誌管理

#### 2.1 更新 .gitignore
```gitignore
# Logs
logs/
*.log
*.log.*

# 但保留 logs 目錄結構
!logs/.gitkeep
```

#### 2.2 建立 logs 目錄結構
```
logs/
├── .gitkeep
├── mcp/           # MCP 伺服器日誌
├── app/           # 應用程式日誌
└── scripts/       # 腳本執行日誌
```

---

### 建議 3: 重組配置目錄

#### 3.1 當前問題
```
config/
└── sentinel.conf    # 只有一個檔案
```

#### 3.2 建議結構
```
config/
├── redis/
│   ├── sentinel.conf
│   ├── redis-dev.conf
│   └── redis-ha.conf
├── docker/
│   ├── docker-compose-redis-dev.yml  # 從根目錄移過來
│   └── docker-compose-redis-ha.yml
└── sonar/
    └── sonar-project.properties      # 從根目錄移過來（可選）
```

**或者更簡單的方案**: 如果 config/ 只用於 Redis，重命名為 `config/redis/`

---

### 建議 4: 整合工具目錄

#### 4.1 當前問題
```
tools/
└── plantuml.jar    # 只有一個檔案

scripts/
└── [33 個腳本]
```

#### 4.2 建議方案 A: 合併到 scripts
```
scripts/
├── tools/
│   └── plantuml.jar
├── build/
├── deployment/
└── development/
```

#### 4.3 建議方案 B: 保持分離但明確用途
```
tools/
├── plantuml/
│   └── plantuml.jar
└── README.md       # 說明工具用途
```

---

### 建議 5: 釐清測試目錄

#### 5.1 當前結構
```
app/src/test/          # 單元測試、整合測試
staging-tests/         # 1.1M - 用途？
```

#### 5.2 建議
1. **檢查 staging-tests/ 內容**
2. **如果是 E2E 測試**: 重命名為 `e2e-tests/` 或移到 `app/src/e2e/`
3. **如果是部署測試**: 移到 `deployment/tests/`
4. **如果已過時**: 刪除或歸檔

---

### 建議 6: 文檔組織優化

#### 6.1 當前大小
```
docs/              13M
reports-summaries/ 2.8M
.kiro/             1.5M
```

#### 6.2 建議
```
docs/
├── architecture/        # 架構文檔
├── development/         # 開發指南
├── api/                 # API 文檔
├── deployment/          # 部署文檔
├── reports/             # 合併 reports-summaries
│   ├── architecture/
│   ├── quality/
│   └── tasks/
└── diagrams/            # 圖表
```

**理由**:
- 統一文檔位置
- reports-summaries 應該是 docs 的一部分
- 更清晰的組織結構

---

## 📋 優先級建議

### 🔴 高優先級（立即執行）

1. **刪除系統和臨時檔案**
   ```bash
   rm .DS_Store
   rm excalidraw.log
   ```

2. **更新 .gitignore**
   ```gitignore
   # macOS
   .DS_Store

   # Logs
   *.log
   logs/
   !logs/.gitkeep
   ```

3. **移動 suppress-unused-warnings.txt**
   ```bash
   mkdir -p docs/development
   mv suppress-unused-warnings.txt docs/development/code-quality-tracking.txt
   ```

### 🟡 中優先級（本週執行）

4. **整理 Docker Compose 檔案**
   - 選擇方案：保留主要的在根目錄，其他移到 deployment/docker/

5. **統一日誌目錄**
   - 建立 logs/ 子目錄結構
   - 更新相關腳本和配置

6. **釐清 staging-tests/ 用途**
   - 檢查內容
   - 決定保留、移動或刪除

### 🟢 低優先級（有時間再做）

7. **重組 config/ 目錄**
   - 如果未來有更多配置檔案再考慮

8. **合併 reports-summaries 到 docs/**
   - 需要更新相關連結和引用

9. **整合 tools/ 目錄**
   - 決定是否合併到 scripts/

---

## 🎯 建議的最終結構

```
.
├── .github/                 # GitHub 配置
├── .kiro/                   # Kiro IDE 配置
├── app/                     # 主應用程式
├── cmc-frontend/            # CMC 前端
├── consumer-frontend/       # 消費者前端
├── deployment/              # 部署相關
│   ├── docker/             # Docker Compose 檔案
│   ├── k8s/                # Kubernetes 配置
│   └── scripts/            # 部署腳本
├── docs/                    # 所有文檔
│   ├── architecture/
│   ├── development/
│   ├── reports/            # 合併 reports-summaries
│   └── diagrams/
├── infrastructure/          # AWS CDK
├── logs/                    # 統一日誌（.gitignore）
│   ├── .gitkeep
│   ├── mcp/
│   └── app/
├── scripts/                 # 開發腳本
│   └── tools/              # 合併 tools/
├── [配置檔案]              # 必要的根目錄配置
├── docker-compose.yml       # 主要 Docker Compose
├── Dockerfile
├── README.md
└── ...
```

---

## ✅ 執行檢查清單

### 階段 1: 清理（立即）
- [ ] 刪除 .DS_Store
- [ ] 刪除 excalidraw.log
- [ ] 移動 suppress-unused-warnings.txt
- [ ] 更新 .gitignore

### 階段 2: 重組（本週）
- [ ] 整理 Docker Compose 檔案
- [ ] 建立 logs/ 子目錄結構
- [ ] 檢查 staging-tests/ 內容
- [ ] 決定 staging-tests/ 去留

### 階段 3: 優化（有時間）
- [ ] 重組 config/ 目錄
- [ ] 考慮合併 reports-summaries
- [ ] 整合 tools/ 目錄
- [ ] 更新相關文檔

---

## 📝 注意事項

1. **備份**: 執行任何移動或刪除前先備份
2. **測試**: 移動檔案後測試構建和部署
3. **文檔**: 更新 README.md 反映新結構
4. **團隊**: 通知團隊成員結構變更
5. **CI/CD**: 更新 CI/CD 配置中的路徑

---

## 🔗 相關文檔

- [Development Standards](.kiro/steering/development-standards.md)
- [Project Structure](docs/architecture/project-structure.md)
- [Deployment Guide](deployment/README.md)

---

**分析完成日期**: 2025-11-23
**建議執行期限**: 2025-11-30
**負責人**: Development Team
