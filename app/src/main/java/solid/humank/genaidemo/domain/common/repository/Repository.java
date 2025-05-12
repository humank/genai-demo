package solid.humank.genaidemo.domain.common.repository;

import java.util.List;
import java.util.Optional;

/**
 * 通用儲存庫接口
 * 
 * @param <T> 實體類型
 * @param <ID> ID類型
 */
public interface Repository<T, ID> {
    
    /**
     * 保存實體
     * 
     * @param entity 實體
     * @return 保存後的實體
     */
    T save(T entity);
    
    /**
     * 根據ID查詢實體
     * 
     * @param id ID
     * @return 實體
     */
    Optional<T> findById(ID id);
    
    /**
     * 查詢所有實體
     * 
     * @return 實體列表
     */
    List<T> findAll();
    
    /**
     * 刪除實體
     * 
     * @param entity 實體
     */
    void delete(T entity);
    
    /**
     * 根據ID刪除實體
     * 
     * @param id ID
     */
    void deleteById(ID id);
    
    /**
     * 計數
     * 
     * @return 實體數量
     */
    long count();
}