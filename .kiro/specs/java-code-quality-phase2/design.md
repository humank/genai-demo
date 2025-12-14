# Design Document: Java Code Quality Phase 2

## Overview

使用 IDE 內建功能完成 Phase 1 之後剩餘的代碼質量改進。

**目標問題**: ~83 個剩餘問題
- Medium Priority: ~49 issues (未使用的字段和方法)
- Low Priority: ~34 issues (@NonNull 註解, TODO 註釋)

## 方法

### 使用 IDE 功能

1. **診斷面板** - 識別代碼問題
2. **快速修復** - 自動修復問題
3. **搜索功能** - 查找 TODO 註釋
4. **重構工具** - 安全移除未使用的代碼

### 框架註解檢查清單

移除代碼前需檢查以下框架註解：

**Spring Framework**:
- `@Autowired`, `@Value`, `@Qualifier`
- `@Bean`, `@Component`, `@Service`
- `@Scheduled`, `@EventListener`, `@Async`

**JPA/Hibernate**:
- `@Column`, `@Id`, `@Transient`
- `@Entity`, `@Table`

**Jackson**:
- `@JsonProperty`, `@JsonIgnore`
- `@JsonInclude`, `@JsonFormat`

## 驗證步驟

每次修改後執行：

```bash
# 1. 編譯檢查
./gradlew :app:compileJava

# 2. 測試檢查
./gradlew :app:test

# 3. 完整構建
./gradlew :app:build
```

## 成功標準

- ✅ 零編譯警告
- ✅ 所有測試通過
- ✅ 構建時間在基準線 20% 以內
- ✅ 所有未使用代碼已審查和處理

