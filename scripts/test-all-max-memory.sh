#!/bin/bash

# 超高記憶體配置的完整測試腳本
# 使用最大可能的記憶體配置來運行所有測試

echo "🚀 開始執行完整測試（超高記憶體配置）..."

# 設定超大記憶體配置
export GRADLE_OPTS="-Xmx12g -Xms4g -XX:MaxMetaspaceSize=3g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:G1HeapRegionSize=32m -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=20 -XX:G1MaxNewSizePercent=30"

echo "📊 記憶體配置:"
echo "   - Gradle JVM: 最大 12GB, 初始 4GB"
echo "   - 測試 JVM: 最大 4GB, 初始 1GB"
echo "   - 編譯 JVM: 最大 4GB"
echo "   - Metaspace: 最大 3GB"
echo "   - 並行執行: 8 個 worker, 6 個測試並行"
echo "🔧 使用 G1 垃圾收集器優化配置"

# 停止所有現有的 Gradle daemon 以釋放記憶體
echo "🛑 停止現有 Gradle daemon..."
./gradlew --stop

# 等待一下讓系統釋放記憶體
sleep 3

# 清理之前的構建
echo "🧹 清理之前的構建..."
./gradlew clean --quiet

# 運行所有測試，包括 Cucumber
echo "🧪 執行所有測試（包括 BDD 測試）..."
./gradlew runAllTests \
  --no-daemon \
  --max-workers=8 \
  --parallel \
  --no-build-cache \
  --no-configuration-cache

# 檢查測試結果
if [ $? -eq 0 ]; then
    echo ""
    echo "🎉 所有測試執行成功！"
    echo ""
    echo "📊 測試報告位置:"
    echo "   - JUnit 報告: app/build/reports/tests/test/index.html"
    echo "   - Cucumber 報告: app/build/reports/cucumber/cucumber-report.html"
    echo "   - Allure 報告: app/build/reports/allure-report/index.html"
    echo ""
    echo "📈 測試統計:"
    if [ -f "app/build/test-results/test/TEST-*.xml" ]; then
        TOTAL_TESTS=$(grep -h "tests=" app/build/test-results/test/TEST-*.xml | sed 's/.*tests="\([0-9]*\)".*/\1/' | awk '{sum += $1} END {print sum}')
        FAILED_TESTS=$(grep -h "failures=" app/build/test-results/test/TEST-*.xml | sed 's/.*failures="\([0-9]*\)".*/\1/' | awk '{sum += $1} END {print sum}')
        PASSED_TESTS=$((TOTAL_TESTS - FAILED_TESTS))
        echo "   - 總測試數: $TOTAL_TESTS"
        echo "   - 通過測試: $PASSED_TESTS"
        echo "   - 失敗測試: $FAILED_TESTS"
        if [ $TOTAL_TESTS -gt 0 ]; then
            SUCCESS_RATE=$(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc -l)
            echo "   - 成功率: ${SUCCESS_RATE}%"
        fi
    fi
else
    echo ""
    echo "❌ 測試執行失敗"
    echo "📋 查看詳細錯誤:"
    echo "   ./gradlew runAllTests --info"
    echo "   或查看測試報告: app/build/reports/tests/test/index.html"
    exit 1
fi