# GenAI Demo Frontend

這是 GenAI Demo 專案的前端應用程式，使用 Next.js 14 + React 18 + TypeScript 構建，與後端 Spring Boot 應用程式完美整合。

## 🚀 技術棧

- **框架**: Next.js 14 (App Router)
- **UI 庫**: React 18 + TypeScript
- **樣式**: Tailwind CSS + shadcn/ui
- **狀態管理**: Zustand + React Query (TanStack Query)
- **API 客戶端**: Axios
- **表單處理**: React Hook Form + Zod
- **圖標**: Lucide React
- **通知**: React Hot Toast

## 📁 專案結構

```
frontend/
├── src/
│   ├── app/                    # Next.js App Router 頁面
│   │   ├── layout.tsx         # 根佈局
│   │   ├── page.tsx           # 首頁
│   │   ├── providers.tsx      # 全域 Providers
│   │   └── orders/            # 訂單相關頁面
│   ├── components/            # React 組件
│   │   ├── ui/               # 基礎 UI 組件
│   │   └── order/            # 訂單相關組件
│   ├── hooks/                # 自定義 Hooks
│   │   └── useApi.ts         # API 相關 Hooks
│   ├── lib/                  # 工具函數
│   │   ├── store.ts          # Zustand 狀態管理
│   │   └── utils.ts          # 通用工具函數
│   ├── services/             # API 服務層
│   │   └── api.ts            # API 客戶端
│   └── types/                # TypeScript 類型定義
│       └── domain.ts         # 領域模型類型
├── package.json
├── next.config.js
├── tailwind.config.js
└── tsconfig.json
```

## 🛠️ 安裝和運行

### 前置條件

- Node.js 18+ 
- npm 或 yarn
- 後端 Spring Boot 應用程式運行在 `http://localhost:8080`

### 安裝依賴

```bash
cd frontend
npm install
```

### 環境變數設置

創建 `.env.local` 文件：

```bash
# API 後端地址
NEXT_PUBLIC_API_URL=http://localhost:8080/api

# 開發模式設置
NODE_ENV=development
```

### 運行開發服務器

```bash
npm run dev
```

應用程式將在 `http://localhost:3000` 啟動。

### 其他指令

```bash
# 構建生產版本
npm run build

# 運行生產版本
npm run start

# 代碼檢查
npm run lint

# 類型檢查
npm run type-check

# 運行測試
npm run test

# E2E 測試
npm run test:e2e
```

## 🎨 功能特色

### 1. 響應式設計
- 支援桌面、平板和手機設備
- 使用 Tailwind CSS 實現響應式佈局
- 優化的移動端用戶體驗

### 2. 現代化 UI
- 基於 shadcn/ui 的設計系統
- 一致的視覺風格和交互體驗
- 支援深色/淺色主題切換

### 3. 高效的狀態管理
- 使用 Zustand 管理全域狀態
- React Query 處理服務器狀態和緩存
- 自動錯誤處理和重試機制

### 4. 類型安全
- 完整的 TypeScript 支援
- 與後端 API 對應的類型定義
- 編譯時錯誤檢查

## 📱 主要頁面和功能

### 首頁 (`/`)
- 系統概覽和快速導航
- 實時統計數據展示
- 最近活動時間線

### 訂單管理 (`/orders`)
- 訂單列表展示和篩選
- 訂單狀態管理
- 訂單詳情查看和編輯

### 商品管理 (`/products`)
- 商品列表和搜尋
- 庫存狀態監控
- 商品資訊管理

### 客戶管理 (`/customers`)
- 客戶資料管理
- 購買記錄追蹤
- 會員等級管理

### 支付管理 (`/payments`)
- 支付記錄查詢
- 支付狀態追蹤
- 退款處理

### 促銷管理 (`/promotions`)
- 促銷活動創建和管理
- 優惠券系統
- 活動效果分析

## 🔧 開發指南

### 添加新頁面

1. 在 `src/app` 目錄下創建新的路由文件夾
2. 添加 `page.tsx` 文件
3. 實現頁面組件

```typescript
// src/app/new-feature/page.tsx
export default function NewFeaturePage() {
  return (
    <div>
      <h1>新功能頁面</h1>
    </div>
  )
}
```

### 添加新組件

1. 在 `src/components` 目錄下創建組件文件
2. 使用 TypeScript 定義 Props 接口
3. 導出組件

```typescript
// src/components/MyComponent.tsx
interface MyComponentProps {
  title: string
  onAction: () => void
}

export const MyComponent: React.FC<MyComponentProps> = ({ title, onAction }) => {
  return (
    <div>
      <h2>{title}</h2>
      <button onClick={onAction}>執行動作</button>
    </div>
  )
}
```

### 添加 API 服務

1. 在 `src/services/api.ts` 中添加新的 API 方法
2. 在 `src/hooks/useApi.ts` 中創建對應的 React Query hooks
3. 在組件中使用 hooks

```typescript
// 在 api.ts 中添加
export const newFeatureService = {
  list: () => apiClient.request<NewFeature[]>('GET', '/new-features'),
  create: (data: CreateNewFeatureRequest) => 
    apiClient.request<NewFeature>('POST', '/new-features', data),
}

// 在 useApi.ts 中添加
export const useNewFeatures = () => {
  return useQuery({
    queryKey: ['newFeatures'],
    queryFn: () => newFeatureService.list(),
  })
}
```

## 🧪 測試

### 單元測試
使用 Jest + React Testing Library：

```bash
npm run test
```

### E2E 測試
使用 Playwright：

```bash
npm run test:e2e
```

## 📦 部署

### 構建生產版本

```bash
npm run build
```

### Docker 部署

```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build
EXPOSE 3000
CMD ["npm", "start"]
```

### Vercel 部署

1. 連接 GitHub 倉庫到 Vercel
2. 設置環境變數
3. 自動部署

## 🔗 與後端整合

### API 代理設置

在 `next.config.js` 中配置 API 代理：

```javascript
async rewrites() {
  return [
    {
      source: '/api/:path*',
      destination: 'http://localhost:8080/api/:path*',
    },
  ]
}
```

### 錯誤處理

- 自動重試機制
- 用戶友好的錯誤提示
- 網路錯誤恢復

### 數據同步

- 實時數據更新
- 樂觀更新策略
- 衝突解決機制

## 🎯 最佳實踐

1. **組件設計**: 遵循單一職責原則，保持組件簡潔
2. **狀態管理**: 合理使用本地狀態和全域狀態
3. **性能優化**: 使用 React.memo、useMemo 等優化渲染
4. **錯誤邊界**: 實現錯誤邊界組件處理異常
5. **無障礙性**: 遵循 WCAG 指南，提供良好的無障礙體驗

## 🤝 貢獻指南

1. Fork 專案
2. 創建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 開啟 Pull Request

## 📄 授權

本專案採用 MIT 授權協議 - 詳見 [LICENSE](../LICENSE) 文件。
