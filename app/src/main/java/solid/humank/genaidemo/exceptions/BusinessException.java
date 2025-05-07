package solid.humank.genaidemo.exceptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 業務邏輯異常
 * 用於表示業務規則違反或業務處理失敗
 */
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    private final List<String> errors;

    /**
     * 創建一個業務異常
     * 
     * @param message 錯誤訊息
     */
    public BusinessException(String message) {
        this(List.of(message));
    }

    /**
     * 創建一個業務異常
     * 
     * @param message 錯誤訊息
     * @param cause 原因
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errors = List.of(message);
    }

    /**
     * 創建一個業務異常
     * 
     * @param errors 錯誤訊息列表
     */
    public BusinessException(List<String> errors) {
        super(String.join(", ", errors));
        this.errors = new ArrayList<>(errors);
    }

    /**
     * 創建一個業務異常
     * 
     * @param errors 錯誤訊息列表
     * @param cause 原因
     */
    public BusinessException(List<String> errors, Throwable cause) {
        super(String.join(", ", errors), cause);
        this.errors = new ArrayList<>(errors);
    }

    /**
     * 獲取錯誤訊息列表
     * 
     * @return 錯誤訊息列表的不可變副本
     */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
