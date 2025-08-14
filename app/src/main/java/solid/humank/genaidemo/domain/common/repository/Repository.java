package solid.humank.genaidemo.domain.common.repository;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;

import java.util.List;
import java.util.Optional;

/**
 * 通用儲存庫接口
 * 
 * @param <T> 聚合根類型，必須標記 @AggregateRoot 註解
 * @param <ID> ID類型
 */
public interface Repository<T, ID> {
    
    /**
     * 保存聚合根
     * 
     * @param aggregateRoot 聚合根
     * @return 保存後的聚合根
     */
    T save(T aggregateRoot);
    
    /**
     * 根據ID查詢聚合根
     * 
     * @param id ID
     * @return 聚合根
     */
    Optional<T> findById(ID id);
    
    /**
     * 查詢所有聚合根
     * 
     * @return 聚合根列表
     */
    List<T> findAll();
    
    /**
     * 刪除聚合根
     * 
     * @param aggregateRoot 聚合根
     */
    void delete(T aggregateRoot);
    
    /**
     * 根據ID刪除聚合根
     * 
     * @param id ID
     */
    void deleteById(ID id);
    
    /**
     * 計數
     * 
     * @return 聚合根數量
     */
    long count();
    
    /**
     * 檢查是否存在
     * 
     * @param id ID
     * @return 是否存在
     */
    boolean existsById(ID id);
}