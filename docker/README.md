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
