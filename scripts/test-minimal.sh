#!/bin/bash

# 極簡測試腳本 - 只運行最基本的測試
# 避免記憶體不足問題

echo "🧪 開始執行極簡測試..."

# 設定更大的記憶體配置
export GRADLE_OPTS="-Xmx6g -Xms2g -XX:MaxMetaspaceSize=1g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:G1HeapRegionSize=32m"

echo "📊 記憶體配置: 最大堆記憶體 6GB, 初始堆記憶體 2GB"
echo "📝 只運行基本測試，跳過整合測試"

# 停止所有 Gradle daemon
./gradlew --stop

# 清理
./gradlew clean --quiet

# 只運行單元測試，排除整合測試
echo "🧪 執行單元測試（排除整合測試）..."
./gradlew test \
  --exclude-task cucumber \
  --tests="*Test" \
  --tests="!*IntegrationTest" \
  --tests="!*HealthCheckIntegrationTest" \
  --tests="!*TracingIntegrationTest" \
  --no-daemon \
  --max-workers=1 \
  --quiet

# 檢查測試結果
if [ $? -eq 0 ]; then
    echo "✅ 基本測試執行成功！"
    echo "📊 測試報告位置: app/build/reports/tests/test/index.html"
else
    echo "❌ 測試執行失敗"
    echo "📋 查看詳細錯誤: ./gradlew test --info"
    exit 1
fi