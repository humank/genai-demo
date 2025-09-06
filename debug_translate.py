#!/usr/bin/env python3
import sys
import subprocess
import os

print("🔍 調試翻譯腳本問題...")
print(f"Python 版本: {sys.version}")
print(f"當前目錄: {os.getcwd()}")

# 檢查 q 命令
try:
    result = subprocess.run(['q', '--version'], capture_output=True, text=True, timeout=5)
    print(f"q 命令版本: {result.stdout.strip()}")
except Exception as e:
    print(f"❌ q 命令問題: {e}")

# 測試簡單的 q chat
try:
    print("🧪 測試 q chat...")
    result = subprocess.run(
        ['q', 'chat'],
        input="Hello, just say 'Hi' back",
        text=True,
        capture_output=True,
        timeout=10
    )
    print(f"q chat 返回碼: {result.returncode}")
    print(f"q chat 輸出長度: {len(result.stdout)}")
    if result.stderr:
        print(f"q chat 錯誤: {result.stderr[:200]}")
except subprocess.TimeoutExpired:
    print("⏰ q chat 超時")
except Exception as e:
    print(f"❌ q chat 測試失敗: {e}")

# 檢查文件權限
script_path = "/Users/yikaikao/git/genai-demo/translate_with_q.py"
try:
    import stat
    file_stat = os.stat(script_path)
    print(f"腳本權限: {oct(file_stat.st_mode)}")
    print(f"腳本大小: {file_stat.st_size} bytes")
except Exception as e:
    print(f"❌ 檢查腳本權限失敗: {e}")

print("✅ 調試完成")