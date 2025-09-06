# 文檔翻譯 Agent

這是一個專門用於翻譯專案文檔的 Kiro Agent。

## 使用方式

當你想要翻譯文檔時，可以：

1. **手動觸發**: 在聊天中輸入 `#translate` 或提及翻譯需求
2. **Commit 觸發**: 在 commit message 中包含 `[translate]` 或 `[en]`

## 翻譯流程

Agent 會自動執行以下步驟：

1. 檢測需要翻譯的 .md 檔案
2. 建立對應的英文目錄結構
3. 翻譯內容並轉換內部連結
4. 將翻譯結果加入 git

## 範例

假設你修改了 `docs/releases/new-feature.md`，使用以下命令觸發翻譯：

```bash
git add docs/releases/new-feature.md
git commit -m "新增功能說明 [translate]"
```

Agent 會自動：
- 翻譯 `docs/releases/new-feature.md`
- 建立 `docs/en/releases/new-feature.md`
- 轉換內部連結指向英文版本
- 將英文版本加入同一個 commit
