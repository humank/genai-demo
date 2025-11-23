# Code Quality Fix Scripts

這些腳本用於自動修復常見的 SonarLint 和程式碼品質問題。

## 📋 可用腳本

### 1. 主要腳本

#### `fix-all-issues.sh` - 一鍵修復所有問題
執行所有修復腳本的主要入口點。

```bash
./scripts/fix-all-issues.sh
```

**功能**:
- 自動備份原始碼
- 執行所有修復腳本
- 格式化程式碼
- 執行品質檢查
- 生成報告

---

### 2. 個別修復腳本

#### `fix-lambda-braces.py` - 修復 Lambda 大括號 (S1602)
移除 lambda 表達式中不必要的大括號。

```bash
python3 scripts/fix-lambda-braces.py
```

**修復範例**:
```java
// 修復前
return registry -> {
    registry.config().meterFilter(...);
};

// 修復後
return registry -> registry.config().meterFilter(...);
```

---

#### `fix-string-constants.py` - 提取重複字串常量 (S1192)
自動提取重複的字串字面值為常量。

```bash
python3 scripts/fix-string-constants.py
```

**修復範例**:
```java
// 修復前
subsegment.putAnnotation("operation", op1);
subsegment.putAnnotation("operation", op2);
subsegment.putAnnotation("operation", op3);

// 修復後
private static final String METADATA_OPERATION = "operation";

subsegment.putAnnotation(METADATA_OPERATION, op1);
subsegment.putAnnotation(METADATA_OPERATION, op2);
subsegment.putAnnotation(METADATA_OPERATION, op3);
```

---

#### `fix-null-safety.py` - 添加 Null Safety 導入
為檔案添加 `@NonNull` 和 `@Nullable` 的導入語句。

```bash
python3 scripts/fix-null-safety.py
```

**注意**: 此腳本只添加導入，實際的註解需要使用 IDE 的快速修復功能手動添加。

---

#### `report-unused-code.py` - 報告未使用的程式碼
生成未使用欄位和變數的報告。

```bash
python3 scripts/report-unused-code.py
```

**輸出範例**:
```
🔴 Unused Private Fields (5):
📁 app/src/main/java/Example.java
   - Logger logger
   - String unusedField

🟡 Potentially Unused Variables (3):
📁 app/src/main/java/Example.java
   - String temp = "value"
```

---

#### `fix-sonar-issues.sh` - 修復其他 SonarLint 問題
處理其他常見的 SonarLint 問題。

```bash
./scripts/fix-sonar-issues.sh
```

---

## 🚀 使用流程

### 快速開始

1. **執行主腳本**:
   ```bash
   chmod +x scripts/*.sh
   ./scripts/fix-all-issues.sh
   ```

2. **檢查備份**:
   備份會自動建立在 `backup-YYYYMMDD-HHMMSS/` 目錄

3. **審查變更**:
   在 IDE 中檢查所有變更

4. **手動修復**:
   使用 IDE 的快速修復功能處理剩餘問題

5. **執行測試**:
   ```bash
   ./gradlew test
   ```

6. **提交變更**:
   ```bash
   git add .
   git commit -m "fix: resolve SonarLint issues"
   ```

---

### 逐步執行

如果你想更細緻地控制修復過程：

```bash
# 1. 修復 lambda 大括號
python3 scripts/fix-lambda-braces.py

# 2. 提取字串常量
python3 scripts/fix-string-constants.py

# 3. 添加 null safety 導入
python3 scripts/fix-null-safety.py

# 4. 生成未使用程式碼報告
python3 scripts/report-unused-code.py

# 5. 格式化程式碼
./gradlew spotlessApply

# 6. 執行檢查
./gradlew check
```

---

## 🔧 IDE 快速修復

某些問題需要使用 IDE 的快速修復功能：

### IntelliJ IDEA / Kiro IDE

1. **顯示快速修復**: `Alt + Enter`
2. **組織導入**: `Ctrl + Alt + O`
3. **格式化程式碼**: `Ctrl + Alt + L`
4. **安全刪除**: `Alt + Delete`

### 常見快速修復

| 問題 | 快速修復 |
|------|----------|
| Null safety warnings | Add @NonNull annotation |
| Unused variables | Remove unused variable |
| Unused fields | Safe delete |
| Missing @Override | Add @Override annotation |
| Deprecated API | Replace with new API |

---

## 📊 問題類型對照表

| SonarLint 規則 | 描述 | 腳本 | 狀態 |
|---------------|------|------|------|
| S1192 | 重複字串字面值 | `fix-string-constants.py` | ✅ 自動 |
| S1602 | Lambda 不必要的大括號 | `fix-lambda-braces.py` | ✅ 自動 |
| S1068 | 未使用的私有欄位 | `report-unused-code.py` | 📋 報告 |
| S1481 | 未使用的局部變數 | `report-unused-code.py` | 📋 報告 |
| S1854 | 無用的賦值 | `report-unused-code.py` | 📋 報告 |
| S125 | 註解掉的程式碼 | - | 🔧 手動 |
| S1126 | 簡化 if-then-else | - | 🔧 手動 |
| S2925 | Thread.sleep() | - | 🔧 手動 |
| Null Safety | Null 安全警告 | `fix-null-safety.py` | ⚠️ 半自動 |

---

## ⚠️ 注意事項

### 備份
- 腳本會自動建立備份
- 備份位置: `backup-YYYYMMDD-HHMMSS/`
- 建議在執行前先提交到 Git

### 審查變更
- **務必審查所有自動變更**
- 某些修復可能不適用於特定情況
- 使用 `git diff` 檢查變更

### 測試
- 修復後務必執行測試
- 確保沒有破壞現有功能
- 檢查 Gradle 構建是否成功

### 限制
- 腳本使用正則表達式，不是完整的 AST 解析
- 某些複雜情況可能無法正確處理
- 建議在小範圍測試後再大規模應用

---

## 🐛 故障排除

### Python 腳本無法執行
```bash
# 確保 Python 3 已安裝
python3 --version

# 賦予執行權限
chmod +x scripts/*.py
```

### Gradle 命令失敗
```bash
# 清理並重新構建
./gradlew clean build

# 檢查 Java 版本
java -version  # 應該是 Java 21
```

### 腳本修改了不該修改的內容
```bash
# 從備份恢復
cp -r backup-YYYYMMDD-HHMMSS/src app/

# 或使用 Git 恢復
git checkout -- app/src
```

---

## 📝 貢獻

如果你發現腳本的問題或有改進建議：

1. 在專案中建立 Issue
2. 描述問題和預期行為
3. 提供範例程式碼
4. 提交 Pull Request

---

## 📚 相關文件

- [Development Standards](../.kiro/steering/development-standards.md)
- [Code Quality Checklist](../.kiro/steering/code-quality-checklist.md)
- [IDE Configuration Standards](../.kiro/steering/ide-configuration-standards.md)

---

**最後更新**: 2025-11-22
**維護者**: Development Team
