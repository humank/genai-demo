
# Guidelines

This project已配置為統一使用 **Excalifont** 字型來創建所有 Excalidraw 圖表，確保視覺一致性和專業外觀。

## 配置文件

### 主配置文件
- **位置**: `.kiro/settings/excalidraw.json`
- **用途**: 定義專案級的 Excalidraw 預設設定

### 配置內容

```json
{
  "defaultSettings": {
    "fontFamily": "Excalifont",
    "fontSize": 14,
    "strokeColor": "#1e1e1e",
    "backgroundColor": "transparent",
    "strokeWidth": 2,
    "roughness": 1,
    "opacity": 100
  },
  "elementDefaults": {
    "text": {"fontFamily": "Excalifont", "fontSize": 14},
    "label": {"fontFamily": "Excalifont", "fontSize": 14},
    "title": {"fontFamily": "Excalifont", "fontSize": 24},
    "subtitle": {"fontFamily": "Excalifont", "fontSize": 18},
    "caption": {"fontFamily": "Excalifont", "fontSize": 12}
  }
}
```

## Tools

### 1. 配置管理器
**腳本**: `scripts/excalidraw-config-manager.py`

```bash
# 更新所有字型為 Excalifont
python scripts/excalidraw-config-manager.py --update-fonts

# 顯示當前配置
python scripts/excalidraw-config-manager.py --show-config
```

### 2. 輔助函數模組
**模組**: `scripts/excalidraw_helpers.py`

提供預配置的函數來創建 Excalidraw 元素，自動應用 Excalifont 字型。

## 使用方法

### 在 Python 腳本中使用

```python
import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent))

from excalidraw_helpers import (
    create_text, create_rectangle, create_ellipse,
    create_bounded_context, create_stakeholder, create_title
)

# 創建標題（自動使用 Excalifont）
title = create_title("系統架構概覽")

# 創建Bounded Context（自動使用 Excalifont）
customer_context = create_bounded_context("Customer", 100, 100, "business")

# 創建Stakeholder（自動使用 Excalifont）
end_user = create_stakeholder("End Users", 200, 200, "user")

# 創建文字元素（自動使用 Excalifont）
description = create_text("系統說明文字", 50, 300, element_type="caption")
```

### Best Practices

當直接使用 Excalidraw MCP 時，請確保所有文字元素都指定 `fontFamily: "Excalifont"`：

```python
# ✅ 正確：使用 Excalifont
mcp_excalidraw_create_element({
    "type": "text",
    "x": 100,
    "y": 100,
    "text": "範例文字",
    "fontFamily": "Excalifont",  # 重要：指定字型
    "fontSize": 14
})

# ❌ 錯誤：使用預設字型
mcp_excalidraw_create_element({
    "type": "text",
    "x": 100,
    "y": 100,
    "text": "範例文字",
    "fontSize": 14
    # 缺少 fontFamily 設定
})
```

## 顏色方案

專案定義了四種標準顏色方案：

### 1. Business（業務）
- **主色**: `#1976d2` (藍色)
- **次色**: `#e3f2fd` (淺藍色)
- **強調色**: `#0d47a1` (深藍色)

### 2. Technical（技術）
- **主色**: `#388e3c` (綠色)
- **次色**: `#e8f5e8` (淺綠色)
- **強調色**: `#1b5e20` (深綠色)

### 3. User（User）
- **主色**: `#f57c00` (橙色)
- **次色**: `#fff3e0` (淺橙色)
- **強調色**: `#e65100` (深橙色)

### 4. Process（流程）
- **主色**: `#7b1fa2` (紫色)
- **次色**: `#f3e5f5` (淺紫色)
- **強調色**: `#4a148c` (深紫色)

## Templates

### Bounded Context (Bounded Context)
- **類型**: 矩形
- **尺寸**: 150x80
- **字型**: Excalifont, 14px

### Stakeholders (Stakeholder)
- **類型**: 橢圓
- **尺寸**: 120x80
- **字型**: Excalifont, 14px

### 流程步驟 (Process Step)
- **類型**: 矩形
- **尺寸**: 120x60
- **字型**: Excalifont, 14px

## Guidelines

- **標題 (Title)**: 24px
- **副標題 (Subtitle)**: 18px
- **一般文字 (Text/Label)**: 14px
- **說明文字 (Caption)**: 12px

## Maintenance

### 更新現有圖表
如果需要將現有的 Excalidraw 圖表更新為使用 Excalifont：

1. 執行配置管理器：
   ```bash
   python scripts/excalidraw-config-manager.py --update-fonts
   ```

2. 手動檢查和更新現有圖表中的字型設定

### 新增顏色方案
要新增新的顏色方案，編輯 `.kiro/settings/excalidraw.json`：

```json
{
  "colorSchemes": {
    "newScheme": {
      "primary": "#color1",
      "secondary": "#color2", 
      "accent": "#color3"
    }
  }
}
```

### Templates
要新增新的元素模板：

```json
{
  "templates": {
    "newTemplate": {
      "type": "rectangle",
      "width": 100,
      "height": 50,
      "strokeWidth": 2,
      "fontFamily": "Excalifont",
      "fontSize": 14
    }
  }
}
```

## Best Practices

1. **一致性**: 所有圖表都使用 Excalifont 字型
2. **顏色標準化**: 使用預定義的顏色方案
3. **尺寸標準化**: 使用預定義的元素模板
4. **可讀性**: 確保文字大小適當（最小 12px）
5. **對比度**: 確保文字和背景有足夠對比度

## Troubleshooting

### 字型未正確顯示
1. 確認 Excalifont 字型已安裝
2. 檢查元素配置中是否正確設定 `fontFamily: "Excalifont"`
3. 使用配置管理器更新設定

### 配置未生效
1. 檢查 `.kiro/settings/excalidraw.json` 文件是否存在
2. 驗證 JSON 格式是否正確
3. 重新執行配置管理器

---

*此配置確保所有 Excalidraw 圖表在專案中保持一致的視覺風格和專業外觀。*