#!/bin/bash

echo "修正所有測試錯誤..."

# 1. 修正 @SpyBean 為 @MockBean
echo "修正 @SpyBean 註解..."
find app/src/test -name "*.java" -type f -exec sed -i '' 's/@SpyBean/@MockBean/g' {} \;

# 2. 修正方法名稱
echo "修正方法名稱..."
find app/src/test -name "*.java" -type f -exec sed -i '' \
    -e 's/handleAnalyticsEvent/publishAnalyticsEvent/g' \
    -e 's/handlePerformanceMetricEvent/publishPerformanceMetric/g' \
    -e 's/handleInMemoryAnalyticsEvent/processInMemoryEvent/g' \
    {} \;

# 3. 修正 AnalyticsEventDto 構造函數 - 簡單替換
echo "修正 AnalyticsEventDto 構造函數..."
find app/src/test -name "*.java" -type f -exec sed -i '' \
    -e 's/new AnalyticsEventDto(/AnalyticsEventDto.create(/g' \
    {} \;

# 4. 修正 PerformanceMetricDto 構造函數
echo "修正 PerformanceMetricDto 構造函數..."
find app/src/test -name "*.java" -type f -exec sed -i '' \
    -e 's/new PerformanceMetricDto(/PerformanceMetricDto.create(/g' \
    {} \;

echo "基本修正完成，需要手動處理複雜的構造函數調用"