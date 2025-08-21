package solid.humank.genaidemo.domain.common.validation;

import java.util.ArrayList;
import java.util.List;

/** 領域驗證器基礎類別 用於實施領域不變條件（Domain Invariants） */
public abstract class DomainValidator<T> {
    private final List<String> errors = new ArrayList<>();

    protected void addError(String error) {
        errors.add(error);
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * 驗證領域物件
     *
     * @param target 要驗證的領域物件
     * @return 驗證是否通過
     */
    public boolean validate(T target) {
        errors.clear();
        doValidate(target);
        return !hasErrors();
    }

    /**
     * 執行實際的驗證邏輯
     *
     * @param target 要驗證的領域物件
     */
    protected abstract void doValidate(T target);
}
