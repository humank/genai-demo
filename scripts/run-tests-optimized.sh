#!/bin/bash

# 優化的測試執行腳本
# 設定記憶體參數並減少日誌輸出

echo "🚀 開始執行優化的測試..."

# 設定 JVM 參數
export GRADLE_OPTS="-Xmx4g -Xms1g -XX:MaxMetaspaceSize=1g -XX:+UseG1GC -XX:+UseStringDeduplication"

# 設定日誌級別為 ERROR
export LOGGING_LEVEL_ROOT=ERROR
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK=ERROR
export LOGGING_LEVEL_SOLID_HUMANK_GENAIDEMO=ERROR

echo "📊 記憶體配置: 最大堆記憶體 4GB, 初始堆記憶體 1GB"
echo "📝 日誌級別: 只輸出 ERROR 級別日誌"

# 清理之前的測試結果
echo "🧹 清理之前的測試結果..."
./gradlew clean

# 執行所有測試
echo "🧪 執行所有測試..."
./gradlew runAllTests --no-daemon --max-workers=2

# 檢查測試結果
if [ $? -eq 0 ]; then
    echo "✅ 所有測試執行成功！"
    echo "📊 測試報告位置:"
    echo "   - JUnit 報告: app/build/reports/tests/test/index.html"
    echo "   - Cucumber 報告: app/build/reports/cucumber/cucumber-report.html"
    echo "   - Allure 報告: app/build/reports/allure-report/index.html"
else
    echo "❌ 測試執行失敗，請檢查錯誤日誌"
    exit 1
fi