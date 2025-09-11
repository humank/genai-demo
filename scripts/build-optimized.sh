#!/bin/bash

# 優化的編譯腳本
# 設定記憶體參數並減少日誌輸出

echo "🔨 開始執行優化的編譯..."

# 設定 JVM 參數
export GRADLE_OPTS="-Xmx4g -Xms1g -XX:MaxMetaspaceSize=1g -XX:+UseG1GC -XX:+UseStringDeduplication"

echo "📊 記憶體配置: 最大堆記憶體 4GB, 初始堆記憶體 1GB"
echo "🔧 使用 G1 垃圾收集器和字串去重優化"

# 清理之前的編譯結果
echo "🧹 清理之前的編譯結果..."
./gradlew clean --quiet

# 執行編譯
echo "🔨 執行編譯..."
./gradlew build -x test --no-daemon --max-workers=2 --quiet

# 檢查編譯結果
if [ $? -eq 0 ]; then
    echo "✅ 編譯成功！"
    echo "📦 編譯產物位置: app/build/libs/"
    ls -la app/build/libs/
else
    echo "❌ 編譯失敗，請檢查錯誤訊息"
    exit 1
fi