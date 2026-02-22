# Frontend Monorepo

> 基於 Turborepo + pnpm 的前端 Monorepo，包含 CMC 管理後台與 Consumer 消費者應用。

## 架構概覽

```text
frontend/
├── apps/
│   ├── cmc/                  # CMC 管理後台 (Next.js 16 + React 19)
│   └── consumer/             # Consumer 消費者應用 (Next.js 15 + React 19)
├── packages/
│   ├── ui/                   # @repo/ui — 共用 UI 元件 (shadcn/ui + Radix UI)
│   ├── api-client/           # @repo/api-client — 共用 API 客戶端 (Axios + React Query)
│   └── config/               # @repo/config — 共用設定 (TypeScript, Tailwind, ESLint)
├── e2e/                      # Playwright E2E 測試
├── Dockerfile.cmc            # CMC Docker 多階段建置
├── Dockerfile.consumer       # Consumer Docker 多階段建置
├── turbo.json                # Turborepo 任務設定
├── pnpm-workspace.yaml       # pnpm Workspace 設定
└── package.json              # 根層級腳本與依賴
```

## 技術棧

| 類別 | 技術 | 說明 |
|------|------|------|
| **建置工具** | Turborepo 2.x + pnpm 9.x | Monorepo 管理與快取 |
| **CMC 管理後台** | Next.js 16 + React 19 + TypeScript | 管理儀表板 |
| **Consumer 應用** | Next.js 15 + React 19 + TypeScript | 消費者購物介面 |
| **UI 元件** | shadcn/ui + Radix UI + Tailwind CSS 4 | 共用元件庫 |
| **API 客戶端** | Axios + TanStack React Query 5 | API 請求與快取 |
| **圖表** | Recharts 3 | CMC 資料視覺化 |
| **表單** | React Hook Form + Zod 4 | 表單驗證 |
| **狀態管理** | Zustand 5 | 客戶端狀態 |
| **E2E 測試** | Playwright | 端對端測試 |

## 快速開始

### 前置需求

- Node.js 20+
- pnpm 9.x（透過 corepack 啟用）

```bash
# 啟用 corepack（如尚未啟用）
corepack enable
```

### 安裝依賴

```bash
cd frontend
pnpm install
```

### 開發模式

```bash
# 同時啟動所有應用
pnpm dev

# 僅啟動 CMC（http://localhost:3002）
pnpm dev --filter @repo/cmc

# 僅啟動 Consumer（http://localhost:3000）
pnpm dev --filter @repo/consumer
```

### 建置

```bash
# 建置所有套件與應用
pnpm build

# 僅建置 CMC 及其依賴
pnpm build --filter @repo/cmc...

# 僅建置 Consumer 及其依賴
pnpm build --filter @repo/consumer...
```

### 型別檢查

```bash
pnpm type-check
```

### E2E 測試

```bash
pnpm e2e              # 執行所有 E2E 測試
pnpm e2e:consumer     # 僅 Consumer 測試
pnpm e2e:cmc          # 僅 CMC 測試
```

## 共用套件

### @repo/ui

共用 UI 元件庫，基於 shadcn/ui + Radix UI 建置。提供 Button、Card、Dialog、Table 等基礎元件，供 CMC 與 Consumer 共用。

### @repo/api-client

共用 API 客戶端，封裝 Axios 實例與 TanStack React Query hooks。提供統一的後端 API 存取層，包含錯誤處理與請求快取。

### @repo/config

共用設定套件，包含 TypeScript、Tailwind CSS、ESLint 等共用設定檔，確保所有應用與套件的一致性。

## Docker 建置

### CMC 管理後台

```bash
cd frontend
docker build -f Dockerfile.cmc -t cmc-frontend .

# 執行（預設 port 3002）
docker run -p 3002:3002 cmc-frontend
```

### Consumer 應用

```bash
cd frontend
docker build -f Dockerfile.consumer -t consumer-frontend .

# 執行（預設 port 3000）
docker run -p 3000:3000 consumer-frontend
```

### 環境變數

建置時可透過 `--build-arg` 傳入 API URL：

```bash
docker build -f Dockerfile.cmc \
  --build-arg NEXT_PUBLIC_API_URL=https://api.example.com \
  -t cmc-frontend .
```

## AWS 部署

### CDK 基礎設施

前端 ECR 儲存庫由 CDK `FrontendStack` 管理（`infrastructure/src/stacks/frontend-stack.ts`），包含：

- `genai-demo/consumer-frontend` — Consumer ECR Repository
- `genai-demo/cmc-frontend` — CMC ECR Repository
- 映像掃描（imageScanOnPush）、生命週期策略（保留 20 個映像）
- EKS 節點拉取權限

部署 ECR 儲存庫：

```bash
cd infrastructure
npx cdk deploy '*-FrontendStack' -c environment=development
```

### 一鍵部署腳本

專案根目錄提供 `deploy-frontend.sh`，整合建置、推送、部署流程：

```bash
# 部署兩個前端至開發環境
./deploy-frontend.sh -e development

# 僅部署 CMC 至 Staging
./deploy-frontend.sh -e staging -a cmc

# 部署至 Production（指定映像標籤）
./deploy-frontend.sh -e production -t v1.2.0

# 預覽部署指令（不實際執行）
./deploy-frontend.sh -e production --dry-run

# 僅建置映像，不推送不部署
./deploy-frontend.sh --skip-push --skip-deploy
```

#### 部署腳本選項

| 選項 | 說明 | 預設值 |
|------|------|--------|
| `-e, --env` | 部署環境 | `development` |
| `-a, --app` | 部署目標 (`all`/`consumer`/`cmc`) | `all` |
| `-t, --tag` | 映像標籤 | git short SHA |
| `-r, --region` | AWS 區域 | `ap-northeast-1` |
| `--api-url` | 後端 API URL | 依環境自動決定 |
| `--skip-build` | 跳過 Docker 建置 | - |
| `--skip-push` | 跳過 ECR 推送 | - |
| `--skip-deploy` | 跳過 K8s 部署 | - |
| `--dry-run` | 僅顯示指令 | - |

#### 環境對應 URL

| 環境 | Consumer | CMC | API |
|------|----------|-----|-----|
| development | shop.dev.kimkao.io | cmc.dev.kimkao.io | api.dev.kimkao.io |
| staging | shop.staging.kimkao.io | cmc.staging.kimkao.io | api.staging.kimkao.io |
| production | shop.kimkao.io | cmc.kimkao.io | api.kimkao.io |

### K8s Canary 部署

前端使用 Argo Rollouts 進行 Canary 部署，配置檔位於：

- `infrastructure/k8s/rollouts/consumer-frontend-canary.yaml`
- `infrastructure/k8s/rollouts/cmc-frontend-canary.yaml`

Canary 步驟：10% → 25% → 50% → 75% → 100%，每步驟含 Prometheus 分析（成功率 ≥ 95%、p95 回應時間 ≤ 1s）。

## 開發指南

### 新增共用元件

1. 在 `packages/ui/src/components/` 新增元件
2. 從 `packages/ui/src/index.ts` 匯出
3. 在應用中透過 `@repo/ui` 引入

### 新增 API Hook

1. 在 `packages/api-client/src/` 新增 service 或 hook
2. 從 `packages/api-client/src/index.ts` 匯出
3. 在應用中透過 `@repo/api-client` 引入

### Turborepo 快取

Turborepo 會自動快取建置產物。如需清除快取：

```bash
# 清除 Turborepo 快取
pnpm turbo clean
```

---

**最後更新**: 2026-02-21
