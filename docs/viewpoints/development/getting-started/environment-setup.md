# 環境配置指南

本文檔提供完整的開發環境設置指南，確保所有開發者都能快速建立一致的開發環境。

## 前置需求

### 必要軟體

- **Java 21**：主要開發語言
- **Node.js 18+**：前端開發和工具鏈
- **Git**：版本控制系統
- **Docker**：容器化開發環境

### 推薦工具

- **IntelliJ IDEA**：Java 開發 IDE
- **VS Code**：通用編輯器
- **Postman**：API 測試工具

## Java 環境設置

### 使用 SDKMAN 安裝 Java

```bash
# 安裝 SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# 安裝 Java 21
sdk install java 21.0.1-tem
sdk use java 21.0.1-tem

# 驗證安裝
java -version
```

### 環境變數設置

```bash
# 添加到 ~/.bashrc 或 ~/.zshrc
export JAVA_HOME=$HOME/.sdkman/candidates/java/current
export PATH=$JAVA_HOME/bin:$PATH
```

## Node.js 環境設置

### 使用 NVM 安裝 Node.js

```bash
# 安裝 NVM
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
source ~/.bashrc

# 安裝 Node.js 18
nvm install 18
nvm use 18
nvm alias default 18

# 驗證安裝
node --version
npm --version
```

## 專案設置

### 複製專案

```bash
# 複製專案
git clone <repository-url>
cd genai-demo

# 檢查 Java 版本
./gradlew --version
```

### 建置專案

```bash
# 清理並建置
./gradlew clean build

# 執行測試
./gradlew test

# 啟動應用
./gradlew bootRun
```

### 前端設置

#### CMC 前端 (Next.js)

```bash
cd cmc-frontend

# 安裝依賴
npm install

# 啟動開發伺服器
npm run dev
```

#### 消費者前端 (Angular)

```bash
cd consumer-frontend

# 安裝依賴
npm install

# 啟動開發伺服器
npm start
```

## IDE 配置

### IntelliJ IDEA 設置

#### 必要插件

- **Lombok Plugin**：支援 Lombok 註解
- **Spring Boot Plugin**：Spring Boot 支援
- **Cucumber for Java**：BDD 測試支援

#### 程式碼格式設置

1. 匯入專案程式碼格式設置
2. 啟用自動格式化
3. 設置 import 優化

#### JVM 參數

```
-Xmx4g
-XX:+UseG1GC
-XX:+UseStringDeduplication
```

### VS Code 設置

#### 推薦擴充套件

- **Extension Pack for Java**：Java 開發支援
- **Spring Boot Extension Pack**：Spring Boot 支援
- **Cucumber (Gherkin) Full Support**：BDD 支援

## 資料庫設置

### 開發環境 (H2)

H2 資料庫會自動啟動，無需額外設置。

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:h2:file:./data/devdb
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
      path: /h2-console
```

### 生產環境 (PostgreSQL)

```bash
# 使用 Docker 啟動 PostgreSQL
docker run --name postgres-dev \
  -e POSTGRES_DB=genaidemo \
  -e POSTGRES_USER=dev \
  -e POSTGRES_PASSWORD=dev123 \
  -p 5432:5432 \
  -d postgres:15
```

## Docker 環境

### 建置 Docker 映像

```bash
# 建置後端映像
./gradlew bootBuildImage

# 建置前端映像
cd cmc-frontend
docker build -t cmc-frontend .

cd ../consumer-frontend
docker build -t consumer-frontend .
```

### 使用 Docker Compose

```bash
# 啟動完整環境
docker-compose up -d

# 查看服務狀態
docker-compose ps

# 停止服務
docker-compose down
```

## 測試環境設置

### 單元測試

```bash
# 執行單元測試
./gradlew unitTest

# 執行特定測試
./gradlew test --tests "CustomerServiceTest"
```

### 整合測試

```bash
# 執行整合測試
./gradlew integrationTest

# 執行 BDD 測試
./gradlew cucumber
```

### 效能測試

```bash
# 執行效能測試
./gradlew performanceTest

# 生成效能報告
./gradlew generatePerformanceReport
```

## 開發工具配置

### Git 配置

```bash
# 設置使用者資訊
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# 設置預設分支
git config --global init.defaultBranch main

# 設置 Git Hook
cp scripts/pre-commit .git/hooks/
chmod +x .git/hooks/pre-commit
```

### 程式碼品質工具

```bash
# 執行程式碼檢查
./gradlew checkstyleMain
./gradlew spotbugsMain
./gradlew pmdMain

# 執行安全掃描
./gradlew dependencyCheckAnalyze
```

## 故障排除

### 常見問題

#### Java 版本問題

```bash
# 檢查 Java 版本
java -version
./gradlew --version

# 切換 Java 版本
sdk use java 21.0.1-tem
```

#### 記憶體不足

```bash
# 增加 Gradle 記憶體
export GRADLE_OPTS="-Xmx4g -XX:+UseG1GC"

# 或在 gradle.properties 中設置
org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC
```

#### 連接埠衝突

```bash
# 檢查連接埠使用情況
lsof -i :8080
lsof -i :3000

# 終止佔用連接埠的程序
kill -9 <PID>
```

### 效能優化

#### Gradle 建置優化

```properties
# gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC
```

#### IDE 效能優化

- 增加 IDE 記憶體配置
- 排除不必要的目錄索引
- 使用增量編譯

## 驗證設置

### 環境檢查清單

- [ ] Java 21 已安裝並設為預設版本
- [ ] Node.js 18+ 已安裝
- [ ] Git 已配置使用者資訊
- [ ] IDE 已安裝必要插件
- [ ] 專案可以成功建置
- [ ] 所有測試都能通過
- [ ] 應用可以正常啟動
- [ ] 前端應用可以正常運行
- [ ] 資料庫連接正常

### 快速驗證腳本

```bash
#!/bin/bash
echo "=== 環境驗證 ==="

# 檢查 Java
echo "Java 版本:"
java -version

# 檢查 Node.js
echo "Node.js 版本:"
node --version

# 檢查 Git
echo "Git 版本:"
git --version

# 建置專案
echo "建置專案:"
./gradlew build --quiet

# 執行測試
echo "執行測試:"
./gradlew test --quiet

echo "=== 驗證完成 ==="
```

## 相關資源

- [前置需求](prerequisites.md)
- [首次貢獻指南](first-contribution.md)
- [開發工作流程](../workflows/development-workflow.md)
- [測試指南](../testing/README.md)

---

*環境設置完成後，請參考 [首次貢獻指南](first-contribution.md) 開始您的第一個功能開發。*