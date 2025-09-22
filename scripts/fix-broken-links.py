#!/usr/bin/env python3
"""
修復專案中損壞的 Markdown 連結
"""

import os
import re
import sys
from pathlib import Path

class LinkFixer:
    def __init__(self, project_root):
        self.project_root = Path(project_root)
        self.fixes_applied = []
        self.errors = []
        
    def log_fix(self, file_path, old_link, new_link):
        """記錄修復的連結"""
        self.fixes_applied.append({
            'file': file_path,
            'old': old_link,
            'new': new_link
        })
        print(f"✅ 修復: {file_path}")
        print(f"   舊連結: {old_link}")
        print(f"   新連結: {new_link}")
        print()
    
    def log_error(self, file_path, error):
        """記錄錯誤"""
        self.errors.append({
            'file': file_path,
            'error': error
        })
        print(f"❌ 錯誤: {file_path} - {error}")
    
    def create_troubleshooting_docs(self):
        """創建故障排除文檔"""
        troubleshooting_dir = self.project_root / "docs" / "troubleshooting"
        troubleshooting_dir.mkdir(parents=True, exist_ok=True)
        
        # 創建主要的故障排除文檔
        readme_content = """# 故障排除指南

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
"""
        
        readme_path = troubleshooting_dir / "README.md"
        with open(readme_path, 'w', encoding='utf-8') as f:
            f.write(readme_content)
        
        print(f"✅ 創建故障排除文檔: {readme_path}")
        return str(readme_path)
    
    def fix_development_viewpoint_links(self):
        """修復 development-viewpoint-reorganization-plan.md 中的連結"""
        file_path = self.project_root / "development-viewpoint-reorganization-plan.md"
        
        if not file_path.exists():
            self.log_error(str(file_path), "文件不存在")
            return
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            original_content = content
            
            # 修復連結映射
            link_fixes = [
                # 舊連結 -> 新連結
                (r'\[快速入門\]\(\.\./viewpoints/development/getting-started/README\.md\)', 
                 '[快速入門](../viewpoints/development/getting-started.md)'),
                (r'\[編碼標準\]\(\.\./viewpoints/development/coding-standards/README\.md\)', 
                 '[編碼標準](../viewpoints/development/coding-standards.md)'),
                (r'\[測試指南\]\(\.\./viewpoints/development/testing/README\.md\)', 
                 '[測試指南](../viewpoints/development/testing/tdd-bdd-testing.md)'),
                (r'\[編碼標準\]\(viewpoints/development/coding-standards/README\.md\)', 
                 '[編碼標準](viewpoints/development/coding-standards.md)'),
                (r'\[測試策略\]\(viewpoints/development/testing/README\.md\)', 
                 '[測試策略](viewpoints/development/testing/tdd-bdd-testing.md)'),
            ]
            
            for old_pattern, new_link in link_fixes:
                if re.search(old_pattern, content):
                    content = re.sub(old_pattern, new_link, content)
                    self.log_fix(str(file_path), old_pattern, new_link)
            
            # 如果有變更，寫回文件
            if content != original_content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(content)
            
        except Exception as e:
            self.log_error(str(file_path), f"處理文件時出錯: {e}")
    
    def fix_deployment_guide_links(self):
        """修復 DEPLOYMENT_GUIDE.md 中的連結"""
        file_path = self.project_root / "DEPLOYMENT_GUIDE.md"
        
        if not file_path.exists():
            self.log_error(str(file_path), "文件不存在")
            return
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            original_content = content
            
            # 修復故障排除連結
            old_link = r'\[故障排除\]\(docs/troubleshooting/\)'
            new_link = '[故障排除](docs/troubleshooting/README.md)'
            
            if re.search(old_link, content):
                content = re.sub(old_link, new_link, content)
                self.log_fix(str(file_path), old_link, new_link)
            
            # 如果有變更，寫回文件
            if content != original_content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(content)
            
        except Exception as e:
            self.log_error(str(file_path), f"處理文件時出錯: {e}")
    
    def fix_developer_quickstart_links(self):
        """修復 DEVELOPER_QUICKSTART.md 中的連結"""
        file_path = self.project_root / "DEVELOPER_QUICKSTART.md"
        
        if not file_path.exists():
            self.log_error(str(file_path), "文件不存在")
            return
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            original_content = content
            
            # 修復故障排除連結
            old_link = r'\[故障排除文檔\]\(docs/troubleshooting/\)'
            new_link = '[故障排除文檔](docs/troubleshooting/README.md)'
            
            if re.search(old_link, content):
                content = re.sub(old_link, new_link, content)
                self.log_fix(str(file_path), old_link, new_link)
            
            # 如果有變更，寫回文件
            if content != original_content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(content)
            
        except Exception as e:
            self.log_error(str(file_path), f"處理文件時出錯: {e}")
    
    def run_fixes(self):
        """執行所有修復"""
        print("🔧 開始修復損壞的連結...")
        print("=" * 50)
        
        # 1. 創建故障排除文檔
        print("📁 創建故障排除文檔...")
        self.create_troubleshooting_docs()
        print()
        
        # 2. 修復各個文件中的連結
        print("🔗 修復文件連結...")
        self.fix_development_viewpoint_links()
        self.fix_deployment_guide_links()
        self.fix_developer_quickstart_links()
        
        # 3. 輸出總結
        print("=" * 50)
        print("📊 修復總結:")
        print(f"✅ 成功修復: {len(self.fixes_applied)} 個連結")
        print(f"❌ 錯誤: {len(self.errors)} 個")
        
        if self.fixes_applied:
            print("\n🔧 修復詳情:")
            for fix in self.fixes_applied:
                print(f"  - {fix['file']}")
        
        if self.errors:
            print("\n❌ 錯誤詳情:")
            for error in self.errors:
                print(f"  - {error['file']}: {error['error']}")
        
        print("\n🎉 連結修復完成！")

def main():
    """主函數"""
    # 獲取專案根目錄
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    
    print(f"📂 專案根目錄: {project_root}")
    
    # 創建修復器並執行
    fixer = LinkFixer(project_root)
    fixer.run_fixes()

if __name__ == "__main__":
    main()