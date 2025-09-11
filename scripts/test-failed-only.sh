#!/bin/bash

# 測試失敗的測試腳本
# 專門運行失敗的測試並獲取詳細信息

set -e

echo "🔍 運行失敗的測試..."

# 設置 JVM 參數
export GRADLE_OPTS="-Xmx12g -Xms4g -XX:MaxMetaspaceSize=3g"

# 運行特定的失敗測試
echo "📋 運行 DeadLetterServiceTest..."
./gradlew test --tests="*DeadLetterServiceTest*" --no-daemon --info --stacktrace || true

echo "📋 運行 KafkaDomainEventPublisherTest..."
./gradlew test --tests="*KafkaDomainEventPublisherTest*" --no-daemon --info --stacktrace || true

echo "📋 運行 HealthCheckIntegrationTest..."
./gradlew test --tests="*HealthCheckIntegrationTest*" --no-daemon --info --stacktrace || true

echo "✅ 測試完成"