package solid.humank.genaidemo.application.common.dto;

import java.util.List;

/**
 * 分頁結果 DTO
 * 用於封裝分頁查詢的結果
 */
public class PagedResult<T> {
    private final List<T> content;
    private final int totalElements;
    private final int totalPages;
    private final int size;
    private final int number;
    private final boolean first;
    private final boolean last;

    public PagedResult(List<T> content, int totalElements, int totalPages, 
                      int size, int number, boolean first, boolean last) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.size = size;
        this.number = number;
        this.first = first;
        this.last = last;
    }

    public static <T> PagedResult<T> of(List<T> content, int totalElements, int page, int size) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean first = page == 0;
        boolean last = page >= totalPages - 1;
        
        return new PagedResult<>(content, totalElements, totalPages, size, page, first, last);
    }

    // Getters
    public List<T> getContent() {
        return content;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getSize() {
        return size;
    }

    public int getNumber() {
        return number;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isLast() {
        return last;
    }
}