package solid.humank.genaidemo.domain.common.factory;

public interface DomainFactory<R, P1> {
    /**
     * 從給定的參數創建領域物件
     *
     * @param parameters 創建物件所需的參數
     * @return 新創建的領域物件
     */
    R create(P1 parameters);

    /**
     * 重建已存在的領域物件
     *
     * @param parameters 重建物件所需的參數
     * @return 重建的領域物件
     */
    R reconstitute(P1 parameters);
}
