#!/bin/bash

# 系統資源檢查腳本
# 檢查系統記憶體和 Java 環境

echo "🔍 檢查系統資源和環境..."

# 檢查 Java 版本
echo "☕ Java 版本:"
java -version

echo ""

# 檢查系統記憶體 (macOS)
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "💾 系統記憶體資訊:"
    TOTAL_MEM=$(sysctl -n hw.memsize)
    TOTAL_MEM_GB=$((TOTAL_MEM / 1024 / 1024 / 1024))
    echo "總記憶體: ${TOTAL_MEM_GB} GB"
    
    # 檢查可用記憶體
    FREE_MEM=$(vm_stat | grep "Pages free" | awk '{print $3}' | sed 's/\.//')
    FREE_MEM_GB=$((FREE_MEM * 4096 / 1024 / 1024 / 1024))
    echo "可用記憶體: 約 ${FREE_MEM_GB} GB"
else
    # Linux
    echo "💾 系統記憶體資訊:"
    free -h
fi

echo ""

# 檢查 Gradle 版本
echo "🔧 Gradle 版本:"
./gradlew --version | head -5

echo ""

# 建議的記憶體配置
echo "💡 建議的記憶體配置:"
if [ $TOTAL_MEM_GB -ge 16 ]; then
    echo "✅ 系統記憶體充足 (${TOTAL_MEM_GB}GB)，可以使用完整配置"
    echo "   建議 Gradle JVM: -Xmx4g"
    echo "   建議測試 JVM: -Xmx4g"
elif [ $TOTAL_MEM_GB -ge 8 ]; then
    echo "⚠️  系統記憶體適中 (${TOTAL_MEM_GB}GB)，建議使用保守配置"
    echo "   建議 Gradle JVM: -Xmx2g"
    echo "   建議測試 JVM: -Xmx2g"
else
    echo "🚨 系統記憶體不足 (${TOTAL_MEM_GB}GB)，建議使用最小配置"
    echo "   建議 Gradle JVM: -Xmx1g"
    echo "   建議測試 JVM: -Xmx1g"
fi

echo ""

# 檢查磁碟空間
echo "💿 磁碟空間:"
df -h . | tail -1

echo ""

# 檢查 Gradle daemon 狀態
echo "🔄 Gradle Daemon 狀態:"
./gradlew --status