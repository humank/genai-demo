package solid.humank.genaidemo.interfaces.web.order.dto;

import java.util.Collections;
import java.util.List;

/** 錯誤響應 DTO - 使用 Record 實作 */
public record ErrorResponse(List<String> errors) {

    /**
     * 單一錯誤訊息建構子
     *
     * @param error 錯誤訊息
     */
    public ErrorResponse(String error) {
        this(Collections.singletonList(error));
    }

    /**
     * 獲取錯誤列表（向後相容方法）
     *
     * @return 錯誤列表
     */
    public List<String> getErrors() {
        return errors;
    }
}
