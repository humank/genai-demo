# 實作任務

## 階段一：Monorepo 建立 + 共享套件

### 任務 1：初始化 Monorepo 骨架
- [x] 在專案根目錄建立 `frontend/` 目錄作為 Monorepo 根目錄
- [x] 建立 `frontend/pnpm-workspace.yaml`，定義 `apps/*` 與 `packages/*` 兩個 workspace
- [x] 建立 `frontend/package.json`，設定 `name: "frontend-monorepo"`，加入 `dev`、`build`、`lint`、`test` 腳本（透過 `turbo` 執行）
- [x] 建立 `frontend/turbo.json`，設定 `build`（dependsOn: `^build`）、`dev`（cache: false, persistent: true）、`lint`、`test`、`type-check` 管線
- [x] 建立 `frontend/.env.example`，包含 `NEXT_PUBLIC_API_URL` 變數
- [x] 執行 `pnpm init` 並安裝 `turbo` 為根層級 devDependency
- [x] 驗證 `pnpm install` 成功且 workspace 結構正確

### 任務 2：建立 @repo/config 共享設定套件
- [x] 建立 `frontend/packages/config/package.json`，設定 `name: "@repo/config"`
- [x] 建立 `frontend/packages/config/tsconfig.base.json`，設定 `target: ES2022`、`strict: true`、`moduleResolution: bundler`（依設計文件 2.3 節）
- [x] 建立 `frontend/packages/config/tailwind.config.base.ts`，匯出共享的字型設定（Geist Sans、Inter、Geist Mono）與 CSS 變數色彩系統（依設計文件 2.3 節）
- [x] 建立 `frontend/packages/config/eslint.config.mjs`，匯出共享的 ESLint 設定（TypeScript + React + Next.js 規則）
- [x] 驗證套件可被其他 workspace 正確匯入


### 任務 3：建立 @repo/ui 共享 UI 元件庫
- [x] 建立 `frontend/packages/ui/package.json`，設定 `name: "@repo/ui"`，依賴 `@repo/config`、`@radix-ui/*`、`class-variance-authority`、`clsx`、`tailwind-merge`、`lucide-react`
- [x] 建立 `frontend/packages/ui/tsconfig.json`，繼承 `@repo/config/tsconfig.base.json`
- [x] 從 `cmc-frontend/src/components/ui/` 遷移 11 個現有元件至 `packages/ui/src/components/`：button、input、label、select、dialog、card、badge、alert、textarea、loading、empty-state
- [x] 新增 7 個元件：Skeleton、Table（含排序/分頁）、Tabs、Toast（取代 react-hot-toast）、DropdownMenu、Sheet、Pagination
- [x] 確保所有元件使用 CSS 變數（`hsl(var(--primary))` 等）而非硬編碼色彩
- [x] 從 `cmc-frontend/src/lib/utils.ts` 遷移 `cn()` 工具函式至 `packages/ui/src/lib/utils.ts`
- [x] 建立 `packages/ui/src/index.ts` 統一匯出所有元件
- [x] 驗證所有元件的 TypeScript 型別正確匯出

### 任務 4：建立 @repo/api-client 共享 API 客戶端
- [x] 建立 `frontend/packages/api-client/package.json`，設定 `name: "@repo/api-client"`，依賴 `@repo/config`、`axios`、`@tanstack/react-query`
- [x] 將 `cmc-frontend/src/types/domain.ts` 的型別拆分為獨立模組：`types/common.ts`（Money、PageRequest、PageResponse、ApiResponse）、`types/order.ts`、`types/product.ts`、`types/customer.ts`、`types/payment.ts`、`types/inventory.ts`、`types/promotion.ts`、`types/cart.ts`（新增 Consumer 購物車型別）
- [x] 建立 `types/index.ts` 統一匯出所有型別
- [x] 基於 `cmc-frontend/src/services/api.ts` 重構 `client.ts`，改為 `createApiClient(config: ApiClientConfig)` 工廠函式，支援可注入的 `baseURL`、`onUnauthorized`、`getAuthToken`
- [x] 將現有 API 服務拆分為獨立模組：`services/order.service.ts`、`services/product.service.ts`、`services/customer.service.ts`、`services/payment.service.ts`、`services/inventory.service.ts`、`services/promotion.service.ts`、`services/stats.service.ts`
- [x] 新增 Consumer 專用服務：`services/cart.service.ts`（`GET/POST/PUT/DELETE /api/consumer/cart/{customerId}/items`）、`services/consumer-product.service.ts`（`GET /api/consumer/products`、`/search`、`/categories`）
- [x] 建立 `query-keys.ts` 集中管理所有 React Query keys（包含現有 CMC keys + 新增 Consumer keys：cart、productSearch、categories）
- [x] 基於 `cmc-frontend/src/hooks/useApi.ts` 重構 hooks，拆分為獨立模組：`hooks/use-orders.ts`、`hooks/use-products.ts`、`hooks/use-customers.ts`、`hooks/use-payments.ts`、`hooks/use-inventory.ts`、`hooks/use-promotions.ts`、`hooks/use-stats.ts`
- [x] 新增 Consumer 專用 hooks：`hooks/use-cart.ts`（useCart、useAddToCart、useUpdateCartItem、useRemoveCartItem）、`hooks/use-consumer-products.ts`（useConsumerProducts、useProductSearch、useCategories）
- [x] 建立 `index.ts` 統一匯出所有型別、服務、hooks
- [x] 驗證所有 hooks 的 TypeScript 型別正確

### 任務 5：驗證階段一建構
- [x] 執行 `pnpm build` 驗證所有套件依序建構成功（config → ui + api-client）
- [x] 執行 `pnpm lint` 驗證所有套件通過 ESLint 檢查
- [x] 執行 `pnpm type-check` 驗證所有套件通過 TypeScript 型別檢查
- [x] 驗證 Turborepo 快取機制正常運作（第二次 build 應使用快取）


---

## 階段二：CMC 遷移至 Monorepo

### 任務 6：遷移 CMC 目錄結構
- [x] 建立 `frontend/apps/cmc/` 目錄
- [x] 複製 `cmc-frontend/src/` 至 `frontend/apps/cmc/src/`
- [x] 複製 `cmc-frontend/public/` 至 `frontend/apps/cmc/public/`
- [x] 建立 `frontend/apps/cmc/package.json`，設定 `name: "@repo/cmc"`，依賴 `@repo/ui`、`@repo/api-client`、`@repo/config`
- [x] 建立 `frontend/apps/cmc/next.config.ts`，設定 `transpilePackages: ['@repo/ui', '@repo/api-client']`
- [x] 建立 `frontend/apps/cmc/tsconfig.json`，繼承 `@repo/config/tsconfig.base.json`
- [x] 建立 `frontend/apps/cmc/tailwind.config.ts`，繼承 `@repo/config/tailwind.config.base.ts` 並覆寫 CMC 專屬設定

### 任務 7：替換 CMC UI 元件匯入
- [x] 將所有 `import { ... } from '@/components/ui/button'` 替換為 `import { Button } from '@repo/ui'`
- [x] 將所有 `import { ... } from '@/components/ui/card'` 替換為 `import { Card, CardHeader, CardTitle, CardContent } from '@repo/ui'`
- [x] 依此模式替換所有 11 個 UI 元件的匯入（button、input、label、select、dialog、card、badge、alert、textarea、loading、empty-state）
- [x] 刪除 `apps/cmc/src/components/ui/` 目錄（元件已遷移至 @repo/ui）
- [x] 驗證所有頁面的 UI 元件正確渲染

### 任務 8：替換 CMC API 客戶端匯入
- [x] 在 `apps/cmc/src/lib/api-provider.tsx` 中建立 CMC 專用的 ApiClient 初始化（呼叫 `createApiClient` 並傳入 CMC 的 `NEXT_PUBLIC_API_URL` 與 `onUnauthorized` 回呼）
- [x] 將所有 `import { ... } from '@/services/api'` 替換為 `import { ... } from '@repo/api-client'`
- [x] 將所有 `import { ... } from '@/types/domain'` 替換為 `import { ... } from '@repo/api-client/types'`
- [x] 將所有 `import { ... } from '@/hooks/useApi'` 替換為 `import { ... } from '@repo/api-client/hooks'`
- [x] 刪除 `apps/cmc/src/services/api.ts`、`apps/cmc/src/types/domain.ts`、`apps/cmc/src/hooks/useApi.ts`
- [x] 驗證所有 API 呼叫正常運作

### 任務 9：套用 CMC 設計系統
- [x] 更新 `apps/cmc/src/styles/globals.css`（或 `app/globals.css`），套用設計文件 3.2 節的 CMC 色彩 Token（Blue 600 主色 + Slate 中性色 + 深色模式）
- [x] 設定 `--radius: 0.5rem`（8px 圓角）
- [x] 驗證淺色模式與深色模式切換正常，色彩過渡平滑
- [x] 驗證所有現有頁面（儀表板、訂單、商品、客戶）在新色彩方案下正確顯示

---

## 階段三：Consumer 核心框架

### 任務 10：建立 Consumer Next.js 15 應用程式
- [x] 建立 `frontend/apps/consumer/package.json`，設定 `name: "@repo/consumer"`，依賴 `@repo/ui`、`@repo/api-client`、`@repo/config`、`zustand`
- [x] 建立 `frontend/apps/consumer/next.config.ts`，設定 `transpilePackages`、`images` 最佳化（WebP/AVIF）、`output: 'standalone'`
- [x] 建立 `frontend/apps/consumer/tsconfig.json`，繼承 `@repo/config/tsconfig.base.json`
- [x] 建立 `frontend/apps/consumer/tailwind.config.ts`，繼承基礎設定並覆寫 Consumer 專屬設定
- [x] 建立 `apps/consumer/src/app/layout.tsx`（根佈局：字型載入 Geist Sans + Inter + Geist Mono、QueryClientProvider、metadata）
- [x] 建立 `apps/consumer/src/app/providers.tsx`（Client Providers：QueryClientProvider + Zustand hydration）

### 任務 11：實作 Consumer 設計系統
- [x] 建立 `apps/consumer/src/app/globals.css`，定義設計文件 3.1 節的 Consumer 色彩 Token（Indigo 600 主色 + Amber 500 強調色 + Stone 中性色）
- [x] 設定 `--radius: 0.75rem`（12px 大圓角）
- [x] 僅定義 `:root`（淺色模式），不實作 `.dark` 深色模式
- [x] 驗證 CSS 變數在 @repo/ui 元件中正確套用

### 任務 12：實作 Consumer 核心佈局與導航
- [x] 建立 `apps/consumer/src/app/(shop)/layout.tsx`（購物區域群組佈局：Navbar + main + Footer）
- [x] 實作 `components/layout/Navbar.tsx`（Client Component：Logo、SearchBar、導航連結「首頁/商品/購物車/我的訂單」、CartBadge、使用者選單）
- [x] 實作 `components/layout/SearchBar.tsx`（Client Component：搜尋輸入框 + 300ms 防抖 + 導航至 `/products?q=keyword`）
- [x] 實作 `components/layout/CartBadge.tsx`（Client Component：讀取 Zustand cart store 顯示商品數量）
- [x] 實作 `components/layout/MobileNav.tsx`（Client Component：使用 @repo/ui Sheet，lg 以下顯示漢堡選單）
- [x] 實作 `components/layout/Footer.tsx`（Server Component：網站連結、聯絡資訊）
- [x] 驗證導航列在 375px / 768px / 1024px / 1440px 斷點的響應式行為

### 任務 13：設定 Zustand 狀態管理
- [x] 建立 `apps/consumer/src/lib/stores/cart-store.ts`（CartStore：items、itemCount、setItems、addItem、updateQuantity、removeItem、clear）
- [x] 建立 `apps/consumer/src/lib/stores/auth-store.ts`（AuthStore：customerId、isAuthenticated、setCustomerId、logout）
- [x] 在 `providers.tsx` 中設定 Zustand hydration

### 任務 14：實作 Consumer 首頁
- [x] 建立 `apps/consumer/src/app/(shop)/page.tsx`（Server Component：組合 HeroBanner + FeaturedProducts + CategoryGrid + PromotionSection）
- [x] 實作 `components/home/HeroBanner.tsx`（Server Component：Hero 橫幅區，Indigo 漸層背景 + 標題 + CTA 按鈕）
- [x] 實作 `components/home/FeaturedProducts.tsx`（Server Component：呼叫 `GET /api/consumer/products` 取得商品，以 ProductCard 網格展示）
- [x] 實作 `components/home/CategoryGrid.tsx`（Server Component：呼叫 `GET /api/consumer/products/categories` 取得分類，以卡片網格展示）
- [x] 實作 `components/home/PromotionSection.tsx`（Server Component：呼叫促銷 API 取得進行中活動）
- [x] 驗證首頁在行動裝置上以垂直堆疊方式排列各區塊
- [x] 建立 `apps/consumer/src/app/(shop)/loading.tsx`（首頁載入狀態：Skeleton 佔位）


---

## 階段四：Consumer 電商頁面

### 任務 15：實作商品列表與搜尋頁面
- [x] 建立 `apps/consumer/src/app/(shop)/products/page.tsx`（商品列表頁：呼叫 `useConsumerProducts` hook，支援分頁與分類篩選）
- [x] 實作 `components/product/ProductGrid.tsx`（響應式網格：`grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6`）
- [x] 實作 `components/product/ProductCard.tsx`（商品卡片：圖片、名稱、價格、「加入購物車」按鈕，使用 Soft Modern 風格 `rounded-xl shadow-sm`）
- [x] 實作 `components/product/CategoryFilter.tsx`（分類篩選器：呼叫 `useCategories` hook，以 Tabs 或 Select 展示）
- [x] 實作 `components/product/ProductSkeleton.tsx`（載入佔位：模擬 ProductCard 形狀的 Skeleton）
- [x] 實作搜尋功能：URL query parameter `?q=keyword` 觸發 `useProductSearch` hook
- [x] 實作分頁：使用 @repo/ui Pagination 元件，搭配 `PageRequest` / `PageResponse` 型別
- [x] 建立 `apps/consumer/src/app/(shop)/products/loading.tsx`

### 任務 16：實作商品詳情頁面
- [x] 建立 `apps/consumer/src/app/(shop)/products/[productId]/page.tsx`（混合渲染：Server Component 取得商品資料，Client Component 處理互動）
- [x] 實作 `components/product/ProductDetail.tsx`（Client Component：商品名稱、描述、價格、庫存狀態、數量選擇、「加入購物車」按鈕）
- [x] 「加入購物車」按鈕呼叫 `useAddToCart` mutation，成功後更新 Zustand cart store 並顯示 Toast 通知
- [x] 顯示庫存狀態：呼叫 `useInventory` hook，有庫存顯示綠色「有貨」，無庫存顯示紅色「缺貨」並禁用加入購物車按鈕

### 任務 17：實作購物車頁面
- [x] 建立 `apps/consumer/src/app/(shop)/cart/page.tsx`（Client Component：呼叫 `useCart` hook 取得購物車內容）
- [x] 實作 `components/cart/CartItemList.tsx`（購物車商品列表，遍歷 CartItem）
- [x] 實作 `components/cart/CartItem.tsx`（單一商品項目：商品資訊、數量調整按鈕呼叫 `useUpdateCartItem`、移除按鈕呼叫 `useRemoveCartItem`、小計金額）
- [x] 實作 `components/cart/CartSummary.tsx`（購物車摘要：商品總數、總金額、「前往結帳」按鈕導航至 `/checkout`）
- [x] 實作 `components/cart/CartEmpty.tsx`（空購物車狀態：使用 @repo/ui EmptyState，提供「繼續購物」連結）
- [x] 購物車數據同步：`useCart` 成功後呼叫 Zustand `setItems` 更新本地快取
- [x] 加入購物車成功時顯示 @repo/ui Toast 通知

### 任務 18：實作結帳頁面
- [x] 建立 `apps/consumer/src/app/(shop)/checkout/page.tsx`（Client Component：結帳流程）
- [x] 實作 `components/order/CheckoutForm.tsx`（結帳表單：收貨地址輸入，使用 react-hook-form + zod 驗證）
- [x] 實作 `components/order/OrderSummary.tsx`（訂單摘要：從 Zustand cart store 讀取商品列表與總金額）
- [x] 實作結帳流程：確認按鈕依序呼叫 `POST /api/orders`（建立訂單）→ `POST /api/orders/{id}/items`（逐一新增項目）→ `POST /api/orders/{id}/submit`（提交訂單）
- [x] 結帳成功後：清空 Zustand cart store → 清空伺服器端購物車 → 導航至訂單確認頁
- [x] 結帳失敗時：顯示中文錯誤訊息 + 重試按鈕
- [x] 實作 `components/order/OrderConfirmation.tsx`（訂單確認頁：訂單編號、狀態、「查看訂單」連結）

### 任務 19：實作訂單列表與詳情頁面
- [x] 建立 `apps/consumer/src/app/(shop)/orders/page.tsx`（Server Component：呼叫 `useOrders` hook 取得訂單列表）
- [x] 實作 `components/order/OrderList.tsx`（訂單列表：遍歷 OrderCard）
- [x] 實作 `components/order/OrderCard.tsx`（訂單卡片：訂單編號、狀態徽章、商品數量、總金額、建立時間、「查看詳情」連結）
- [x] 實作 `components/order/OrderStatusBadge.tsx`（狀態徽章：CREATED=灰色、PENDING=黃色、CONFIRMED=藍色、SHIPPED=紫色、DELIVERED=綠色、CANCELLED=紅色）
- [x] 建立 `apps/consumer/src/app/(shop)/orders/[orderId]/page.tsx`（訂單詳情頁）
- [x] 實作 `components/order/OrderDetail.tsx`（訂單詳情：狀態、商品明細表格、金額、收貨地址、時間戳記）
- [x] 當訂單狀態為 CREATED 或 PENDING 時，顯示「取消訂單」按鈕，呼叫 `useCancelOrder` mutation
- [x] 取消成功後更新訂單狀態並顯示 Toast 通知


---

## 階段五：CMC 補充頁面

### 任務 20：實作 CMC 支付管理頁面
- [x] 建立 `apps/cmc/src/app/(dashboard)/payments/page.tsx`（支付管理頁面）
- [x] 實作 `components/payment/PaymentTable.tsx`（支付記錄表格：使用 @repo/ui Table，欄位包含支付 ID、訂單 ID、金額、支付方式、狀態、建立時間）
- [x] 實作 `components/payment/PaymentStatusBadge.tsx`（狀態徽章：PENDING=黃色、PROCESSING=藍色、COMPLETED=綠色、FAILED=紅色、CANCELLED=灰色）
- [x] 實作 `components/payment/PaymentDetail.tsx`（支付詳情面板：點擊表格行展開，顯示關聯訂單資訊）
- [x] 實作 `components/payment/RefundDialog.tsx`（退款確認對話框：使用 @repo/ui Dialog，呼叫 `POST /api/payments/{id}/refund`）
- [x] 實作 `components/payment/CancelPaymentDialog.tsx`（取消支付確認對話框：呼叫 `POST /api/payments/{id}/cancel`）
- [x] 退款/取消成功後更新支付狀態並顯示 Toast 通知
- [x] 退款/取消失敗時顯示具體的中文錯誤訊息
- [x] 更新 `Sidebar.tsx` 確認支付管理連結（`/payments`）已存在且正確高亮

### 任務 21：增強 CMC 數據分析儀表板
- [x] 安裝 `recharts` 至 `apps/cmc` 的依賴
- [x] 實作 `components/dashboard/KPIGrid.tsx`（KPI 指標網格：呼叫 `useStats` hook，以 StatsCard 展示總訂單數、總營收、總客戶數、庫存總量，含趨勢指示箭頭）
- [x] 實作 `components/dashboard/OrderStatusChart.tsx`（訂單狀態分佈圖表：呼叫 `useOrderStatusStats` hook，使用 recharts PieChart 或 BarChart 呈現）
- [x] 實作 `components/dashboard/PaymentMethodChart.tsx`（支付方式分佈圖表：呼叫 `usePaymentMethodStats` hook，使用 recharts PieChart 呈現）
- [x] 實作 `components/dashboard/AutoRefreshProvider.tsx`（自動重新整理：設定 `refetchInterval: 60_000` 在統計相關 hooks 中）
- [x] 使用 `next/dynamic` 動態匯入 recharts 元件，避免增加首次載入 bundle 大小
- [x] 更新 `apps/cmc/src/app/(dashboard)/page.tsx`，整合 KPIGrid + OrderStatusChart + PaymentMethodChart + AutoRefreshProvider
- [x] 統計數據載入失敗時顯示錯誤狀態 + 重試按鈕

---

## 階段六：端對端測試與品質驗證

### 任務 22：錯誤處理與載入狀態
- [x] 建立 `apps/consumer/src/app/error.tsx`（Consumer 全域錯誤邊界：顯示中文錯誤訊息 + 重試按鈕）
- [x] 建立 `apps/consumer/src/app/global-error.tsx`（Consumer 最外層錯誤邊界）
- [x] 建立 `apps/consumer/src/app/not-found.tsx`（Consumer 404 頁面）
- [x] 為 Consumer 所有路由群組建立 `loading.tsx`（products、cart、checkout、orders）
- [x] 驗證 CMC 現有的 `error.tsx`、`global-error.tsx`、`not-found.tsx` 在遷移後正常運作
- [x] 驗證 API 錯誤處理流程：401 重導向、4xx 中文錯誤訊息、5xx 通用錯誤 + 重試、網路中斷提示
- [x] 驗證所有資料表格在無資料時顯示 EmptyState 空狀態提示

### 任務 23：響應式設計驗證
- [x] 驗證 Consumer 商品列表在 375px（1 欄）、768px（2 欄）、1024px（3 欄）、1440px（4 欄）的網格佈局
- [x] 驗證 Consumer 導航列在 lg 以下收合為漢堡選單
- [x] 驗證 CMC 側邊欄在 lg 以上固定顯示、md-lg 可收合、md 以下隱藏
- [x] 驗證 Consumer 與 CMC 在所有支援斷點上不產生水平捲軸
- [x] 驗證 Consumer 首頁在行動裝置上以垂直堆疊方式排列

### 任務 24：無障礙設計驗證
- [x] 驗證所有圖片包含描述性 alt 屬性
- [x] 驗證所有表單輸入欄位關聯對應的 label 元素
- [x] 驗證色彩對比度符合 WCAG 2.1 AA 標準（使用瀏覽器開發者工具檢查）
- [x] 驗證所有互動元素支援鍵盤導航（Tab 切換焦點、Enter/Space 觸發）
- [x] 驗證語義化 HTML 標籤使用正確（nav、main、article、section、header、footer）
- [x] 驗證 `prefers-reduced-motion` 偏好設定時動畫停用
- [x] 驗證所有可點擊元素設定 `cursor: pointer`

### 任務 25：效能驗證
- [x] 使用 Lighthouse 測量 Consumer 首頁 Core Web Vitals：LCP ≤ 2.5s、FID ≤ 100ms、CLS ≤ 0.1
- [x] 驗證 Consumer 使用 `next/image` 進行圖片最佳化（WebP/AVIF 自動轉換 + 延遲載入）
- [x] 驗證 Consumer 路由級程式碼分割正常（每頁僅載入所需 JS）
- [x] 驗證 Consumer 頁面間導航使用預取機制，切換在 200ms 內完成
- [x] 驗證 CMC 儀表板在 3 秒內完成首次載入
- [x] 驗證 React Query 快取機制正常運作（重複訪問不發送多餘 API 請求）

### 任務 26：E2E 測試實作
- [x] 在 Monorepo 根層級建立 `frontend/e2e/` 目錄，安裝 `@playwright/test`
- [x] 建立 `e2e/playwright.config.ts`，設定 Consumer（port 3000）與 CMC（port 3002）兩個 project
- [x] 實作 `e2e/consumer/home.spec.ts`：首頁載入 → 精選商品顯示 → 點擊商品 → 進入詳情頁
- [x] 實作 `e2e/consumer/products.spec.ts`：搜尋商品 → 結果顯示 → 分類篩選 → 分頁
- [x] 實作 `e2e/consumer/cart.spec.ts`：加入購物車 → 數量更新 → 修改數量 → 移除商品
- [x] 實作 `e2e/consumer/checkout.spec.ts`：完整結帳流程（加入購物車 → 結帳 → 填寫地址 → 確認 → 訂單建立）
- [x] 實作 `e2e/consumer/orders.spec.ts`：訂單列表 → 訂單詳情 → 取消訂單
- [x] 實作 `e2e/cmc/dashboard.spec.ts`：儀表板載入 → 統計數據顯示 → 圖表渲染
- [x] 實作 `e2e/cmc/payments.spec.ts`：支付列表 → 詳情 → 退款操作
- [x] 驗證所有 E2E 測試通過

### 任務 27：建構與部署驗證
- [x] 建立 `frontend/Dockerfile.consumer`（多階段建構，依設計文件 10.1 節）
- [x] 建立 `frontend/Dockerfile.cmc`（多階段建構，結構同 Consumer）
- [x] 驗證 `pnpm build --filter=@repo/consumer...` 僅建構 Consumer 及其依賴
- [x] 驗證 `pnpm build --filter=@repo/cmc...` 僅建構 CMC 及其依賴
- [x] 驗證 Docker 映像建構成功（Consumer + CMC）
- [x] 驗證環境變數 `NEXT_PUBLIC_API_URL` 在 Docker 建構時正確注入
- [x] 驗證 Turborepo 增量建構正常（修改 @repo/ui 後僅重新建構依賴的 apps）
