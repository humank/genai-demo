#!/bin/bash

echo "🧪 執行單元測試 (排除整合測試) - 並行執行"
echo "============================================"

# 設定 JVM 參數
export GRADLE_OPTS="-Xmx12g -XX:MaxMetaspaceSize=3g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:G1HeapRegionSize=32m"

echo "🔧 配置參數:"
echo "   - Gradle JVM: 最大 12GB"
echo "   - 測試 JVM: 最大 4GB"
echo "   - 並行執行: 6 個測試並行"
echo "   - 排除整合測試"

# 執行單元測試，排除整合測試
./gradlew :app:test \
  --exclude-task :app:cucumber \
  --tests "*Test" \
  --tests "!*IntegrationTest" \
  --tests "!*HealthCheck*" \
  --tests "!*Tracing*" \
  --tests "!*EnhancedDomainEventPublishing*" \
  --no-daemon \
  --no-build-cache \
  --rerun-tasks \
  --continue \
  --parallel \
  --max-workers=8

exit_code=$?

if [ $exit_code -eq 0 ]; then
    echo "✅ 單元測試執行成功"
    echo "📋 查看測試報告: app/build/reports/tests/test/index.html"
else
    echo "❌ 單元測試執行失敗"
    echo "📋 查看詳細錯誤:"
    echo "   ./gradlew :app:test --info"
    echo "   或查看測試報告: app/build/reports/tests/test/index.html"
fi

exit $exit_code