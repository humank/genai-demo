# Documentation Changelog - February 2026

## 2026-02-21 - 前端 Monorepo 遷移與文件更新

### 🎯 重大變更

#### 前端架構遷移

原本分散的前端應用（`cmc-frontend/` 使用 Next.js 14 + React 18、`consumer-frontend/` 使用 Angular 20）已整合遷移至統一的 Turborepo + pnpm Monorepo 架構，位於 `frontend/` 目錄。舊版目錄已於 2026-03-16 從 repo 中移除。

**遷移前**:
- `cmc-frontend/` — Next.js 14 + React 18（已移除）
- `consumer-frontend/` — Angular 20 + PrimeNG（已移除）

**遷移後**:
- `frontend/apps/cmc/` — Next.js 16 + React 19
- `frontend/apps/consumer/` — Next.js 15 + React 19
- `frontend/packages/ui/` — @repo/ui 共用 UI 元件
- `frontend/packages/api-client/` — @repo/api-client 共用 API 客戶端
- `frontend/packages/config/` — @repo/config 共用設定
- `frontend/e2e/` — Playwright E2E 測試

#### React 19 升級

CMC 管理後台從 React 18 升級至 React 19，修復所有相關型別不相容問題：
- `@types/react` / `@types/react-dom` 升級至 ^19.0.0
- 修復 `toast` API 從 react-hot-toast 遷移至 shadcn toast
- 修復 `StatsOverview` index signature 型別問題
- 修復 recharts formatter 型別
- 修復 JSX namespace 相關錯誤

### 📝 文件變更

#### 新增文件

| 文件 | 說明 |
|------|------|
| `frontend/README.md` | 前端 Monorepo 完整文件（架構、技術棧、快速開始、開發指南） |
| `docs/CHANGELOG-2026-02.md` | 本月變更日誌 |

#### 更新文件

| 文件 | 變更內容 |
|------|----------|
| `README.md` | 更新技術棧（React 19、Turborepo）、新增前端 Monorepo 專案結構、更新 Quick Start 加入前端啟動步驟 |
| `CONTRIBUTING.md` | 新增前端開發指南、更新前置需求（Node.js 20+、pnpm 9.x）、新增共用元件開發流程 |
| `docker-compose.yml` | 新增 CMC (port 3002) 與 Consumer (port 3000) 前端服務 |
| `deployment/README.md` | 新增前端 Docker 建置與部署說明 |
| `docs/README.md` | 更新技術棧、新增前端 Monorepo 章節 |

### 🔧 技術修復

#### Docker 建置修復
- `frontend/Dockerfile.cmc`: 移除 COPY 指令中無效的 `2>/dev/null || true`
- 建立 `frontend/apps/consumer/public/.gitkeep` 確保 Consumer Dockerfile COPY 不會失敗
- 驗證兩個 Docker 映像皆可成功建置

#### 型別錯誤修復（共 6 個檔案）
- `toast.success()` / `toast.error()` → `toast({ title, variant })` shadcn API
- `Activity[]` → `ActivityItem[]` 型別映射
- `StatsOverview` 欄位名稱修正（`uniqueCustomers` → `totalCustomers` 等）
- recharts `formatter` 型別修正
- `EditProductDialog` mutation 參數名稱修正

### 📊 影響範圍

| 類別 | 變更前 | 變更後 |
|------|--------|--------|
| 前端框架 | React 18 + Angular 18 | React 19 (統一) |
| 建置工具 | 各自獨立 npm | Turborepo + pnpm Monorepo |
| 共用程式碼 | 無 | @repo/ui, @repo/api-client, @repo/config |
| Docker 服務 | 僅後端 | 後端 + CMC + Consumer |
| E2E 測試 | 無 | Playwright |

### 🔗 相關文件

- [frontend/README.md](../frontend/README.md) — 前端 Monorepo 完整文件
- [deployment/README.md](../deployment/README.md) — 部署指南（含前端 Docker）

---

## 2026-02-21 - 前端 CDK FrontendStack 與部署腳本

### 🎯 變更摘要

新增 CDK FrontendStack 管理前端 ECR 容器倉庫，並建立一鍵部署腳本 `deploy-frontend.sh`，支援多環境、多應用目標、Argo Rollouts Canary 部署。

### 📝 新增文件

| 文件 | 說明 |
|------|------|
| `infrastructure/src/stacks/frontend-stack.ts` | CDK FrontendStack — 管理 Consumer 與 CMC 前端 ECR 倉庫，含映像掃描、生命週期策略（保留 20 個映像）、EKS 拉取權限、生產環境 RETAIN 保護 |
| `deploy-frontend.sh` | 前端一鍵部署腳本 — 支援環境選擇（-e dev/staging/prod）、應用目標（-a all/consumer/cmc）、映像標籤（-t）、dry-run、skip-build/push/deploy 等選項 |

### 📝 更新文件

| 文件 | 變更內容 |
|------|----------|
| `infrastructure/src/stacks/index.ts` | 匯出 FrontendStack 與 FrontendStackProps |
| `infrastructure/bin/infrastructure.ts` | 匯入 FrontendStack，以 NetworkStack 為依賴進行實例化 |
| `README.md` | CDK Stacks 18→19、新增 deploy-frontend.sh 至專案結構、新增前端部署指令 |
| `frontend/README.md` | 新增「AWS 部署」章節（CDK 基礎設施、一鍵部署腳本、選項表、環境 URL 對照、K8s Canary 部署說明） |
| `deployment/README.md` | 新增前端一鍵部署腳本章節與 CDK FrontendStack 章節 |
| `CONTRIBUTING.md` | 新增前端部署章節（deploy-frontend.sh 使用方式與 CDK 前置需求） |
| `docs/viewpoints/operational/deployment/deployment-process.md` | 新增一鍵部署腳本章節與前置需求 |
| `docs/CHANGELOG-2026-02.md` | 本條目 |

### 📊 影響範圍

| 類別 | 變更前 | 變更後 |
|------|--------|--------|
| CDK Stacks | 18 個 | 19 個（新增 FrontendStack） |
| ECR 管理 | 手動建立 | CDK 自動管理（含掃描、生命週期、權限） |
| 前端部署 | 手動 Docker build/push/kubectl | `deploy-frontend.sh` 一鍵部署 |
| 部署策略 | 手動 kubectl apply | Argo Rollouts Canary 自動觸發 |

### 🔗 相關文件

- [infrastructure/src/stacks/frontend-stack.ts](../infrastructure/src/stacks/frontend-stack.ts) — CDK FrontendStack
- [deploy-frontend.sh](../deploy-frontend.sh) — 一鍵部署腳本
- [frontend/README.md](../frontend/README.md) — 前端 Monorepo 文件（含 AWS 部署章節）

---

## 2026-02-21 - CDK/K8s 部署基礎設施更新

### 🎯 變更摘要

配合前端 Monorepo 遷移，更新所有 CDK、K8s、部署相關文件，確保基礎設施配置與新架構一致。

### 📝 K8s Rollout 更新

| 文件 | 變更內容 |
|------|----------|
| `infrastructure/k8s/rollouts/consumer-frontend-canary.yaml` | containerPort 4200→3000、env `API_URL`→`NEXT_PUBLIC_API_URL`、新增 `PORT`/`NEXT_PUBLIC_APP_ENV` 環境變數、health check 路徑 `/health`→`/api/health` |
| `infrastructure/k8s/rollouts/cmc-frontend-canary.yaml` | containerPort 3000→3002、PORT 環境變數 3000→3002 |

### 📝 CDK 基礎設施更新

| 文件 | 變更內容 |
|------|----------|
| `infrastructure/src/stacks/observability-stack.ts` | Consumer RUM tag `Framework: 'Angular'`→`'NextJS'`、註解更新 Angular→Next.js 15、CMC 註解 Next.js→Next.js 16、Dashboard 文字更新 |

### 📝 部署文件更新

| 文件 | 變更內容 |
|------|----------|
| `docs/viewpoints/operational/deployment/deployment-process.md` | 前端部署章節全面改寫：S3+CloudFront→Docker+EKS Canary 部署、新增 Monorepo 建置流程、新增環境變數對照表 |
| `docs/viewpoints/operational/monitoring/application-insights-rum-integration.md` | 架構圖更新（Angular 18→Next.js 15、Next.js 14→Next.js 16）、Consumer 整合章節從 Angular 改寫為 Next.js 15、CMC 章節標註 Next.js 16 |
| `docs/viewpoints/operational/maintenance/security-compliance.md` | npm audit 指令從 `cd cmc-frontend && npm audit` 改為 `cd frontend && pnpm audit` |

### 📝 Steering 文件更新

| 文件 | 變更內容 |
|------|----------|
| `.kiro/steering/development-guide.md` | 技術棧更新：CMC→Next.js 16 + React 19、Consumer→Next.js 15 + React 19、UI Components 標註 @repo/ui |

---

**Changelog Version**: 1.0
**Last Updated**: 2026-02-21
**Maintained By**: Documentation Team
