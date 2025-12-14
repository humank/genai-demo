# Implementation Plan - Java Code Quality Phase 2

> **方法**: 使用 IDE 內建功能進行代碼質量改進
> **工具**: Kiro IDE / IntelliJ IDEA 診斷和快速修復

- [x] 1. 移除未使用的私有字段 (~20 issues)
  - 使用 IDE 診斷識別未使用的字段
  - 檢查每個字段是否被框架使用 (@Autowired, @Value, @Column, @JsonProperty)
  - 對確認未使用的字段使用 IDE 快速修復移除
  - 對框架使用的字段添加 @SuppressWarnings("unused") 並加註釋
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 2. 移除未使用的私有方法 (~29 issues)
  - 使用 IDE 診斷識別未使用的方法
  - 檢查每個方法是否被框架使用 (@Scheduled, @EventListener, @Async)
  - 對確認未使用的方法使用 IDE 快速修復移除
  - 對框架使用的方法保留並加文檔說明
  - _Requirements: 2.1, 2.2, 2.3_

- [x] 3. Checkpoint - 驗證編譯和測試
  - 運行 `./gradlew :app:compileJava` 確認零警告
  - 運行 `./gradlew :app:test` 確認測試通過
  - _Requirements: 5.1, 5.2_

- [x] 4. 添加 @NonNull 註解 (~30 issues)
  - 使用 IDE 診斷識別需要 @NonNull 的參數
  - 使用 IDE 快速修復添加 org.springframework.lang.NonNull 註解
  - 確認導入語句正確
  - _Requirements: 3.1, 3.2, 3.3_

- [x] 5. 處理 TODO 註釋 (~4 issues)
  - 使用 IDE 搜索所有 TODO 註釋
  - 對簡單修復直接實現
  - 對複雜功能創建 GitHub issue 並更新註釋
  - 移除過時的 TODO 註釋
  - _Requirements: 4.1, 4.2, 4.3, 4.4_
  - **結果**: 搜索整個代碼庫後發現沒有 TODO、FIXME、XXX 或 HACK 註釋需要處理。任務完成。

- [x] 6. Final Checkpoint - 驗證所有改進
  - 運行 `./gradlew :app:compileJava` 確認零警告
  - 運行 `./gradlew :app:test` 確認所有測試通過
  - 運行 `./gradlew :app:build` 確認構建成功
  - 記錄修復的問題數量
  - _Requirements: 5.1, 5.2, 5.3, 5.5_
  - **最終結果**:
    - ✅ 編譯: 零警告 (BUILD SUCCESSFUL)
    - ✅ 構建: 成功 (BUILD SUCCESSFUL)
    - ✅ 測試: 全部通過 (BUILD SUCCESSFUL)
      - 修復了 8 個失敗的測試 (ProfileConfigurationIntegrationTest, TestProfileBeanConfigurationTest)
      - 問題原因: ProfileConfigurationProperties 同時有 @ConfigurationProperties 註解和 @Bean 方法定義，導致 NoUniqueBeanDefinitionException
      - 解決方案: 移除 @ConfigurationProperties 註解，因為 beans 是在 ProfileConfigurationResolver 中手動創建的
    - **Phase 2 完成摘要**:
      - Task 1: 移除未使用的私有字段 - 已完成
      - Task 2: 移除未使用的私有方法 - 已完成
      - Task 3: Checkpoint 驗證 - 已完成
      - Task 4: @NonNull 註解 - 已完成 (30 個註解已存在)
      - Task 5: TODO 註釋 - 已完成 (無 TODO 註釋需要處理)
      - Task 6: Final Checkpoint - 已完成
      - 額外修復: 8 個測試失敗問題 - 已修復

