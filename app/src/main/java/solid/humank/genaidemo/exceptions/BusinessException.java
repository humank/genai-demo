package solid.humank.genaidemo.exceptions;

import java.util.List;

/**
 * 業務異常
 * 表示業務邏輯錯誤
 */
public class BusinessException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    private final List<String> errors;
    
    /**
     * 構造函數
     * 
     * @param message 錯誤信息
     */
    public BusinessException(String message) {
        super(message);
        this.errors = List.of(message);
    }
    
    /**
     * 構造函數
     * 
     * @param message 錯誤信息
     * @param cause 原因
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errors = List.of(message);
    }
    
    /**
     * 構造函數
     * 
     * @param errors 錯誤信息列表
     */
    public BusinessException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = errors;
    }
    
    /**
     * 獲取錯誤信息列表
     * 
     * @return 錯誤信息列表
     */
    public List<String> getErrors() {
        return errors;
    }
}