#!/bin/bash

# 簡化的測試執行腳本
# 使用保守的記憶體設定和最少的日誌輸出

echo "🧪 開始執行簡化測試..."

# 設定保守的 JVM 參數
export GRADLE_OPTS="-Xmx3g -Xms512m -XX:MaxMetaspaceSize=512m -XX:+UseG1GC"

# 設定日誌級別為 ERROR
export LOGGING_LEVEL_ROOT=ERROR

echo "📊 記憶體配置: 最大堆記憶體 3GB, 初始堆記憶體 512MB"
echo "📝 日誌級別: 只輸出 ERROR 級別日誌"

# 清理之前的測試結果
echo "🧹 清理之前的測試結果..."
./gradlew clean --quiet

# 只執行單元測試，跳過 Cucumber 測試以節省記憶體
echo "🧪 執行單元測試..."
./gradlew test --no-daemon --max-workers=1 --quiet

# 檢查測試結果
if [ $? -eq 0 ]; then
    echo "✅ 單元測試執行成功！"
    echo "📊 測試報告位置: app/build/reports/tests/test/index.html"
else
    echo "❌ 測試執行失敗，請檢查錯誤日誌"
    echo "📋 查看詳細錯誤: ./gradlew test --info"
    exit 1
fi