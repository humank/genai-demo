# 生產環境部署檢查清單

## 概述

本檢查清單確保可觀測性系統在生產環境中的正確部署和配置。請在部署前逐項檢查並確認。

## 部署前檢查

### 基礎設施準備

#### AWS 基礎設施

- [ ] **VPC 和網路配置**
  - [ ] VPC 已創建並正確配置
  - [ ] 子網路 (公有/私有) 已設置
  - [ ] 路由表和 NAT 閘道器已配置
  - [ ] 安全群組規則已設定 (最小權限原則)

- [ ] **MSK 叢集**
  - [ ] MSK 叢集已部署並處於 ACTIVE 狀態
  - [ ] 可觀測性主題已在 CDK 配置中定義
    - [ ] `observability.user.behavior`
    - [ ] `observability.performance.metrics`
    - [ ] `observability.business.analytics`
  - [ ] DLQ 主題已配置
  - [ ] 加密配置已啟用 (傳輸中和靜態)
  - [ ] IAM 權限已正確設定

- [ ] **RDS 資料庫**
  - [ ] PostgreSQL 實例已部署
  - [ ] 資料庫加密已啟用
  - [ ] 備份策略已配置
  - [ ] 連接安全群組已設定

- [ ] **ElastiCache Redis**
  - [ ] Redis 叢集已部署
  - [ ] 加密配置已啟用
  - [ ] 安全群組已配置

#### Kubernetes 叢集

- [ ] **EKS 叢集**
  - [ ] EKS 叢集已創建並可訪問
  - [ ] 節點群組已配置
  - [ ] RBAC 權限已設定
  - [ ] 服務帳戶已創建

- [ ] **命名空間和資源**
  - [ ] `genai-demo-production` 命名空間已創建
  - [ ] ConfigMaps 和 Secrets 已配置
  - [ ] 資源限制和請求已設定

### 應用程式配置

#### 後端配置

- [ ] **Spring Boot 配置**
  - [ ] `application-msk.yml` 配置正確
  - [ ] Kafka 連接配置已設定
  - [ ] 資料庫連接配置已設定
  - [ ] Redis 連接配置已設定
  - [ ] 可觀測性功能已啟用

- [ ] **環境變數**
  - [ ] `SPRING_PROFILES_ACTIVE=msk`
  - [ ] `MSK_BOOTSTRAP_SERVERS` 已設定
  - [ ] 資料庫憑證已配置
  - [ ] AWS 憑證已配置

#### 前端配置

- [ ] **Angular 配置**
  - [ ] `environment.prod.ts` 配置正確
  - [ ] API URL 指向生產環境
  - [ ] 可觀測性配置已啟用
  - [ ] WebSocket URL 配置正確

- [ ] **Nginx 配置**
  - [ ] SSL 憑證已配置
  - [ ] 反向代理規則已設定
  - [ ] WebSocket 代理已配置
  - [ ] 安全標頭已設定

### 安全配置

#### 憑證和加密

- [ ] **SSL/TLS 憑證**
  - [ ] 有效的 SSL 憑證已安裝
  - [ ] 憑證自動更新已配置
  - [ ] HTTPS 重定向已啟用

- [ ] **數據加密**
  - [ ] 傳輸中加密已啟用 (TLS 1.2+)
  - [ ] 靜態數據加密已啟用
  - [ ] KMS 金鑰已配置

#### 存取控制

- [ ] **IAM 權限**
  - [ ] 服務角色已創建 (最小權限)
  - [ ] MSK 存取權限已設定
  - [ ] CloudWatch 權限已配置
  - [ ] S3 存取權限已設定 (如需要)

- [ ] **網路安全**
  - [ ] 安全群組規則已設定
  - [ ] NACLs 已配置 (如需要)
  - [ ] VPC 端點已設定 (如需要)

### 監控和日誌

#### CloudWatch 配置

- [ ] **指標收集**
  - [ ] 自定義指標命名空間已設定
  - [ ] 應用程式指標已配置
  - [ ] 基礎設施指標已啟用

- [ ] **警報設定**
  - [ ] 關鍵指標警報已配置
  - [ ] 通知目標已設定 (SNS, Email)
  - [ ] 警報閾值已調整

- [ ] **日誌管理**
  - [ ] CloudWatch Logs 已配置
  - [ ] 日誌保留期已設定
  - [ ] 結構化日誌格式已啟用

#### 儀表板

- [ ] **監控儀表板**
  - [ ] CloudWatch 儀表板已創建
  - [ ] 關鍵業務指標已顯示
  - [ ] 系統健康指標已包含

## 部署執行

### 基礎設施部署

- [ ] **CDK 部署**

  ```bash
  cd infrastructure
  npm install
  npx cdk deploy --all --require-approval never \
    --context environment=production \
    --context projectName=genai-demo
  ```

- [ ] **驗證基礎設施**

  ```bash
  # 檢查 MSK 叢集狀態
  aws kafka describe-cluster --cluster-arn <MSK_CLUSTER_ARN>
  
  # 檢查 RDS 狀態
  aws rds describe-db-instances --db-instance-identifier genai-demo-production-db
  
  # 檢查 EKS 叢集
  kubectl cluster-info
  ```

### 應用程式部署

- [ ] **後端部署**

  ```bash
  # 構建 Docker 映像
  docker build -t genai-demo/backend:latest .
  
  # 推送到 ECR
  aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <ECR_URI>
  docker tag genai-demo/backend:latest <ECR_URI>/genai-demo/backend:latest
  docker push <ECR_URI>/genai-demo/backend:latest
  
  # 部署到 Kubernetes
  kubectl apply -f k8s/production/
  ```

- [ ] **前端部署**

  ```bash
  # 構建前端應用
  cd consumer-frontend
  npm run build:prod
  
  # 部署到 S3 + CloudFront 或 Kubernetes
  kubectl apply -f k8s/production/frontend/
  ```

### Kafka 主題創建

- [ ] **執行主題創建腳本**

  ```bash
  ./scripts/create-kafka-topics.sh production
  ```

- [ ] **驗證主題創建**
  - [ ] 用戶行為分析主題已創建
  - [ ] 效能指標主題已創建
  - [ ] 業務分析主題已創建
  - [ ] DLQ 主題已創建

## 部署後驗證

### 功能測試

- [ ] **健康檢查**

  ```bash
  curl https://api.genai-demo.com/actuator/health
  ```

- [ ] **API 測試**

  ```bash
  # 執行完整的部署驗證
  ./scripts/validate-observability-deployment.sh production
  ```

- [ ] **前端測試**
  - [ ] 網站可正常訪問
  - [ ] 可觀測性功能正常工作
  - [ ] WebSocket 連接正常

### 效能測試

- [ ] **負載測試**

  ```bash
  # 使用 K6 或 Apache Bench 進行負載測試
  k6 run scripts/load-test-production.js
  ```

- [ ] **效能基準**
  - [ ] API 響應時間 < 200ms (95th percentile)
  - [ ] 事件處理延遲 < 100ms
  - [ ] 系統資源使用率 < 70%

### 監控驗證

- [ ] **指標收集**
  - [ ] CloudWatch 指標正常上報
  - [ ] 自定義業務指標可見
  - [ ] 應用程式指標端點可訪問

- [ ] **警報測試**
  - [ ] 觸發測試警報驗證通知
  - [ ] 警報恢復機制正常

- [ ] **日誌驗證**
  - [ ] 應用程式日誌正常輸出
  - [ ] 結構化日誌格式正確
  - [ ] 追蹤 ID 正確傳播

## 安全驗證

### 安全掃描

- [ ] **漏洞掃描**

  ```bash
  # 使用 AWS Inspector 或其他工具掃描
  aws inspector2 create-findings-report
  ```

- [ ] **配置檢查**

  ```bash
  # 執行安全配置檢查
  ./scripts/security-compliance-check.sh production
  ```

### 存取測試

- [ ] **權限驗證**
  - [ ] 服務帳戶權限正確
  - [ ] 最小權限原則已實施
  - [ ] 未授權存取被正確拒絕

- [ ] **加密驗證**
  - [ ] HTTPS 強制執行
  - [ ] 資料庫連接加密
  - [ ] Kafka 通信加密

## 災難恢復準備

### 備份驗證

- [ ] **資料庫備份**
  - [ ] 自動備份已啟用
  - [ ] 備份恢復測試已執行
  - [ ] 跨區域備份已配置 (如需要)

- [ ] **配置備份**
  - [ ] Kubernetes 配置已備份
  - [ ] 應用程式配置已版本控制
  - [ ] 基礎設施代碼已備份

### 恢復程序

- [ ] **恢復計劃**
  - [ ] 災難恢復計劃已文檔化
  - [ ] 恢復時間目標 (RTO) 已定義
  - [ ] 恢復點目標 (RPO) 已定義

- [ ] **恢復測試**
  - [ ] 恢復程序已測試
  - [ ] 恢復時間已測量
  - [ ] 恢復腳本已準備

## 文檔和培訓

### 文檔更新

- [ ] **操作手冊**
  - [ ] 部署程序已文檔化
  - [ ] 故障排除指南已更新
  - [ ] 監控指南已準備

- [ ] **API 文檔**
  - [ ] Swagger UI 可訪問
  - [ ] API 文檔已更新
  - [ ] 範例代碼已提供

### 團隊培訓

- [ ] **操作培訓**
  - [ ] 運維團隊已培訓
  - [ ] 監控程序已說明
  - [ ] 緊急響應程序已演練

- [ ] **開發培訓**
  - [ ] 開發團隊了解新功能
  - [ ] 可觀測性最佳實踐已分享
  - [ ] 故障排除技能已培訓

## 上線準備

### 流量切換

- [ ] **漸進式部署**
  - [ ] 金絲雀部署策略已準備
  - [ ] 流量分流機制已配置
  - [ ] 回滾計劃已準備

- [ ] **DNS 配置**
  - [ ] DNS 記錄已更新
  - [ ] TTL 已調整為較短時間
  - [ ] 健康檢查已配置

### 最終檢查

- [ ] **系統整體測試**
  - [ ] 端到端功能測試通過
  - [ ] 效能測試通過
  - [ ] 安全測試通過

- [ ] **團隊準備**
  - [ ] 值班人員已安排
  - [ ] 緊急聯絡方式已確認
  - [ ] 監控儀表板已設置

## 上線後監控

### 即時監控 (前 24 小時)

- [ ] **關鍵指標監控**
  - [ ] 錯誤率 < 0.1%
  - [ ] 響應時間正常
  - [ ] 資源使用率穩定

- [ ] **業務指標監控**
  - [ ] 用戶活動正常
  - [ ] 事件處理正常
  - [ ] 轉換率穩定

### 持續監控 (前一週)

- [ ] **趨勢分析**
  - [ ] 效能趨勢穩定
  - [ ] 錯誤模式分析
  - [ ] 容量規劃調整

- [ ] **優化機會**
  - [ ] 效能瓶頸識別
  - [ ] 成本優化機會
  - [ ] 用戶體驗改進

## 簽核確認

### 技術簽核

- [ ] **開發團隊**: _________________ 日期: _________
- [ ] **運維團隊**: _________________ 日期: _________
- [ ] **安全團隊**: _________________ 日期: _________
- [ ] **架構師**: _________________ 日期: _________

### 業務簽核

- [ ] **產品經理**: _________________ 日期: _________
- [ ] **專案經理**: _________________ 日期: _________
- [ ] **業務負責人**: _________________ 日期: _________

---

**部署完成確認**

部署負責人: _________________  
部署日期: _________  
部署版本: _________________  

**備註**:
_________________________________________________
_________________________________________________
_________________________________________________
