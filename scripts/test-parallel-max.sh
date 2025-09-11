#!/bin/bash

echo "🚀 執行高並行測試 (最大記憶體 + 最大並行)"
echo "=========================================="

# 檢查系統資源
echo "📊 系統資源檢查:"
echo "   - CPU 核心數: $(nproc)"
echo "   - 可用記憶體: $(free -h | grep '^Mem:' | awk '{print $7}' || echo 'N/A')"

# 設定最大記憶體和並行配置
export GRADLE_OPTS="-Xmx12g -Xms4g -XX:MaxMetaspaceSize=3g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:G1HeapRegionSize=32m -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=20 -XX:G1MaxNewSizePercent=30"

echo "🔧 配置參數:"
echo "   - Gradle JVM: 最大 12GB, 初始 4GB"
echo "   - 測試 JVM: 最大 4GB, 初始 1GB"
echo "   - Metaspace: 最大 3GB"
echo "   - Gradle Workers: 8"
echo "   - 測試並行數: 6"
echo "   - 垃圾收集器: G1GC"

# 停止現有 daemon
echo "🛑 停止現有 Gradle daemon..."
./gradlew --stop

# 等待系統釋放資源
sleep 2

# 清理構建
echo "🧹 清理構建..."
./gradlew clean --quiet

# 執行測試
echo "🧪 開始並行測試執行..."
./gradlew test \
  --no-daemon \
  --parallel \
  --max-workers=8 \
  --no-build-cache \
  --no-configuration-cache \
  --continue

exit_code=$?

if [ $exit_code -eq 0 ]; then
    echo ""
    echo "✅ 並行測試執行成功！"
    echo "📋 測試報告: app/build/reports/tests/test/index.html"
else
    echo ""
    echo "❌ 並行測試執行失敗"
    echo "📋 查看詳細錯誤:"
    echo "   ./gradlew test --info"
    echo "   或查看測試報告: app/build/reports/tests/test/index.html"
fi

exit $exit_code