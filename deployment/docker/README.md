# Docker Compose 配置

此目錄包含各種 Docker Compose 配置檔案。

## 檔案說明

### docker-compose-redis-dev.yml
開發環境的 Redis 配置，使用單一 Redis 實例。

**使用方式**:
```bash
docker-compose -f deployment/docker/docker-compose-redis-dev.yml up -d
```

### docker-compose-redis-ha.yml
高可用性 Redis 配置，使用 Redis Sentinel 進行主從複製和自動故障轉移。

**使用方式**:
```bash
docker-compose -f deployment/docker/docker-compose-redis-ha.yml up -d
```

## 主要 Docker Compose

主要的 `docker-compose.yml` 位於專案根目錄，用於啟動完整的應用程式堆疊。

**使用方式**:
```bash
# 在專案根目錄執行
docker-compose up -d
```

## 相關配置

Redis 配置檔案位於 `config/sentinel.conf`。

## 注意事項

1. 開發環境建議使用 `docker-compose-redis-dev.yml`
2. 生產環境或測試高可用性時使用 `docker-compose-redis-ha.yml`
3. 確保 Docker 和 Docker Compose 已正確安裝

---

**最後更新**: 2025-11-23
