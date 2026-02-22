# 需求文件

## 簡介

本文件定義企業級電商平台前端全面重新設計的需求。目標是將現有的雙前端架構（CMC 管理後台使用 Next.js、消費者前台使用 Angular）統一為基於 Turborepo + pnpm 的 Monorepo 架構，兩個應用程式均採用 Next.js 15 + React 19，共享元件庫與 API 客戶端。

## 術語表

- **Monorepo**：單一程式碼倉庫中管理多個專案的架構模式
- **Turborepo**：高效能的 JavaScript/TypeScript Monorepo 建構系統
- **pnpm**：高效的 Node.js 套件管理器，支援 workspace
- **CMC_Frontend**：後台管理系統（Content Management Console），供管理員使用
- **Consumer_Frontend**：消費者前台應用程式，供終端客戶使用
- **Shared_UI**：共享 UI 元件套件（`@repo/ui`），基於 shadcn/ui
- **Shared_API_Client**：共享 API 客戶端套件（`@repo/api-client`）
- **Shared_Config**：共享設定套件（`@repo/config`），包含 Tailwind 與 TypeScript 設定
- **Design_System**：設計系統，定義色彩、字型、間距等視覺規範
- **Backend_API**：Spring Boot 3.4.5 + Java 21 後端 REST API
- **shadcn/ui**：基於 Radix UI 的可自訂元件庫
- **Tailwind_CSS**：功能優先的 CSS 框架
- **React_Query**：伺服器狀態管理庫（@tanstack/react-query）
- **Zustand**：輕量級 React 狀態管理庫
- **Domain_Types**：對應後端領域模型的 TypeScript 型別定義

---

## 需求

### 需求 1：Monorepo 架構建立

**使用者故事：** 身為開發者，我希望在單一 Monorepo 中管理所有前端專案，以便共享程式碼、統一建構流程並減少重複工作。

#### 驗收標準

1. THE Monorepo SHALL 使用 Turborepo 作為建構編排工具，pnpm 作為套件管理器
2. THE Monorepo SHALL 包含以下 workspace 結構：`apps/cmc`（管理後台）、`apps/consumer`（消費者前台）、`packages/ui`（共享 UI）、`packages/api-client`（共享 API 客戶端）、`packages/config`（共享設定）
3. WHEN 執行 `pnpm build` 時，THE Turborepo SHALL 根據套件依賴關係正確排序建構順序，先建構 packages 再建構 apps
4. WHEN 修改 `packages/ui` 中的元件時，THE Turborepo SHALL 自動重新建構所有依賴該套件的應用程式
5. THE Monorepo SHALL 在根目錄提供統一的 `pnpm dev`、`pnpm build`、`pnpm lint`、`pnpm test` 指令
6. THE Shared_Config SHALL 匯出共享的 TypeScript 設定（`tsconfig.json`）和 Tailwind CSS 設定供所有套件與應用程式使用
7. IF 套件之間存在循環依賴，THEN THE Turborepo SHALL 在建構時報告錯誤並中止建構

</text>
</invoke>

---

### 需求 2：共享 UI 元件庫（@repo/ui）

**使用者故事：** 身為開發者，我希望有一套共享的 UI 元件庫，以便 CMC 與 Consumer 兩個應用程式使用一致的基礎元件，減少重複開發。

#### 驗收標準

1. THE Shared_UI SHALL 基於 shadcn/ui + Radix UI 建構，取代 Consumer_Frontend 現有的 PrimeNG 元件
2. THE Shared_UI SHALL 匯出所有基礎元件，包含但不限於：Button、Input、Select、Dialog、Table、Card、Badge、Tabs、Toast、Dropdown Menu、Sheet、Skeleton
3. THE Shared_UI SHALL 支援透過 Tailwind CSS 變數進行主題客製化，使 CMC 與 Consumer 可套用不同的色彩方案
4. WHEN CMC_Frontend 匯入 Shared_UI 元件時，THE 元件 SHALL 自動套用 CMC 的 Design_System 色彩與間距
5. WHEN Consumer_Frontend 匯入 Shared_UI 元件時，THE 元件 SHALL 自動套用 Consumer 的 Design_System 色彩與間距
6. THE Shared_UI SHALL 將現有 `cmc-frontend/src/components/ui/` 中的 shadcn/ui 元件遷移至共享套件中
7. THE Shared_UI 中的所有互動元件 SHALL 支援鍵盤導航與 ARIA 屬性

---

### 需求 3：共享 API 客戶端（@repo/api-client）

**使用者故事：** 身為開發者，我希望有一個共享的 API 客戶端，以便統一管理與後端的通訊邏輯，避免在兩個應用程式中重複實作。

#### 驗收標準

1. THE Shared_API_Client SHALL 基於現有 `cmc-frontend/src/services/api.ts` 的 Axios 客戶端進行重構，保留請求攔截器與響應攔截器邏輯
2. THE Shared_API_Client SHALL 匯出所有 Domain_Types，基於現有 `cmc-frontend/src/types/domain.ts` 中定義的型別（Money、Order、Product、Customer、Payment、Inventory、Promotion 等）
3. THE Shared_API_Client SHALL 匯出所有服務方法，涵蓋後端 10 個 REST Controller 的完整 API 端點：Customer API（`/api/customers`）、Product API（`/api/products`）、Consumer Product API（`/api/consumer/products`）、Order API（`/api/orders`）、Payment API（`/api/payments`）、Shopping Cart API（`/api/consumer/cart`）、Inventory API（`/api/inventory`）、Statistics API（`/api/stats`）
4. THE Shared_API_Client SHALL 匯出 React Query hooks（基於現有 `cmc-frontend/src/hooks/useApi.ts`），供兩個應用程式直接使用
5. WHEN API 請求失敗且 HTTP 狀態碼為 401 時，THE Shared_API_Client SHALL 清除認證 token 並重導向至登入頁面
6. THE Shared_API_Client SHALL 支援設定不同的 `baseURL`，使 CMC 與 Consumer 可連接不同的 API 端點
7. FOR ALL API 請求與響應型別，序列化後再反序列化 SHALL 產生等價的物件（round-trip 屬性）

---

### 需求 4：Consumer 設計系統

**使用者故事：** 身為設計師，我希望 Consumer 前台有一套明確的設計系統，以便建立一致且專業的消費者購物體驗。

#### 驗收標準

1. THE Consumer_Frontend Design_System SHALL 使用以下色彩方案：主色 Indigo 600（`#4F46E5`）、強調色 Amber 500（`#F59E0B`）、中性色系使用 Stone 色階
2. THE Consumer_Frontend Design_System SHALL 採用「Soft Modern」風格：大圓角（12-16px）、柔和陰影、充裕的留白空間
3. THE Consumer_Frontend Design_System SHALL 使用以下字型組合：Geist Sans 用於標題、Inter 用於內文、Geist Mono 用於程式碼與數據顯示
4. THE Consumer_Frontend SHALL 僅提供淺色模式，不實作深色模式
5. THE Consumer_Frontend SHALL 僅支援繁體中文介面，不實作國際化（i18n）功能
6. THE Consumer_Frontend Design_System SHALL 透過 Tailwind CSS 的 CSS 變數定義所有設計 token（色彩、間距、圓角、陰影），使主題可在 `tailwind.config.ts` 中集中管理
7. WHILE 使用者在 Consumer_Frontend 瀏覽時，THE 頁面 SHALL 保持視覺一致性，所有頁面使用相同的色彩方案、字型與間距規範

---

### 需求 5：CMC 設計系統

**使用者故事：** 身為設計師，我希望 CMC 管理後台有一套功能導向的設計系統，以便管理員高效地處理大量數據與操作。

#### 驗收標準

1. THE CMC_Frontend Design_System SHALL 使用以下色彩方案：主色 Blue 600（`#2563EB`）、中性色系使用 Slate 色階
2. THE CMC_Frontend Design_System SHALL 採用「Functional Minimal」風格：緊湊間距、高數據密度、較小圓角（6-8px）
3. THE CMC_Frontend Design_System SHALL 使用與 Consumer 相同的字型組合：Geist Sans（標題）、Inter（內文）、Geist Mono（程式碼與數據）
4. THE CMC_Frontend SHALL 保留深色模式支援
5. THE CMC_Frontend Design_System SHALL 透過 Tailwind CSS 的 CSS 變數定義所有設計 token
6. WHEN 管理員切換深色模式時，THE CMC_Frontend SHALL 平滑過渡所有色彩，不產生閃爍
7. THE CMC_Frontend SHALL 將現有 `cmc-frontend/` 中的元件與頁面遷移至 Monorepo 的 `apps/cmc` 目錄，保留現有功能

---

### 需求 6：Consumer 前台重建 — 核心佈局與導航

**使用者故事：** 身為消費者，我希望有一個清晰的網站佈局與導航結構，以便輕鬆找到商品並完成購物。

#### 驗收標準

1. THE Consumer_Frontend SHALL 使用 Next.js 15 App Router 建構，取代現有的 Angular 20 應用程式
2. THE Consumer_Frontend SHALL 包含以下核心佈局元素：頂部導航列（Logo、搜尋列、購物車圖示、使用者選單）、頁尾（網站連結、聯絡資訊）
3. THE Consumer_Frontend 導航列 SHALL 包含以下主要連結：首頁、商品分類、購物車、我的訂單
4. WHEN 使用者在行動裝置上瀏覽時，THE 導航列 SHALL 收合為漢堡選單，提供完整的導航功能
5. THE Consumer_Frontend SHALL 使用 Next.js 15 的 Server Components 作為預設渲染策略，僅在需要互動的元件使用 Client Components
6. THE Consumer_Frontend SHALL 使用 Zustand 管理客戶端全域狀態（購物車、使用者認證狀態）
7. THE Consumer_Frontend SHALL 使用 React_Query 管理所有伺服器狀態（商品列表、訂單資料等）

---

### 需求 7：Consumer 前台 — 商品瀏覽與搜尋

**使用者故事：** 身為消費者，我希望能瀏覽和搜尋商品，以便找到我想購買的商品。

#### 驗收標準

1. THE Consumer_Frontend SHALL 提供商品列表頁面，呼叫 `GET /api/consumer/products` 端點取得商品資料
2. THE Consumer_Frontend SHALL 提供商品搜尋功能，呼叫 `GET /api/consumer/products/search` 端點
3. THE Consumer_Frontend SHALL 提供商品分類瀏覽功能，呼叫 `GET /api/consumer/products/categories` 端點取得分類列表
4. THE Consumer_Frontend SHALL 提供商品詳情頁面，顯示商品名稱、描述、價格、庫存狀態與圖片
5. WHEN 商品列表超過一頁時，THE Consumer_Frontend SHALL 實作分頁功能，使用 PageRequest 與 PageResponse 型別
6. WHEN 使用者輸入搜尋關鍵字時，THE Consumer_Frontend SHALL 在使用者停止輸入 300 毫秒後自動發送搜尋請求（防抖處理）
7. WHILE 商品資料正在載入時，THE Consumer_Frontend SHALL 顯示 Skeleton 載入佔位元件

---

### 需求 8：Consumer 前台 — 購物車功能

**使用者故事：** 身為消費者，我希望能將商品加入購物車並管理購物車內容，以便準備結帳。

#### 驗收標準

1. THE Consumer_Frontend SHALL 提供購物車頁面，呼叫 `GET /api/consumer/cart/{customerId}/items` 端點取得購物車內容
2. WHEN 使用者點擊「加入購物車」按鈕時，THE Consumer_Frontend SHALL 呼叫 `POST /api/consumer/cart/{customerId}/items` 端點新增商品
3. WHEN 使用者修改購物車商品數量時，THE Consumer_Frontend SHALL 呼叫 `PUT /api/consumer/cart/{customerId}/items` 端點更新數量
4. WHEN 使用者點擊「移除」按鈕時，THE Consumer_Frontend SHALL 呼叫 `DELETE /api/consumer/cart/{customerId}/items` 端點移除商品
5. THE Consumer_Frontend SHALL 在導航列的購物車圖示上顯示購物車商品數量徽章
6. THE Consumer_Frontend SHALL 使用 Zustand 在客戶端維護購物車狀態的本地快取，並與伺服器端資料同步
7. WHEN 加入購物車成功時，THE Consumer_Frontend SHALL 顯示 Toast 通知確認操作成功
8. THE Consumer_Frontend 購物車頁面 SHALL 顯示每項商品的小計金額與購物車總金額，金額計算使用 Money 型別

---

### 需求 9：Consumer 前台 — 結帳與訂單

**使用者故事：** 身為消費者，我希望能完成結帳流程並查看我的訂單，以便追蹤購買狀態。

#### 驗收標準

1. THE Consumer_Frontend SHALL 提供結帳頁面，包含收貨地址輸入、訂單摘要與確認按鈕
2. WHEN 使用者確認結帳時，THE Consumer_Frontend SHALL 依序呼叫：`POST /api/orders`（建立訂單）、`POST /api/orders/{id}/items`（新增訂單項目）、`POST /api/orders/{id}/submit`（提交訂單）
3. THE Consumer_Frontend SHALL 提供「我的訂單」頁面，呼叫 `GET /api/orders` 端點取得使用者的訂單列表
4. THE Consumer_Frontend SHALL 提供訂單詳情頁面，顯示訂單狀態（CREATED、PENDING、CONFIRMED、SHIPPED、DELIVERED、CANCELLED）、商品明細與金額
5. WHEN 訂單狀態為 CREATED 或 PENDING 時，THE Consumer_Frontend SHALL 顯示「取消訂單」按鈕，呼叫 `POST /api/orders/{id}/cancel` 端點
6. IF 結帳過程中任何 API 呼叫失敗，THEN THE Consumer_Frontend SHALL 顯示明確的錯誤訊息並允許使用者重試
7. WHEN 訂單建立成功時，THE Consumer_Frontend SHALL 清空購物車並導航至訂單確認頁面

---

### 需求 10：CMC 管理後台 — 遷移至 Monorepo

**使用者故事：** 身為管理員，我希望 CMC 管理後台遷移至新的 Monorepo 架構後，所有現有功能保持正常運作，同時套用新的設計系統。

#### 驗收標準

1. THE CMC_Frontend SHALL 從現有 `cmc-frontend/` 目錄遷移至 Monorepo 的 `apps/cmc` 目錄，保留所有現有頁面與功能
2. THE CMC_Frontend SHALL 將現有 `src/components/ui/` 中的 shadcn/ui 元件替換為 Shared_UI 套件（`@repo/ui`）的匯入
3. THE CMC_Frontend SHALL 將現有 `src/services/api.ts` 與 `src/types/domain.ts` 替換為 Shared_API_Client 套件（`@repo/api-client`）的匯入
4. THE CMC_Frontend SHALL 將現有 `src/hooks/useApi.ts` 替換為 Shared_API_Client 匯出的 React Query hooks
5. THE CMC_Frontend SHALL 保留現有的儀表板頁面（Dashboard）、訂單管理、商品管理、客戶管理、庫存管理功能
6. THE CMC_Frontend SHALL 保留現有的側邊欄導航（基於 `src/components/layout/Sidebar.tsx`）與儀表板佈局（基於 `src/components/layout/DashboardLayout.tsx`）
7. WHEN 遷移完成後，THE CMC_Frontend 的所有現有功能 SHALL 通過手動驗證測試，確認功能與遷移前一致
8. THE CMC_Frontend SHALL 更新色彩方案為 Blue 600 主色 + Slate 中性色系，套用「Functional Minimal」風格

---

### 需求 11：CMC 管理後台 — 支付管理頁面

**使用者故事：** 身為管理員，我希望能在 CMC 中管理支付記錄，以便追蹤訂單的付款狀態與處理退款。

#### 驗收標準

1. THE CMC_Frontend SHALL 提供支付列表頁面，呼叫 `GET /api/payments` 端點取得支付記錄
2. THE CMC_Frontend 支付列表 SHALL 以表格形式顯示：支付 ID、訂單 ID、金額、支付方式、狀態（PENDING、PROCESSING、COMPLETED、FAILED、CANCELLED）、建立時間
3. WHEN 管理員點擊支付記錄時，THE CMC_Frontend SHALL 顯示支付詳情，包含關聯的訂單資訊
4. THE CMC_Frontend SHALL 提供退款功能，呼叫 `POST /api/payments/refund` 端點
5. THE CMC_Frontend SHALL 提供取消支付功能，呼叫 `POST /api/payments/cancel` 端點
6. WHEN 退款或取消操作成功時，THE CMC_Frontend SHALL 更新支付狀態並顯示成功通知
7. IF 退款或取消操作失敗，THEN THE CMC_Frontend SHALL 顯示具體的錯誤訊息

---

### 需求 12：CMC 管理後台 — 數據分析儀表板

**使用者故事：** 身為管理員，我希望能查看業務數據分析，以便了解營運狀況並做出決策。

#### 驗收標準

1. THE CMC_Frontend 儀表板 SHALL 呼叫 `GET /api/stats` 端點取得總覽統計數據（總訂單數、總營收、總客戶數等）
2. THE CMC_Frontend 儀表板 SHALL 呼叫 `GET /api/stats/order-status` 端點取得訂單狀態分佈數據，並以圖表形式呈現
3. THE CMC_Frontend 儀表板 SHALL 呼叫 `GET /api/stats/payment-methods` 端點取得支付方式分佈數據，並以圖表形式呈現
4. THE CMC_Frontend 儀表板 SHALL 使用數據卡片顯示關鍵指標（KPI），包含數值與趨勢指示
5. WHEN 統計數據載入失敗時，THE CMC_Frontend SHALL 顯示錯誤狀態並提供重試按鈕
6. THE CMC_Frontend 儀表板 SHALL 支援自動重新整理，每 60 秒重新取得最新數據

---

### 需求 13：響應式設計

**使用者故事：** 身為使用者，我希望在不同裝置上都能正常使用應用程式，以便在桌面電腦、平板與手機上都有良好的體驗。

#### 驗收標準

1. THE Consumer_Frontend SHALL 支援以下斷點的響應式佈局：手機（375px）、平板（768px）、桌面（1024px）、大螢幕（1440px）
2. THE CMC_Frontend SHALL 支援以下斷點的響應式佈局：平板（768px）、桌面（1024px）、大螢幕（1440px）
3. WHILE 使用者在手機上瀏覽 Consumer_Frontend 時，THE 商品列表 SHALL 以單欄佈局顯示
4. WHILE 使用者在平板上瀏覽 Consumer_Frontend 時，THE 商品列表 SHALL 以雙欄佈局顯示
5. WHILE 使用者在桌面上瀏覽 Consumer_Frontend 時，THE 商品列表 SHALL 以三欄或四欄佈局顯示
6. THE Consumer_Frontend 與 CMC_Frontend SHALL 在所有支援的斷點上不產生水平捲軸
7. THE Consumer_Frontend 與 CMC_Frontend SHALL 使用 Tailwind_CSS 的響應式工具類別實作所有斷點的佈局調整

---

### 需求 14：前端效能

**使用者故事：** 身為使用者，我希望頁面載入快速且操作流暢，以便有良好的使用體驗。

#### 驗收標準

1. THE Consumer_Frontend SHALL 達到以下 Core Web Vitals 指標：Largest Contentful Paint（LCP）≤ 2.5 秒、First Input Delay（FID）≤ 100 毫秒、Cumulative Layout Shift（CLS）≤ 0.1
2. THE Consumer_Frontend SHALL 使用 Next.js 15 的 Image 元件進行圖片最佳化，包含自動格式轉換（WebP/AVIF）與延遲載入
3. THE Consumer_Frontend SHALL 使用 Next.js 15 的路由級程式碼分割，確保每個頁面僅載入所需的 JavaScript
4. THE Consumer_Frontend 商品列表頁面 SHALL 使用 React_Query 的快取機制，避免重複的 API 請求
5. WHEN 使用者在頁面間導航時，THE Consumer_Frontend SHALL 使用 Next.js 15 的預取（prefetch）機制，使頁面切換在 200 毫秒內完成
6. THE CMC_Frontend 儀表板頁面 SHALL 在 3 秒內完成首次載入（包含所有統計數據）
7. THE Shared_API_Client 中的 React_Query hooks SHALL 設定適當的 staleTime 與 cacheTime，減少不必要的網路請求

---

### 需求 15：無障礙設計

**使用者故事：** 身為使用者，我希望應用程式符合基本的無障礙標準，以便所有人都能使用。

#### 驗收標準

1. THE Consumer_Frontend 與 CMC_Frontend 中的所有圖片 SHALL 包含描述性的 alt 屬性
2. THE Consumer_Frontend 與 CMC_Frontend 中的所有表單輸入欄位 SHALL 關聯對應的 label 元素
3. THE Consumer_Frontend 與 CMC_Frontend SHALL 確保色彩對比度符合 WCAG 2.1 AA 標準（正常文字 4.5:1、大文字 3:1）
4. THE Consumer_Frontend 與 CMC_Frontend 中的所有互動元素 SHALL 支援鍵盤導航（Tab 鍵切換焦點、Enter/Space 鍵觸發操作）
5. THE Consumer_Frontend 與 CMC_Frontend SHALL 使用語義化 HTML 標籤（`<nav>`、`<main>`、`<article>`、`<section>`、`<header>`、`<footer>`）
6. WHEN 使用者設定了 `prefers-reduced-motion` 偏好時，THE Consumer_Frontend 與 CMC_Frontend SHALL 減少或停用動畫效果
7. THE Consumer_Frontend 與 CMC_Frontend 中的所有可點擊元素 SHALL 設定 `cursor: pointer` 樣式

---

### 需求 16：錯誤處理與載入狀態

**使用者故事：** 身為使用者，我希望在操作失敗或資料載入時有清楚的回饋，以便了解系統狀態。

#### 驗收標準

1. THE Consumer_Frontend 與 CMC_Frontend SHALL 使用 Next.js 15 的 `error.tsx` 檔案實作頁面級錯誤邊界
2. THE Consumer_Frontend 與 CMC_Frontend SHALL 使用 Next.js 15 的 `loading.tsx` 檔案實作頁面級載入狀態
3. WHEN API 請求返回 4xx 錯誤時，THE 應用程式 SHALL 顯示使用者可理解的中文錯誤訊息
4. WHEN API 請求返回 5xx 錯誤時，THE 應用程式 SHALL 顯示通用的「系統錯誤」訊息並提供重試選項
5. WHEN 網路連線中斷時，THE 應用程式 SHALL 顯示離線提示訊息
6. IF 頁面渲染過程中發生未預期的錯誤，THEN THE Next.js 15 的 `global-error.tsx` SHALL 捕獲錯誤並顯示降級的錯誤頁面
7. THE Consumer_Frontend 與 CMC_Frontend 中的所有資料表格 SHALL 在無資料時顯示「暫無資料」的空狀態提示

---

### 需求 17：Consumer 前台 — 首頁

**使用者故事：** 身為消費者，我希望首頁能展示精選商品與促銷活動，以便快速發現感興趣的商品。

#### 驗收標準

1. THE Consumer_Frontend 首頁 SHALL 包含以下區塊：Hero 橫幅區、精選商品區、商品分類區、促銷活動區
2. THE Consumer_Frontend 首頁精選商品區 SHALL 呼叫 `GET /api/consumer/products` 端點取得商品並以卡片形式展示
3. THE Consumer_Frontend 首頁促銷活動區 SHALL 呼叫促銷相關 API 取得進行中的促銷活動
4. THE Consumer_Frontend 首頁 SHALL 使用 Next.js 15 的 Server Components 進行伺服器端渲染，提升首次載入效能
5. WHEN 使用者點擊商品卡片時，THE Consumer_Frontend SHALL 導航至該商品的詳情頁面
6. THE Consumer_Frontend 首頁 SHALL 取代現有 Angular Consumer_Frontend 中 2000 行的 home component
7. THE Consumer_Frontend 首頁在行動裝置上 SHALL 以垂直堆疊方式排列各區塊

---

### 需求 18：技術棧統一與遷移

**使用者故事：** 身為開發者，我希望兩個前端應用程式使用統一的技術棧，以便降低維護成本並提升開發效率。

#### 驗收標準

1. THE Consumer_Frontend SHALL 使用 Next.js 15 + React 19 建構，取代現有的 Angular 20 應用程式
2. THE Consumer_Frontend SHALL 使用 shadcn/ui + Radix UI 作為元件庫，取代現有的 PrimeNG 20
3. THE Consumer_Frontend SHALL 使用 Tailwind_CSS 4 作為樣式方案，取代現有的 SCSS design tokens
4. THE CMC_Frontend 與 Consumer_Frontend SHALL 使用相同版本的 Next.js、React、TypeScript、Tailwind_CSS
5. THE Monorepo SHALL 在 `packages/config` 中定義統一的 TypeScript 編譯設定（strict mode 啟用、target ES2022）
6. THE Monorepo SHALL 在 `packages/config` 中定義統一的 ESLint 設定，確保兩個應用程式遵循相同的程式碼風格
7. WHEN 新增共享依賴時，THE Monorepo SHALL 在根目錄的 `package.json` 中管理，避免版本不一致

---

### 需求 19：實作階段規劃

**使用者故事：** 身為專案經理，我希望有明確的實作階段規劃，以便追蹤進度並管理風險。

#### 驗收標準

1. THE 實作計畫 SHALL 分為六個階段依序執行：階段一（Monorepo 建立 + 共享套件）、階段二（CMC 遷移）、階段三（Consumer 核心框架）、階段四（Consumer 電商頁面）、階段五（CMC 補充頁面）、階段六（端對端測試）
2. WHEN 階段一完成時，THE Monorepo SHALL 能成功執行 `pnpm build` 建構所有套件與應用程式
3. WHEN 階段二完成時，THE CMC_Frontend SHALL 在 Monorepo 中正常運作，所有現有功能保持一致
4. WHEN 階段三完成時，THE Consumer_Frontend SHALL 具備核心佈局、導航與首頁功能
5. WHEN 階段四完成時，THE Consumer_Frontend SHALL 具備完整的商品瀏覽、購物車、結帳與訂單功能
6. WHEN 階段五完成時，THE CMC_Frontend SHALL 具備支付管理、物流管理、促銷管理與數據分析頁面
7. WHEN 階段六完成時，THE 專案 SHALL 具備涵蓋核心使用者流程的端對端測試計畫與實作

---

### 需求 20：建構與部署

**使用者故事：** 身為 DevOps 工程師，我希望 Monorepo 有清晰的建構與部署流程，以便自動化 CI/CD 管線。

#### 驗收標準

1. THE Monorepo SHALL 在根目錄提供 `Dockerfile`，支援分別建構 CMC_Frontend 與 Consumer_Frontend 的 Docker 映像
2. THE Turborepo SHALL 支援增量建構，僅重新建構有變更的套件與應用程式
3. THE Monorepo SHALL 提供環境變數設定機制，使 CMC_Frontend 與 Consumer_Frontend 可設定不同的 `NEXT_PUBLIC_API_URL`
4. WHEN 執行 `pnpm build --filter=@repo/cmc` 時，THE Turborepo SHALL 僅建構 CMC_Frontend 及其依賴的共享套件
5. WHEN 執行 `pnpm build --filter=@repo/consumer` 時，THE Turborepo SHALL 僅建構 Consumer_Frontend 及其依賴的共享套件
6. THE Monorepo 的 `pnpm lint` 指令 SHALL 對所有套件與應用程式執行 ESLint 檢查
7. THE Monorepo 的 `pnpm test` 指令 SHALL 對所有套件與應用程式執行單元測試
