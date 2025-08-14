package solid.humank.genaidemo.application.customer;

import java.util.List;

/** 客戶分頁數據傳輸對象 */
public record CustomerPageDto(
        List<CustomerDto> content,
        int totalElements,
        int totalPages,
        int size,
        int number,
        boolean first,
        boolean last) {}
