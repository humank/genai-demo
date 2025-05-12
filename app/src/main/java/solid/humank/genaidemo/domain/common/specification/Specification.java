package solid.humank.genaidemo.domain.common.specification;

/**
 * 規格接口
 * 
 * @param <T> 實體類型
 */
public interface Specification<T> {
    
    /**
     * 檢查實體是否滿足規格
     * 
     * @param entity 實體
     * @return 是否滿足規格
     */
    boolean isSatisfiedBy(T entity);
    
    /**
     * 與操作
     * 
     * @param other 其他規格
     * @return 新的規格
     */
    default Specification<T> and(Specification<T> other) {
        return entity -> isSatisfiedBy(entity) && other.isSatisfiedBy(entity);
    }
    
    /**
     * 或操作
     * 
     * @param other 其他規格
     * @return 新的規格
     */
    default Specification<T> or(Specification<T> other) {
        return entity -> isSatisfiedBy(entity) || other.isSatisfiedBy(entity);
    }
    
    /**
     * 非操作
     * 
     * @return 新的規格
     */
    default Specification<T> not() {
        return entity -> !isSatisfiedBy(entity);
    }
}