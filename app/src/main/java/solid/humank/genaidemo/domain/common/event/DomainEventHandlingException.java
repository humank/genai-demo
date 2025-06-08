package solid.humank.genaidemo.domain.common.event;

import java.util.ArrayList;
import java.util.List;

/**
 * 領域事件處理異常
 * 將其從DomainEventBus中提取出來並設為public，以符合架構測試要求
 */
public class DomainEventHandlingException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    private final List<Exception> exceptions;

    /**
     * 構造函數
     * 
     * @param message 錯誤訊息
     * @param exceptions 異常列表
     */
    public DomainEventHandlingException(String message, List<Exception> exceptions) {
        super(String.format("%s: %d handlers failed", message, exceptions.size()));
        this.exceptions = new ArrayList<>(exceptions);
    }

    /**
     * 獲取異常列表
     * 
     * @return 異常列表的副本
     */
    public List<Exception> getExceptions() {
        return new ArrayList<>(exceptions);
    }
}