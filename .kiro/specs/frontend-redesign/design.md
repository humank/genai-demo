# 設計文件

## 簡介

本文件定義企業級電商平台前端全面重新設計的技術設計方案。基於需求文件中的 20 項需求，本設計涵蓋 Monorepo 架構、共享套件、兩個前端應用程式的元件設計、頁面結構、狀態管理、API 整合與部署策略。

**最後更新**：2026-02-21

---

## 1. Monorepo 架構設計

### 1.1 目錄結構

```
frontend/
├── turbo.json                    # Turborepo 管線設定
├── pnpm-workspace.yaml           # pnpm workspace 定義
├── package.json                  # 根層級腳本與共享 devDependencies
├── .env.example                  # 環境變數範本
├── Dockerfile.cmc                # CMC 建構用 Dockerfile
├── Dockerfile.consumer           # Consumer 建構用 Dockerfile
│
├── apps/
│   ├── cmc/                      # CMC 管理後台（Next.js 15）
│   │   ├── package.json          # name: "@repo/cmc"
│   │   ├── next.config.ts
│   │   ├── tailwind.config.ts    # 繼承 @repo/config + CMC 主題覆寫
│   │   ├── tsconfig.json         # 繼承 @repo/config
│   │   └── src/
│   │       ├── app/              # Next.js App Router 路由
│   │       ├── components/       # CMC 專屬元件
│   │       ├── lib/              # CMC 工具函式
│   │       └── styles/           # CMC 全域樣式與主題 CSS 變數
│   │
│   └── consumer/                 # 消費者前台（Next.js 15）
│       ├── package.json          # name: "@repo/consumer"
│       ├── next.config.ts
│       ├── tailwind.config.ts    # 繼承 @repo/config + Consumer 主題覆寫
│       ├── tsconfig.json
│       └── src/
│           ├── app/              # Next.js App Router 路由
│           ├── components/       # Consumer 專屬元件
│           ├── lib/              # Consumer 工具函式（含 Zustand stores）
│           └── styles/           # Consumer 全域樣式與主題 CSS 變數
│
└── packages/
    ├── ui/                       # @repo/ui — 共享 UI 元件
    │   ├── package.json
    │   ├── tsconfig.json
    │   └── src/
    │       ├── components/       # shadcn/ui 基礎元件
    │       ├── primitives/       # Radix UI 原始元件封裝
    │       └── index.ts          # 統一匯出
    │
    ├── api-client/               # @repo/api-client — 共享 API 客戶端
    │   ├── package.json
    │   ├── tsconfig.json
    │   └── src/
    │       ├── client.ts         # Axios 客戶端（攔截器、錯誤處理）
    │       ├── types/            # Domain Types（對應後端模型）
    │       ├── services/         # 各領域 API 服務
    │       ├── hooks/            # React Query hooks
    │       ├── query-keys.ts     # 集中管理 Query Keys
    │       └── index.ts          # 統一匯出
    │
    └── config/                   # @repo/config — 共享設定
        ├── package.json
        ├── tsconfig.base.json    # 基礎 TypeScript 設定
        ├── tailwind.config.base.ts # 基礎 Tailwind 設定（共享 plugin、字型）
        └── eslint.config.mjs     # 共享 ESLint 設定
```


### 1.2 Turborepo 管線設定

```jsonc
// turbo.json
{
  "$schema": "https://turbo.build/schema.json",
  "globalDependencies": [".env"],
  "tasks": {
    "build": {
      "dependsOn": ["^build"],
      "outputs": [".next/**", "dist/**"]
    },
    "dev": {
      "cache": false,
      "persistent": true
    },
    "lint": {
      "dependsOn": ["^build"]
    },
    "test": {
      "dependsOn": ["^build"]
    },
    "type-check": {
      "dependsOn": ["^build"]
    }
  }
}
```

### 1.3 套件依賴關係

```
@repo/config ← 無依賴（基礎設定）
@repo/ui ← 依賴 @repo/config
@repo/api-client ← 依賴 @repo/config
@repo/cmc ← 依賴 @repo/ui, @repo/api-client, @repo/config
@repo/consumer ← 依賴 @repo/ui, @repo/api-client, @repo/config
```

### 1.4 共享依賴版本（根 package.json）

| 依賴 | 版本 | 用途 |
|------|------|------|
| next | 15.x | 應用程式框架 |
| react / react-dom | 19.x | UI 渲染 |
| typescript | 5.5+ | 型別系統 |
| tailwindcss | 4.x | 樣式框架 |
| @tanstack/react-query | 5.x | 伺服器狀態管理 |
| axios | 1.x | HTTP 客戶端 |
| zustand | 5.x | 客戶端狀態管理 |
| lucide-react | 0.5x+ | 圖示庫 |
| zod | 4.x | 執行時驗證 |

---

## 2. 共享套件設計

### 2.1 @repo/ui — 共享 UI 元件庫

#### 元件清單

從現有 `cmc-frontend/src/components/ui/` 遷移並擴充：

| 元件 | 來源 | 說明 |
|------|------|------|
| Button | 現有 `button.tsx` | 按鈕，支援 variant / size |
| Input | 現有 `input.tsx` | 文字輸入框 |
| Label | 現有 `label.tsx` | 表單標籤 |
| Select | 現有 `select.tsx` | 下拉選單（Radix UI） |
| Dialog | 現有 `dialog.tsx` | 對話框 |
| Card | 現有 `card.tsx` | 卡片容器 |
| Badge | 現有 `badge.tsx` | 標籤徽章 |
| Alert | 現有 `alert.tsx` | 提示訊息 |
| Textarea | 現有 `textarea.tsx` | 多行文字輸入 |
| Loading | 現有 `loading.tsx` | 載入指示器 |
| EmptyState | 現有 `empty-state.tsx` | 空狀態提示 |
| Skeleton | 新增 | 載入佔位元件 |
| Table | 新增 | 資料表格（含排序、分頁） |
| Tabs | 新增 | 頁籤切換 |
| Toast | 新增 | 通知提示（取代 react-hot-toast） |
| DropdownMenu | 新增 | 下拉選單 |
| Sheet | 新增 | 側邊抽屜（行動版導航用） |
| Pagination | 新增 | 分頁元件 |

#### 主題機制

元件透過 CSS 變數實現主題切換，不硬編碼色彩：

```css
/* 元件內部使用語義化變數 */
.btn-primary {
  background-color: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
}
```

各應用程式在自己的 `globals.css` 中定義變數值（見第 3 節）。


### 2.2 @repo/api-client — 共享 API 客戶端

#### 架構設計

```
packages/api-client/src/
├── client.ts              # ApiClient 類別（Axios 實例、攔截器）
├── types/
│   ├── common.ts          # Money, PageRequest, PageResponse, ApiResponse
│   ├── customer.ts        # Customer, CustomerId
│   ├── order.ts           # Order, OrderItem, OrderStatus, CreateOrderRequest
│   ├── product.ts         # Product, ProductId
│   ├── payment.ts         # Payment, PaymentStatus, ProcessPaymentRequest
│   ├── inventory.ts       # Inventory
│   ├── promotion.ts       # Promotion, PromotionCondition
│   ├── cart.ts            # CartItem（Consumer 專用）
│   └── index.ts           # 統一匯出所有型別
├── services/
│   ├── order.service.ts   # orderService
│   ├── product.service.ts # productService
│   ├── customer.service.ts
│   ├── payment.service.ts
│   ├── inventory.service.ts
│   ├── promotion.service.ts
│   ├── cart.service.ts    # Consumer 購物車 API
│   ├── consumer-product.service.ts  # Consumer 商品 API（/api/consumer/products）
│   ├── stats.service.ts
│   └── index.ts
├── hooks/
│   ├── use-orders.ts      # useOrders, useOrder, useCreateOrder, useSubmitOrder, useCancelOrder
│   ├── use-products.ts    # useProducts, useProduct, useUpdateProduct, useDeleteProduct
│   ├── use-customers.ts   # useCustomers, useCustomer
│   ├── use-payments.ts    # useProcessPayment, useOrderPayments
│   ├── use-inventory.ts   # useInventory, useAdjustInventory, useCheckInventory
│   ├── use-promotions.ts  # usePromotions, useActivePromotions, useApplyPromotion
│   ├── use-cart.ts        # useCart, useAddToCart, useUpdateCartItem, useRemoveCartItem
│   ├── use-consumer-products.ts  # useConsumerProducts, useProductSearch, useCategories
│   ├── use-stats.ts       # useStats, useOrderStatusStats, usePaymentMethodStats
│   └── index.ts
├── query-keys.ts          # 集中管理所有 React Query keys
└── index.ts               # 套件入口，統一匯出
```

#### ApiClient 設計

基於現有 `cmc-frontend/src/services/api.ts` 重構，關鍵變更：

1. **可設定 baseURL**：透過 `createApiClient(config)` 工廠函式，各 app 傳入不同的 `NEXT_PUBLIC_API_URL`
2. **認證攔截器**：保留現有 401 處理邏輯，但改為可注入的 `onUnauthorized` 回呼
3. **型別拆分**：將現有 `domain.ts` 的 130+ 行型別拆分為獨立模組

```typescript
// packages/api-client/src/client.ts
export interface ApiClientConfig {
  baseURL: string
  onUnauthorized?: () => void  // 各 app 自行處理登出邏輯
  getAuthToken?: () => string | null
}

export function createApiClient(config: ApiClientConfig): AxiosInstance {
  const client = axios.create({
    baseURL: config.baseURL,
    timeout: 10000,
    headers: { 'Content-Type': 'application/json' },
  })
  // 請求攔截器：注入 auth token
  // 響應攔截器：401 時呼叫 onUnauthorized
  return client
}
```

#### React Query Hooks 設計

基於現有 `cmc-frontend/src/hooks/useApi.ts` 重構，新增 Consumer 專用 hooks：

```typescript
// 新增：Consumer 購物車 hooks
export const useCart = (customerId: string) => useQuery({
  queryKey: queryKeys.cart(customerId),
  queryFn: () => cartService.getItems(customerId),
  enabled: !!customerId,
})

export const useAddToCart = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ customerId, item }: { customerId: string; item: AddCartItemRequest }) =>
      cartService.addItem(customerId, item),
    onSuccess: (_, { customerId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.cart(customerId) })
    },
  })
}

// 新增：Consumer 商品搜尋 hooks
export const useProductSearch = (keyword: string) => useQuery({
  queryKey: queryKeys.productSearch(keyword),
  queryFn: () => consumerProductService.search(keyword),
  enabled: keyword.length > 0,
})

export const useCategories = () => useQuery({
  queryKey: queryKeys.categories,
  queryFn: () => consumerProductService.getCategories(),
  staleTime: 30 * 60 * 1000, // 30 分鐘
})
```


### 2.3 @repo/config — 共享設定

#### TypeScript 基礎設定

```jsonc
// packages/config/tsconfig.base.json
{
  "compilerOptions": {
    "target": "ES2022",
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "jsx": "react-jsx"
  }
}
```

#### Tailwind 基礎設定

```typescript
// packages/config/tailwind.config.base.ts
import type { Config } from 'tailwindcss'

export const baseConfig: Partial<Config> = {
  theme: {
    extend: {
      fontFamily: {
        sans: ['Geist Sans', 'Inter', 'system-ui', 'sans-serif'],
        heading: ['Geist Sans', 'system-ui', 'sans-serif'],
        mono: ['Geist Mono', 'monospace'],
      },
      borderRadius: {
        DEFAULT: 'var(--radius)',
      },
      colors: {
        border: 'hsl(var(--border))',
        input: 'hsl(var(--input))',
        ring: 'hsl(var(--ring))',
        background: 'hsl(var(--background))',
        foreground: 'hsl(var(--foreground))',
        primary: {
          DEFAULT: 'hsl(var(--primary))',
          foreground: 'hsl(var(--primary-foreground))',
        },
        secondary: {
          DEFAULT: 'hsl(var(--secondary))',
          foreground: 'hsl(var(--secondary-foreground))',
        },
        destructive: {
          DEFAULT: 'hsl(var(--destructive))',
          foreground: 'hsl(var(--destructive-foreground))',
        },
        muted: {
          DEFAULT: 'hsl(var(--muted))',
          foreground: 'hsl(var(--muted-foreground))',
        },
        accent: {
          DEFAULT: 'hsl(var(--accent))',
          foreground: 'hsl(var(--accent-foreground))',
        },
        card: {
          DEFAULT: 'hsl(var(--card))',
          foreground: 'hsl(var(--card-foreground))',
        },
      },
    },
  },
}
```

---

## 3. 設計系統

### 3.1 Consumer 設計系統 — Soft Modern

#### 色彩 Token（CSS 變數）

```css
/* apps/consumer/src/styles/globals.css */
:root {
  /* 主色：Indigo 600 */
  --primary: 239 84% 67%;           /* #4F46E5 */
  --primary-foreground: 0 0% 100%;

  /* 強調色：Amber 500 */
  --accent: 38 92% 50%;             /* #F59E0B */
  --accent-foreground: 0 0% 9%;

  /* 中性色：Stone 色階 */
  --background: 60 9% 98%;          /* Stone 50 */
  --foreground: 24 10% 10%;         /* Stone 900 */
  --muted: 60 5% 96%;               /* Stone 100 */
  --muted-foreground: 25 5% 45%;    /* Stone 500 */
  --border: 20 6% 90%;              /* Stone 200 */
  --input: 20 6% 90%;
  --ring: 239 84% 67%;              /* 同主色 */

  /* 卡片 */
  --card: 0 0% 100%;
  --card-foreground: 24 10% 10%;

  /* 次要色 */
  --secondary: 60 5% 96%;
  --secondary-foreground: 24 10% 10%;

  /* 危險色 */
  --destructive: 0 84% 60%;
  --destructive-foreground: 0 0% 100%;

  /* 圓角：大圓角 */
  --radius: 0.75rem;                /* 12px */
}
```

#### 風格特徵

| 屬性 | 值 | 說明 |
|------|-----|------|
| 圓角 | 12-16px | 柔和、友善的視覺感受 |
| 陰影 | `shadow-sm` / `shadow-md` | 柔和陰影，不使用硬邊框 |
| 間距 | 寬鬆（p-6, gap-6） | 充裕的呼吸空間 |
| 動畫 | `transition-colors duration-200` | 平滑但不誇張 |
| 卡片 | `bg-white rounded-xl shadow-sm border border-stone-100` | 微妙的邊框 + 陰影 |
| 按鈕 | `rounded-xl px-6 py-3` | 大圓角、舒適的點擊區域 |


### 3.2 CMC 設計系統 — Functional Minimal

#### 色彩 Token（CSS 變數）

```css
/* apps/cmc/src/styles/globals.css */
:root {
  /* 主色：Blue 600 */
  --primary: 217 91% 60%;           /* #2563EB */
  --primary-foreground: 0 0% 100%;

  /* 中性色：Slate 色階 */
  --background: 210 40% 98%;        /* Slate 50 */
  --foreground: 222 47% 11%;        /* Slate 900 */
  --muted: 210 40% 96%;             /* Slate 100 */
  --muted-foreground: 215 16% 47%;  /* Slate 500 */
  --border: 214 32% 91%;            /* Slate 200 */
  --input: 214 32% 91%;
  --ring: 217 91% 60%;

  --card: 0 0% 100%;
  --card-foreground: 222 47% 11%;
  --secondary: 210 40% 96%;
  --secondary-foreground: 222 47% 11%;
  --destructive: 0 84% 60%;
  --destructive-foreground: 0 0% 100%;
  --accent: 210 40% 96%;
  --accent-foreground: 222 47% 11%;

  /* 圓角：較小 */
  --radius: 0.5rem;                 /* 8px */
}

/* 深色模式 */
.dark {
  --background: 222 47% 11%;
  --foreground: 210 40% 98%;
  --muted: 217 33% 17%;
  --muted-foreground: 215 20% 65%;
  --border: 217 33% 17%;
  --input: 217 33% 17%;
  --card: 222 47% 11%;
  --card-foreground: 210 40% 98%;
  --primary: 217 91% 60%;
  --primary-foreground: 0 0% 100%;
  --secondary: 217 33% 17%;
  --secondary-foreground: 210 40% 98%;
  --accent: 217 33% 17%;
  --accent-foreground: 210 40% 98%;
  --destructive: 0 63% 31%;
  --destructive-foreground: 210 40% 98%;
}
```

#### 風格特徵

| 屬性 | 值 | 說明 |
|------|-----|------|
| 圓角 | 6-8px | 緊湊、專業 |
| 陰影 | 極少使用，以邊框為主 | 高數據密度下減少視覺噪音 |
| 間距 | 緊湊（p-4, gap-4） | 最大化數據展示空間 |
| 表格 | 緊湊行高、斑馬紋 | 高效瀏覽大量數據 |
| 側邊欄 | 固定寬度 240px | 保留現有 Sidebar 結構 |

### 3.3 字型設定

兩個應用程式共用相同字型，透過 `@repo/config` 的 Tailwind 基礎設定統一管理：

| 用途 | 字型 | CSS 類別 | 載入方式 |
|------|------|----------|----------|
| 標題 | Geist Sans | `font-heading` | `next/font/google` |
| 內文 | Inter | `font-sans` | `next/font/google` |
| 程式碼/數據 | Geist Mono | `font-mono` | `next/font/google` |

---

## 4. Consumer 前台頁面設計

### 4.1 路由結構

```
apps/consumer/src/app/
├── layout.tsx                    # 根佈局（字型載入、QueryClientProvider、Zustand）
├── globals.css                   # Consumer 主題 CSS 變數
├── error.tsx                     # 全域錯誤邊界
├── global-error.tsx              # 最外層錯誤邊界
├── not-found.tsx                 # 404 頁面
│
├── (shop)/                       # 購物區域群組佈局
│   ├── layout.tsx                # 共用佈局：Navbar + Footer
│   ├── page.tsx                  # 首頁（需求 17）
│   ├── products/
│   │   ├── page.tsx              # 商品列表（需求 7）
│   │   ├── [productId]/
│   │   │   └── page.tsx          # 商品詳情（需求 7）
│   │   └── loading.tsx
│   ├── cart/
│   │   ├── page.tsx              # 購物車（需求 8）
│   │   └── loading.tsx
│   ├── checkout/
│   │   ├── page.tsx              # 結帳（需求 9）
│   │   └── loading.tsx
│   └── orders/
│       ├── page.tsx              # 我的訂單列表（需求 9）
│       ├── [orderId]/
│       │   └── page.tsx          # 訂單詳情（需求 9）
│       └── loading.tsx
│
└── (auth)/                       # 認證區域（未來擴充）
    ├── login/page.tsx
    └── register/page.tsx
```


### 4.2 元件架構

#### 佈局元件

```
apps/consumer/src/components/
├── layout/
│   ├── Navbar.tsx                # 頂部導航列（Logo、搜尋、購物車、使用者選單）
│   ├── MobileNav.tsx             # 行動版漢堡選單（使用 @repo/ui Sheet）
│   ├── Footer.tsx                # 頁尾（網站連結、聯絡資訊）
│   ├── SearchBar.tsx             # 搜尋列（含 300ms 防抖）
│   └── CartBadge.tsx             # 購物車數量徽章（讀取 Zustand store）
```

#### 首頁元件（需求 17）

```
├── home/
│   ├── HeroBanner.tsx            # Hero 橫幅區（Server Component）
│   ├── FeaturedProducts.tsx      # 精選商品區（Server Component，呼叫 API）
│   ├── CategoryGrid.tsx          # 商品分類區（Server Component）
│   └── PromotionSection.tsx      # 促銷活動區（Server Component）
```

#### 商品元件（需求 7）

```
├── product/
│   ├── ProductGrid.tsx           # 商品網格（響應式 1/2/3/4 欄）
│   ├── ProductCard.tsx           # 商品卡片（圖片、名稱、價格、加入購物車）
│   ├── ProductDetail.tsx         # 商品詳情（Client Component，含互動）
│   ├── ProductSkeleton.tsx       # 商品載入佔位
│   └── CategoryFilter.tsx        # 分類篩選器
```

#### 購物車元件（需求 8）

```
├── cart/
│   ├── CartItemList.tsx          # 購物車商品列表
│   ├── CartItem.tsx              # 單一購物車項目（數量調整、移除）
│   ├── CartSummary.tsx           # 購物車摘要（小計、總計）
│   └── CartEmpty.tsx             # 購物車空狀態
```

#### 訂單元件（需求 9）

```
├── order/
│   ├── CheckoutForm.tsx          # 結帳表單（收貨地址、確認）
│   ├── OrderSummary.tsx          # 訂單摘要（結帳頁用）
│   ├── OrderList.tsx             # 訂單列表
│   ├── OrderCard.tsx             # 訂單卡片（狀態、金額、操作）
│   ├── OrderDetail.tsx           # 訂單詳情
│   ├── OrderStatusBadge.tsx      # 訂單狀態徽章（色彩對應）
│   └── OrderConfirmation.tsx     # 訂單確認頁
```

### 4.3 狀態管理設計

#### Zustand Store（客戶端狀態）

```typescript
// apps/consumer/src/lib/stores/cart-store.ts
interface CartStore {
  items: CartItem[]
  itemCount: number
  // Actions
  setItems: (items: CartItem[]) => void
  addItem: (item: CartItem) => void
  updateQuantity: (productId: string, quantity: number) => void
  removeItem: (productId: string) => void
  clear: () => void
}

// apps/consumer/src/lib/stores/auth-store.ts
interface AuthStore {
  customerId: string | null
  isAuthenticated: boolean
  // Actions
  setCustomerId: (id: string) => void
  logout: () => void
}
```

#### React Query（伺服器狀態）

所有伺服器資料透過 `@repo/api-client` 的 hooks 管理，Consumer 專用 hooks：

| Hook | API 端點 | staleTime |
|------|----------|-----------|
| `useConsumerProducts` | `GET /api/consumer/products` | 10 分鐘 |
| `useProductSearch` | `GET /api/consumer/products/search` | 5 分鐘 |
| `useCategories` | `GET /api/consumer/products/categories` | 30 分鐘 |
| `useCart` | `GET /api/consumer/cart/{id}/items` | 2 分鐘 |
| `useOrders` | `GET /api/orders` | 5 分鐘 |

### 4.4 Server Components vs Client Components 策略

| 元件 | 類型 | 原因 |
|------|------|------|
| 首頁 HeroBanner | Server | 靜態內容，SEO 友善 |
| 首頁 FeaturedProducts | Server | 初始資料可在伺服器端取得 |
| 首頁 CategoryGrid | Server | 分類資料變動頻率低 |
| 商品列表頁 | Server | 初始渲染 + SEO |
| 商品詳情頁 | 混合 | 頁面 Server，「加入購物車」按鈕 Client |
| Navbar | Client | 需要讀取 Zustand（購物車數量、認證狀態） |
| SearchBar | Client | 需要使用者輸入互動 + 防抖 |
| 購物車頁 | Client | 高互動性（數量調整、移除） |
| 結帳頁 | Client | 表單互動 |
| 訂單列表 | Server | 初始渲染，取消按鈕為 Client 子元件 |

---

## 5. CMC 管理後台頁面設計

### 5.1 路由結構

保留現有 `(dashboard)` 群組佈局，新增支付與分析頁面：

```
apps/cmc/src/app/
├── layout.tsx                    # 根佈局（字型、QueryClientProvider、ThemeProvider）
├── globals.css                   # CMC 主題 CSS 變數（含深色模式）
├── error.tsx                     # 全域錯誤邊界（保留現有）
├── global-error.tsx              # 最外層錯誤邊界（保留現有）
├── not-found.tsx                 # 404 頁面（保留現有）
├── providers.tsx                 # Client Providers（保留現有）
│
├── (dashboard)/                  # 儀表板群組佈局
│   ├── layout.tsx                # Sidebar + Header 佈局（保留現有）
│   ├── page.tsx                  # 儀表板首頁（增強，需求 12）
│   ├── orders/
│   │   └── page.tsx              # 訂單管理（保留現有）
│   ├── products/
│   │   └── page.tsx              # 商品管理（保留現有）
│   ├── customers/
│   │   └── page.tsx              # 客戶管理（保留現有）
│   ├── payments/                 # 新增（需求 11）
│   │   └── page.tsx              # 支付管理
│   ├── delivery/
│   │   └── page.tsx              # 物流配送（保留現有側邊欄連結）
│   ├── promotions/
│   │   └── page.tsx              # 促銷活動（保留現有側邊欄連結）
│   └── settings/
│       └── page.tsx              # 系統設定
│
└── (auth)/
    └── login/page.tsx            # 登入頁（未來擴充）
```


### 5.2 遷移策略

CMC 遷移的核心原則是「替換匯入來源，保留業務邏輯」：

#### 步驟 1：UI 元件替換

| 現有匯入 | 替換為 |
|----------|--------|
| `@/components/ui/button` | `@repo/ui` |
| `@/components/ui/card` | `@repo/ui` |
| `@/components/ui/dialog` | `@repo/ui` |
| `@/components/ui/input` | `@repo/ui` |
| `@/components/ui/select` | `@repo/ui` |
| `@/components/ui/badge` | `@repo/ui` |
| `@/components/ui/alert` | `@repo/ui` |
| `@/components/ui/label` | `@repo/ui` |
| `@/components/ui/textarea` | `@repo/ui` |
| `@/components/ui/loading` | `@repo/ui` |
| `@/components/ui/empty-state` | `@repo/ui` |

#### 步驟 2：API 客戶端替換

| 現有匯入 | 替換為 |
|----------|--------|
| `@/services/api` | `@repo/api-client` |
| `@/types/domain` | `@repo/api-client/types` |
| `@/hooks/useApi` | `@repo/api-client/hooks` |

#### 步驟 3：保留的 CMC 專屬元件

以下元件保留在 `apps/cmc/src/components/` 中，不遷移至共享套件：

- `layout/Sidebar.tsx` — CMC 專屬導航結構
- `layout/DashboardLayout.tsx` — CMC 專屬佈局
- `layout/Header.tsx` — CMC 專屬頂部列
- `dashboard/StatsCard.tsx` — CMC 儀表板統計卡片
- `dashboard/FeatureCard.tsx` — CMC 功能模組卡片
- `dashboard/ActivityTimeline.tsx` — CMC 活動時間線
- `order/OrderCard.tsx` — CMC 訂單卡片
- `order/OrderList.tsx` — CMC 訂單列表
- `product/ProductCard.tsx` — CMC 商品卡片
- `product/EditProductDialog.tsx` — CMC 商品編輯對話框
- `customer/CustomerCard.tsx` — CMC 客戶卡片
- `inventory/AdjustInventoryDialog.tsx` — CMC 庫存調整對話框

### 5.3 新增頁面設計

#### 支付管理頁面（需求 11）

```
apps/cmc/src/components/payment/
├── PaymentTable.tsx              # 支付記錄表格（ID、訂單、金額、方式、狀態、時間）
├── PaymentStatusBadge.tsx        # 支付狀態徽章
├── PaymentDetail.tsx             # 支付詳情面板
├── RefundDialog.tsx              # 退款確認對話框
└── CancelPaymentDialog.tsx       # 取消支付確認對話框
```

API 整合：
- 列表：`GET /api/payments` → `useQuery` + `PaymentTable`
- 退款：`POST /api/payments/{id}/refund` → `useMutation` + `RefundDialog`
- 取消：`POST /api/payments/{id}/cancel` → `useMutation` + `CancelPaymentDialog`

#### 數據分析儀表板增強（需求 12）

```
apps/cmc/src/components/dashboard/
├── StatsCard.tsx                 # 現有，增強趨勢指示
├── OrderStatusChart.tsx          # 新增：訂單狀態分佈圖表
├── PaymentMethodChart.tsx        # 新增：支付方式分佈圖表
├── KPIGrid.tsx                   # 新增：KPI 指標網格
└── AutoRefreshProvider.tsx       # 新增：60 秒自動重新整理
```

圖表方案：使用 `recharts`（輕量級 React 圖表庫），取代 Consumer 端的 `chart.js`。

API 整合：
- 總覽：`GET /api/stats` → `useStats()` + `KPIGrid`
- 訂單狀態：`GET /api/stats/order-status` → `useOrderStatusStats()` + `OrderStatusChart`
- 支付方式：`GET /api/stats/payment-methods` → `usePaymentMethodStats()` + `PaymentMethodChart`
- 自動重新整理：`refetchInterval: 60_000` 設定在 React Query hooks 中

---

## 6. 響應式設計策略

### 6.1 斷點定義

使用 Tailwind CSS 預設斷點：

| 斷點 | 寬度 | Consumer | CMC |
|------|------|----------|-----|
| `sm` | 640px | 手機橫向 | — |
| `md` | 768px | 平板 | 平板 |
| `lg` | 1024px | 桌面 | 桌面 |
| `xl` | 1280px | 大螢幕 | 大螢幕 |
| `2xl` | 1536px | — | 超寬螢幕 |

### 6.2 Consumer 商品列表響應式

```tsx
// 商品網格：1 欄 → 2 欄 → 3 欄 → 4 欄
<div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
  {products.map(product => <ProductCard key={product.id} product={product} />)}
</div>
```

### 6.3 Consumer 導航列響應式

- `lg` 以上：完整水平導航（Logo、搜尋列、導航連結、購物車、使用者選單）
- `lg` 以下：Logo + 購物車圖示 + 漢堡選單按鈕，點擊展開 `Sheet` 側邊抽屜

### 6.4 CMC 側邊欄響應式

- `lg` 以上：固定側邊欄 240px + 主內容區
- `md` - `lg`：可收合側邊欄（圖示模式 64px）
- `md` 以下：隱藏側邊欄，透過漢堡選單觸發 `Sheet` 覆蓋

---

## 7. 效能設計

### 7.1 Next.js 15 最佳化策略

| 策略 | 實作方式 | 目標 |
|------|----------|------|
| Server Components | 預設使用，僅互動元件標記 `'use client'` | 減少客戶端 JS bundle |
| 路由級程式碼分割 | Next.js App Router 自動處理 | 每頁僅載入所需 JS |
| 圖片最佳化 | `next/image` + WebP/AVIF 自動轉換 | LCP ≤ 2.5s |
| 預取 | `<Link prefetch>` 預設啟用 | 頁面切換 ≤ 200ms |
| 字型最佳化 | `next/font` 自動子集化 + `font-display: swap` | 避免 FOIT |

### 7.2 React Query 快取策略

| 資料類型 | staleTime | cacheTime | refetchOnWindowFocus |
|----------|-----------|-----------|---------------------|
| 商品分類 | 30 分鐘 | 60 分鐘 | false |
| 商品列表 | 10 分鐘 | 30 分鐘 | true |
| 購物車 | 2 分鐘 | 10 分鐘 | true |
| 訂單列表 | 5 分鐘 | 15 分鐘 | true |
| 統計數據 | 5 分鐘 | 15 分鐘 | true |
| CMC 儀表板 | 1 分鐘 | 5 分鐘 | true（+ 60s 自動重新整理） |

### 7.3 Bundle 最佳化

- `recharts`：僅在 CMC 儀表板頁面動態匯入（`next/dynamic`）
- `zod`：僅在含表單的頁面使用
- `date-fns`：使用 tree-shakable 匯入（`import { format } from 'date-fns'`）

---

## 8. 錯誤處理設計

### 8.1 錯誤邊界層級

```
global-error.tsx          ← 最外層，捕獲 layout.tsx 的錯誤
└── layout.tsx
    └── (shop)/layout.tsx
        └── error.tsx     ← 頁面級錯誤邊界
            └── page.tsx
                └── loading.tsx  ← 頁面級載入狀態
```

### 8.2 API 錯誤處理流程

```
API 請求失敗
├── 401 Unauthorized → 清除 token → 重導向 /login
├── 403 Forbidden → 顯示「權限不足」提示
├── 404 Not Found → 顯示「資源不存在」提示
├── 4xx Client Error → 解析錯誤訊息 → 顯示中文錯誤提示
├── 5xx Server Error → 顯示「系統錯誤，請稍後重試」+ 重試按鈕
└── Network Error → 顯示「網路連線中斷」提示
```

### 8.3 錯誤訊息中文化

在 `@repo/api-client` 中提供錯誤訊息映射：

```typescript
const ERROR_MESSAGES: Record<string, string> = {
  'CUSTOMER_NOT_FOUND': '找不到該客戶',
  'ORDER_NOT_FOUND': '找不到該訂單',
  'INSUFFICIENT_INVENTORY': '庫存不足',
  'PAYMENT_FAILED': '支付處理失敗',
  'ORDER_CANNOT_BE_CANCELLED': '此訂單無法取消',
  'INVALID_ORDER_STATUS': '訂單狀態不正確',
}
```


---

## 9. 無障礙設計

### 9.1 實作標準

| 項目 | 實作方式 |
|------|----------|
| 語義化 HTML | 使用 `<nav>`, `<main>`, `<article>`, `<section>`, `<header>`, `<footer>` |
| 圖片 alt | 所有 `<Image>` 元件必須提供描述性 `alt` |
| 表單 label | 所有 `<Input>` 搭配 `<Label htmlFor>` |
| 鍵盤導航 | Radix UI 元件內建支援；自訂元件加入 `tabIndex`, `onKeyDown` |
| 色彩對比 | Consumer 主色 Indigo 600 在白底上對比度 > 4.5:1 ✓ |
| 減少動畫 | `@media (prefers-reduced-motion: reduce)` 停用 transition |
| 焦點指示 | `focus-visible:ring-2 focus-visible:ring-primary` |
| cursor | 所有可點擊元素加入 `cursor-pointer` |

### 9.2 @repo/ui 元件內建無障礙

所有 Radix UI 基礎元件已內建：
- ARIA 屬性（`role`, `aria-expanded`, `aria-selected` 等）
- 鍵盤導航（Tab, Enter, Space, Escape, Arrow keys）
- 焦點管理（Focus trap in Dialog/Sheet）

---

## 10. 建構與部署設計

### 10.1 Docker 建構

```dockerfile
# Dockerfile.consumer（多階段建構）
FROM node:20-alpine AS base
RUN corepack enable && corepack prepare pnpm@latest --activate

FROM base AS deps
WORKDIR /app
COPY pnpm-lock.yaml pnpm-workspace.yaml package.json ./
COPY apps/consumer/package.json ./apps/consumer/
COPY packages/ui/package.json ./packages/ui/
COPY packages/api-client/package.json ./packages/api-client/
COPY packages/config/package.json ./packages/config/
RUN pnpm install --frozen-lockfile

FROM base AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ARG NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
RUN pnpm build --filter=@repo/consumer

FROM base AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY --from=builder /app/apps/consumer/.next/standalone ./
COPY --from=builder /app/apps/consumer/.next/static ./apps/consumer/.next/static
COPY --from=builder /app/apps/consumer/public ./apps/consumer/public
EXPOSE 3000
CMD ["node", "apps/consumer/server.js"]
```

### 10.2 環境變數

| 變數 | Consumer | CMC | 說明 |
|------|----------|-----|------|
| `NEXT_PUBLIC_API_URL` | `http://api:8080/api` | `http://api:8080/api` | 後端 API 基礎 URL |
| `NEXT_PUBLIC_APP_NAME` | `電商平台` | `商務管理中心` | 應用程式名稱 |

### 10.3 增量建構

```bash
# 僅建構 Consumer 及其依賴
pnpm build --filter=@repo/consumer...

# 僅建構 CMC 及其依賴
pnpm build --filter=@repo/cmc...

# 建構所有
pnpm build
```

---

## 11. 實作階段與任務對應

### 階段一：Monorepo 建立 + 共享套件

對應需求：1, 2, 3, 18

| 任務 | 產出 |
|------|------|
| 初始化 Turborepo + pnpm workspace | `turbo.json`, `pnpm-workspace.yaml`, 根 `package.json` |
| 建立 `@repo/config` | `tsconfig.base.json`, `tailwind.config.base.ts`, `eslint.config.mjs` |
| 建立 `@repo/ui` | 遷移 11 個現有 shadcn/ui 元件 + 新增 7 個元件 |
| 建立 `@repo/api-client` | 重構型別、服務、hooks |
| 驗證 `pnpm build` 全套件建構成功 | CI 綠燈 |

### 階段二：CMC 遷移

對應需求：5, 10

| 任務 | 產出 |
|------|------|
| 遷移 `cmc-frontend/` → `apps/cmc/` | 目錄結構調整 |
| 替換 UI 元件匯入為 `@repo/ui` | 所有 `@/components/ui/*` 替換 |
| 替換 API 客戶端匯入為 `@repo/api-client` | `api.ts`, `domain.ts`, `useApi.ts` 替換 |
| 套用 CMC 設計系統色彩 | `globals.css` 更新 |
| 驗證所有現有功能正常 | 手動測試通過 |

### 階段三：Consumer 核心框架

對應需求：4, 6, 17

| 任務 | 產出 |
|------|------|
| 建立 `apps/consumer/` Next.js 15 應用程式 | 基礎結構 |
| 實作 Consumer 設計系統 | `globals.css` + 主題 token |
| 實作核心佈局（Navbar、Footer、MobileNav） | 佈局元件 |
| 實作首頁（Hero、精選商品、分類、促銷） | 首頁完成 |
| 設定 Zustand stores（cart、auth） | 狀態管理 |

### 階段四：Consumer 電商頁面

對應需求：7, 8, 9

| 任務 | 產出 |
|------|------|
| 實作商品列表與搜尋頁面 | 商品瀏覽功能 |
| 實作商品詳情頁面 | 商品詳情功能 |
| 實作購物車頁面 | 購物車功能 |
| 實作結帳頁面 | 結帳流程 |
| 實作訂單列表與詳情頁面 | 訂單管理功能 |

### 階段五：CMC 補充頁面

對應需求：11, 12

| 任務 | 產出 |
|------|------|
| 實作支付管理頁面 | 支付列表、退款、取消 |
| 增強儀表板（圖表、KPI、自動重新整理） | 數據分析功能 |

### 階段六：端對端測試

對應需求：13, 14, 15, 16, 19, 20

| 任務 | 產出 |
|------|------|
| 響應式設計驗證（所有斷點） | 響應式測試報告 |
| 效能測試（Core Web Vitals） | 效能測試報告 |
| 無障礙檢查 | 無障礙測試報告 |
| 錯誤處理驗證 | 錯誤場景測試 |
| Docker 建構與部署驗證 | 部署流程驗證 |
| E2E 測試實作（Playwright） | 核心流程自動化測試 |

---

## 12. E2E 測試計畫

### 12.1 測試框架

使用 Playwright，設定在 Monorepo 根層級：

```
frontend/
├── e2e/
│   ├── playwright.config.ts      # 設定（兩個 app 的 baseURL）
│   ├── consumer/                 # Consumer E2E 測試
│   │   ├── home.spec.ts
│   │   ├── products.spec.ts
│   │   ├── cart.spec.ts
│   │   ├── checkout.spec.ts
│   │   └── orders.spec.ts
│   └── cmc/                      # CMC E2E 測試
│       ├── dashboard.spec.ts
│       ├── orders.spec.ts
│       ├── products.spec.ts
│       ├── customers.spec.ts
│       └── payments.spec.ts
```

### 12.2 核心測試場景

#### Consumer 端

1. 首頁載入 → 精選商品顯示 → 點擊商品 → 進入詳情頁
2. 搜尋商品 → 結果顯示 → 分類篩選
3. 加入購物車 → 購物車數量更新 → 修改數量 → 移除商品
4. 完整結帳流程：加入購物車 → 結帳 → 填寫地址 → 確認 → 訂單建立
5. 查看訂單列表 → 訂單詳情 → 取消訂單

#### CMC 端

1. 儀表板載入 → 統計數據顯示 → 圖表渲染
2. 訂單管理：列表 → 詳情 → 狀態操作
3. 商品管理：列表 → 編輯 → 儲存
4. 支付管理：列表 → 詳情 → 退款操作
5. 響應式：側邊欄收合 → 行動版導航

---

## 13. 自訂 Sub-Agent 設計

為高效執行各階段任務，設計以下專用 sub-agent：

### 13.1 monorepo-scaffold-agent

**用途**：執行階段一，建立 Monorepo 骨架與共享套件

**職責**：
- 初始化 Turborepo + pnpm workspace
- 建立 `@repo/config`（TypeScript、Tailwind、ESLint 設定）
- 建立 `@repo/ui`（遷移現有 shadcn/ui 元件 + 新增元件）
- 建立 `@repo/api-client`（重構型別、服務、hooks）
- 驗證 `pnpm build` 成功

**輸入上下文**：
- 本設計文件第 1-2 節
- 現有 `cmc-frontend/src/components/ui/` 所有元件
- 現有 `cmc-frontend/src/services/api.ts`
- 現有 `cmc-frontend/src/types/domain.ts`
- 現有 `cmc-frontend/src/hooks/useApi.ts`

### 13.2 cmc-migration-agent

**用途**：執行階段二，將 CMC 遷移至 Monorepo

**職責**：
- 複製 `cmc-frontend/` 至 `apps/cmc/`
- 替換所有 UI 元件匯入為 `@repo/ui`
- 替換 API 客戶端匯入為 `@repo/api-client`
- 套用 CMC 設計系統色彩
- 驗證所有現有頁面功能正常

**輸入上下文**：
- 本設計文件第 3.2、5 節
- 現有 `cmc-frontend/` 完整目錄結構

### 13.3 consumer-app-agent

**用途**：執行階段三與四，建構 Consumer 前台

**職責**：
- 建立 `apps/consumer/` Next.js 15 應用程式
- 實作 Consumer 設計系統
- 實作所有頁面（首頁、商品、購物車、結帳、訂單）
- 設定 Zustand stores
- 實作響應式佈局

**輸入上下文**：
- 本設計文件第 3.1、4 節
- `@repo/ui` 元件清單
- `@repo/api-client` hooks 清單
- 後端 API 端點規格

### 13.4 cmc-enhancement-agent

**用途**：執行階段五，CMC 新增頁面

**職責**：
- 實作支付管理頁面
- 增強儀表板（圖表、KPI、自動重新整理）
- 整合 recharts 圖表庫

**輸入上下文**：
- 本設計文件第 5.3 節
- `@repo/api-client` 支付與統計 hooks

### 13.5 e2e-test-agent

**用途**：執行階段六，E2E 測試與品質驗證

**職責**：
- 設定 Playwright 測試框架
- 實作 Consumer 與 CMC 的 E2E 測試
- 執行響應式設計驗證
- 執行效能測試
- 執行無障礙檢查
- 驗證 Docker 建構

**輸入上下文**：
- 本設計文件第 12 節
- 所有已實作的頁面路由

---

## 14. 需求追溯矩陣

| 需求 | 設計章節 | 階段 |
|------|----------|------|
| 需求 1：Monorepo 架構 | 1.1 - 1.4 | 階段一 |
| 需求 2：共享 UI 元件庫 | 2.1 | 階段一 |
| 需求 3：共享 API 客戶端 | 2.2 | 階段一 |
| 需求 4：Consumer 設計系統 | 3.1 | 階段三 |
| 需求 5：CMC 設計系統 | 3.2 | 階段二 |
| 需求 6：Consumer 核心佈局 | 4.1, 4.2 | 階段三 |
| 需求 7：商品瀏覽與搜尋 | 4.2（商品元件） | 階段四 |
| 需求 8：購物車功能 | 4.2（購物車元件）, 4.3 | 階段四 |
| 需求 9：結帳與訂單 | 4.2（訂單元件） | 階段四 |
| 需求 10：CMC 遷移 | 5.1, 5.2 | 階段二 |
| 需求 11：支付管理頁面 | 5.3 | 階段五 |
| 需求 12：數據分析儀表板 | 5.3 | 階段五 |
| 需求 13：響應式設計 | 6 | 階段六 |
| 需求 14：前端效能 | 7 | 階段六 |
| 需求 15：無障礙設計 | 9 | 階段六 |
| 需求 16：錯誤處理 | 8 | 階段三-五 |
| 需求 17：Consumer 首頁 | 4.1, 4.2（首頁元件） | 階段三 |
| 需求 18：技術棧統一 | 1.4, 2.3 | 階段一 |
| 需求 19：實作階段規劃 | 11 | 全階段 |
| 需求 20：建構與部署 | 10 | 階段六 |
