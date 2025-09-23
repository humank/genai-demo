# 故障排除指南

## 概述

本指南提供常見問題的解決方案和故障排除步驟，幫助開發者快速解決開發過程中遇到的問題。

## 🚨 常見問題

### 建置問題

#### Java 版本不符
**問題**: 建置失敗，提示 Java 版本不正確
**解決方案**:
```bash
# 檢查當前 Java 版本
java -version

# 使用 SDKMAN 切換到 Java 21
sdk use java 21.0.1-tem

# 驗證版本
./gradlew --version
```

#### Gradle 建置失敗
**問題**: Gradle 建置過程中出現錯誤
**解決方案**:
```bash
# 清理建置快取
./gradlew clean

# 重新整理依賴
./gradlew --refresh-dependencies

# 完整重建
./gradlew clean build
```

#### 記憶體不足
**問題**: 建置過程中出現 OutOfMemoryError
**解決方案**:
```bash
# 增加 Gradle 記憶體
export GRADLE_OPTS="-Xmx4g -XX:+UseG1GC"

# 或在 gradle.properties 中設置
echo "org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC" >> gradle.properties
```

### 測試問題

#### 測試資料庫連接失敗
**問題**: 測試執行時無法連接到資料庫
**解決方案**:
```bash
# 檢查 H2 資料庫檔案
ls -la data/

# 重置測試資料庫
rm -rf data/testdb*
./gradlew test
```

#### 測試間相互影響
**問題**: 測試在單獨執行時通過，但一起執行時失敗
**解決方案**:
```java
// 確保測試隔離
@Transactional
@Rollback
class CustomerServiceTest {
    
    @BeforeEach
    void setUp() {
        // 清理測試資料
        customerRepository.deleteAll();
    }
}
```

### 前端問題

#### Node.js 依賴衝突
**問題**: npm install 失敗或出現依賴衝突
**解決方案**:
```bash
# 清理 node_modules
rm -rf node_modules package-lock.json
npm install

# 或使用 npm ci 進行乾淨安裝
npm ci
```

#### 連接埠衝突
**問題**: 應用啟動時提示連接埠已被佔用
**解決方案**:
```bash
# 檢查連接埠使用情況
lsof -i :8080  # 後端
lsof -i :3000  # React
lsof -i :4200  # Angular

# 終止佔用連接埠的程序
kill -9 <PID>

# 或使用不同連接埠啟動
npm start -- --port 3001
```

### Docker 問題

#### 容器啟動失敗
**問題**: Docker 容器無法正常啟動
**解決方案**:
```bash
# 檢查 Docker 服務狀態
sudo systemctl status docker

# 重啟 Docker 服務
sudo systemctl restart docker

# 清理 Docker 資源
docker system prune -a
```

#### 資料庫容器連接問題
**問題**: 應用無法連接到 Docker 中的資料庫
**解決方案**:
```bash
# 檢查容器狀態
docker ps -a

# 查看容器日誌
docker logs postgres-dev

# 重新啟動容器
docker restart postgres-dev
```

## 🔧 開發環境問題

### IDE 配置問題

#### IntelliJ IDEA 無法識別專案結構
**解決方案**:
1. File → Invalidate Caches and Restart
2. 重新匯入 Gradle 專案
3. 檢查 Project SDK 設置為 Java 21

#### VS Code 擴充套件問題
**解決方案**:
1. 重新載入視窗 (Ctrl+Shift+P → Developer: Reload Window)
2. 檢查 Java 擴充套件包是否正確安裝
3. 驗證 JAVA_HOME 環境變數

### 效能問題

#### 應用啟動緩慢
**解決方案**:
```bash
# 檢查 JVM 參數
./gradlew bootRun --info

# 使用開發 profile
./gradlew bootRun --args='--spring.profiles.active=dev'

# 啟用 JVM 預熱
export JAVA_OPTS="-XX:TieredStopAtLevel=1 -noverify"
```

#### 測試執行緩慢
**解決方案**:
```bash
# 並行執行測試
./gradlew test --parallel

# 只執行單元測試
./gradlew unitTest

# 跳過慢速測試
./gradlew test -x integrationTest
```

## 📞 獲取幫助

### 內部資源
- [開發視點文檔](../viewpoints/development/README.md)
- [快速入門指南](../viewpoints/development/getting-started.md)
- [建置和部署指南](../viewpoints/development/build-system/build-deployment.md)

### 外部資源
- [Spring Boot 官方文檔](https://spring.io/projects/spring-boot)
- [Gradle 使用指南](https://docs.gradle.org/current/userguide/userguide.html)
- [Docker 官方文檔](https://docs.docker.com/)

### 聯繫支援
- 建立 GitHub Issue 描述問題
- 在團隊 Slack 頻道尋求幫助
- 查看專案 Wiki 中的 FAQ

---

**最後更新**: 2025年1月21日  
**維護者**: Development Team  
**版本**: 1.0

> 💡 **提示**: 如果遇到本指南未涵蓋的問題，請建立 GitHub Issue 或聯繫開發團隊，我們會及時更新本指南。
