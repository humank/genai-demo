package solid.humank.genaidemo.domain.common.repository;

import solid.humank.genaidemo.domain.common.annotations.Repository;

/**
 * 基礎儲存庫介面
 * 
 * 為了向後相容性而提供的別名介面，實際上繼承自 Repository 介面。
 * 所有領域儲存庫都應該繼承此介面以保持一致性。
 * 
 * @param <T>  聚合根類型，必須標記 @AggregateRoot 註解
 * @param <ID> ID類型
 */
@Repository(name = "BaseRepository", description = "基礎儲存庫介面，提供通用的聚合根儲存操作")
public interface BaseRepository<T, ID> extends solid.humank.genaidemo.domain.common.repository.Repository<T, ID> {
    // 此介面為 Repository 的別名，提供向後相容性
    // 所有方法都由 Repository 介面提供
}