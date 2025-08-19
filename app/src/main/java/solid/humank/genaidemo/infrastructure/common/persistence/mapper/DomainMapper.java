package solid.humank.genaidemo.infrastructure.common.persistence.mapper;

/**
 * 統一的領域模型映射器介面
 * 定義領域模型和持久化模型之間的轉換標準
 * 
 * @param <D> 領域模型類型
 * @param <E> 持久化實體類型
 */
public interface DomainMapper<D, E> {

    /**
     * 將領域模型轉換為持久化實體
     * 
     * @param domainModel 領域模型
     * @return 持久化實體
     */
    E toJpaEntity(D domainModel);

    /**
     * 將持久化實體轉換為領域模型
     * 
     * @param entity 持久化實體
     * @return 領域模型
     */
    D toDomainModel(E entity);
}