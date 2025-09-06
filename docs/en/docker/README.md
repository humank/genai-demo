<!-- 
此文件需要手動翻譯
原文件: docker/README.md
翻譯日期: Thu Aug 21 22:04:37 CST 2025

請將以下中文內容翻譯為英文，保持 Markdown 格式不變
-->

# Docker 目錄

此目錄包含所有 Docker 相關的檔案和腳本。

## 檔案說明

- `docker-build.sh` - ARM64 優化的 Docker 映像構建腳本
- `verify-deployment.sh` - 部署驗證腳本
- `postgres/` - PostgreSQL 初始化腳本 (備用)

## 使用方式

```bash
# 構建映像
./docker/docker-build.sh

# 啟動服務
docker-compose up -d

# 驗證部署
./docker/verify-deployment.sh
```

詳細說明請參考：[Docker 部署指南](../docs/DOCKER_GUIDE.md)


<!-- 翻譯完成後請刪除此註釋 -->
