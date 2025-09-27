# Infrastructure 目錄清理建議報告

**分析時間**: 2025年9月24日 下午4:50 (台北時間)  
**分析範圍**: infrastructure/ 目錄完整結構  
**目的**: 識別不必要的檔案和改善 .gitignore 配置

## 🗑️ 建議刪除的檔案和目錄

### 1. 自動生成的檔案 (應該刪除)

#### Build 輸出目錄
```bash
# 這些目錄包含編譯後的檔案，應該被 .gitignore 忽略
infrastructure/dist/                    # TypeScript 編譯輸出
infrastructure/coverage/                # 測試覆蓋率報告
infrastructure/test-results/            # 測試結果
```

#### CDK 輸出
```bash
# CDK 合成輸出，每次部署時重新生成
infrastructure/cdk.out/                 # CDK 合成輸出 (已在 .gitignore)
```

### 2. 依賴管理檔案 (已正確忽略)
```bash
# 這些已經在 .gitignore 中，確認正確
infrastructure/node_modules/           # NPM 依賴 ✅
infrastructure/package-lock.json       # 應該保留 ✅
```

### 3. 可能重複或過時的檔案

#### 成本和配置檔案 (需要檢查)
```bash
# 檢查這些檔案是否還需要
infrastructure/cost-estimation-report.json     # 可能過時
infrastructure/budget-configuration.json       # 檢查是否使用中
```

## 📝 .gitignore 改善建議

### 當前 .gitignore 分析
✅ **良好的配置**:
- `node_modules/` - 正確忽略依賴
- `cdk.out/` - 正確忽略 CDK 輸出
- `dist/` - 正確忽略編譯輸出
- `coverage/` - 正確忽略測試覆蓋率

### 建議添加的項目

```gitignore
# 在現有 .gitignore 基礎上添加以下項目

# CDK 相關 (補充)
.cdk.staging/
*.asset.json
*.assets.json

# 測試相關 (補充)
test-results/
junit.xml
*.junit.xml
.jest-cache/
allure-results/
allure-report/

# 性能和分析報告
performance-report.json
cost-estimation-report.json
architecture-assessment-summary.json
well-architected-summary.json

# IDE 和編輯器 (補充)
.vscode/settings.json
.vscode/launch.json
*.code-workspace

# 臨時檔案 (補充)
*.tmp
*.temp
.DS_Store?
ehthumbs.db
Icon?

# AWS 相關 (補充)
.aws-sam/
samconfig.toml
.aws-credentials

# Docker (如果使用)
.dockerignore
docker-compose.override.yml

# 本地配置檔案
local.config.json
*.local.json
```

## 🧹 清理腳本建議

### 立即清理腳本
```bash
#!/bin/bash
# infrastructure/scripts/cleanup-generated-files.sh

echo "🧹 清理 Infrastructure 目錄中的生成檔案..."

# 刪除編譯輸出
rm -rf infrastructure/dist/
rm -rf infrastructure/coverage/
rm -rf infrastructure/test-results/

# 刪除 CDK 輸出 (如果存在)
rm -rf infrastructure/cdk.out/

# 刪除快取檔案
rm -rf infrastructure/.jest-cache/
rm -f infrastructure/.eslintcache
rm -f infrastructure/tsconfig.tsbuildinfo

# 清理日誌檔案
find infrastructure/ -name "*.log" -type f -delete
find infrastructure/ -name "npm-debug.log*" -type f -delete

echo "✅ 清理完成！"
```

### 定期清理腳本
```bash
#!/bin/bash
# infrastructure/scripts/deep-cleanup.sh

echo "🔍 深度清理 Infrastructure 目錄..."

# 清理 node_modules (重新安裝)
rm -rf infrastructure/node_modules/
rm -f infrastructure/package-lock.json

# 重新安裝依賴
cd infrastructure/
npm install

# 重新編譯
npm run build

echo "✅ 深度清理和重建完成！"
```

## 📊 檔案大小分析

### 大型目錄 (需要關注)
```bash
# 使用 du 命令檢查目錄大小
du -sh infrastructure/node_modules/     # ~500MB+ (正常，但不應提交)
du -sh infrastructure/coverage/         # ~10-50MB (應該忽略)
du -sh infrastructure/dist/             # ~5-20MB (應該忽略)
du -sh infrastructure/cdk.out/          # ~1-10MB (應該忽略)
```

### 建議的大小限制
- **單個檔案**: < 1MB (除了必要的二進制檔案)
- **文檔檔案**: < 100KB
- **配置檔案**: < 10KB

## 🔍 檔案類型分析

### 應該保留的檔案類型
```bash
✅ 源碼檔案:
- *.ts (TypeScript 源碼)
- *.js (JavaScript 配置檔案)
- *.json (配置檔案)
- *.md (文檔)
- *.yml, *.yaml (配置檔案)

✅ 配置檔案:
- package.json
- tsconfig.json
- jest.config.js
- .eslintrc.*
- cdk.json
```

### 應該忽略的檔案類型
```bash
❌ 編譯輸出:
- *.d.ts (TypeScript 定義檔案)
- *.js.map (Source map)
- *.tsbuildinfo

❌ 測試輸出:
- *.lcov
- junit.xml
- coverage/

❌ 臨時檔案:
- *.tmp
- *.log
- .DS_Store
```

## 🎯 具體清理建議

### 立即執行 (安全)
```bash
# 1. 刪除編譯輸出
rm -rf infrastructure/dist/
rm -rf infrastructure/coverage/
rm -rf infrastructure/test-results/

# 2. 清理快取
rm -rf infrastructure/.jest-cache/
rm -f infrastructure/.eslintcache
rm -f infrastructure/tsconfig.tsbuildinfo

# 3. 清理日誌
find infrastructure/ -name "*.log" -delete
```

### 需要確認後執行
```bash
# 檢查這些檔案是否還需要
ls -la infrastructure/cost-estimation-report.json
ls -la infrastructure/budget-configuration.json

# 如果不需要，可以刪除
# rm infrastructure/cost-estimation-report.json
# rm infrastructure/budget-configuration.json
```

### 更新 .gitignore
```bash
# 將建議的項目添加到 infrastructure/.gitignore
cat >> infrastructure/.gitignore << 'EOF'

# 測試相關 (補充)
test-results/
*.junit.xml
allure-results/
allure-report/

# 性能和分析報告
performance-report.json
cost-estimation-report.json
architecture-assessment-summary.json
well-architected-summary.json

# CDK 相關 (補充)
*.asset.json
*.assets.json

# AWS 相關 (補充)
.aws-sam/
samconfig.toml
EOF
```

## 🔄 維護建議

### 定期清理 (建議每週)
```bash
# 添加到 package.json scripts
{
  "scripts": {
    "clean": "rm -rf dist coverage test-results .jest-cache",
    "clean:deep": "npm run clean && rm -rf node_modules && npm install",
    "clean:cdk": "rm -rf cdk.out .cdk.staging"
  }
}
```

### Git Hooks 建議
```bash
# .git/hooks/pre-commit
#!/bin/bash
# 確保不會提交生成的檔案
if git diff --cached --name-only | grep -E "(dist/|coverage/|test-results/|\.log$)"; then
    echo "❌ 錯誤: 嘗試提交生成的檔案"
    echo "請執行 npm run clean 清理後重新提交"
    exit 1
fi
```

## 📋 檢查清單

### 清理前檢查
- [ ] 確認 `dist/` 目錄可以重新生成
- [ ] 確認 `coverage/` 目錄可以重新生成
- [ ] 確認 `test-results/` 目錄可以重新生成
- [ ] 備份重要的配置檔案

### 清理後驗證
- [ ] 執行 `npm run build` 確認編譯正常
- [ ] 執行 `npm test` 確認測試正常
- [ ] 執行 `cdk synth` 確認 CDK 合成正常
- [ ] 檢查 Git 狀態確認沒有意外刪除重要檔案

## 💾 磁碟空間節省估算

### 預期節省空間
```bash
coverage/           ~20MB
dist/              ~10MB
test-results/      ~5MB
.jest-cache/       ~15MB
*.log files        ~2MB
----------------------------
總計節省:          ~52MB
```

### 長期維護效益
- **Git 倉庫大小**: 減少不必要的檔案追蹤
- **CI/CD 效能**: 減少需要處理的檔案數量
- **開發體驗**: 更清潔的工作目錄
- **部署速度**: 減少需要傳輸的檔案

## 🚨 注意事項

### 不要刪除的檔案
```bash
✅ 保留這些重要檔案:
- package.json
- package-lock.json
- tsconfig.json
- cdk.json
- jest.config.js
- deploy.config.ts
- 所有 .ts 源碼檔案
- 所有 .md 文檔檔案
```

### 謹慎處理的檔案
```bash
⚠️ 需要確認的檔案:
- cost-estimation-report.json (可能包含重要成本資訊)
- budget-configuration.json (可能是預算配置)
- 任何 .local.* 檔案 (可能包含本地配置)
```

---

**建議執行順序**:
1. 先更新 `.gitignore`
2. 執行安全的清理操作
3. 測試編譯和部署
4. 提交 `.gitignore` 更新
5. 設定定期清理腳本
