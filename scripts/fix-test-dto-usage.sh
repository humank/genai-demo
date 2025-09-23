#!/bin/bash

# 修正測試文件中的 DTO 使用方式

echo "修正測試文件中的 DTO 構造函數調用..."

# 修正 AnalyticsEventDto 構造函數調用
# 從: new AnalyticsEventDto(eventId, eventType, sessionId, Optional.of(userId), traceId, data, timestamp)
# 到: new AnalyticsEventDto(eventId, eventType, userId, data, timestamp)

find app/src/test -name "*.java" -type f -exec sed -i '' \
    -e 's/new AnalyticsEventDto(\([^,]*\),\([^,]*\),\([^,]*\),\s*Optional\.of(\([^)]*\)),\([^,]*\),\([^,]*\),\([^)]*\))/AnalyticsEventDto.create(\1,\2,\4,\6)/g' \
    -e 's/new AnalyticsEventDto(\([^,]*\),\([^,]*\),\([^,]*\),\s*Optional\.empty(),\([^,]*\),\([^,]*\),\([^)]*\))/AnalyticsEventDto.createAnonymous(\1,\2,\5)/g' \
    {} \;

# 修正 PerformanceMetricDto 構造函數調用
# 從: new PerformanceMetricDto(metricId, metricType, value, page, userId, sessionId, timestamp)
# 到: new PerformanceMetricDto(metricId, metricType, value, page, timestamp)

find app/src/test -name "*.java" -type f -exec sed -i '' \
    -e 's/new PerformanceMetricDto(\([^,]*\),\([^,]*\),\([^,]*\),\([^,]*\),\([^,]*\),\([^,]*\),\([^)]*\))/PerformanceMetricDto.create(\1,\2,\3,\4)/g' \
    {} \;

echo "DTO 構造函數調用修正完成"