package solid.humank.genaidemo.domain.customer;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import java.util.regex.Pattern;

/**
 * 電話號碼值對象
 */
@ValueObject(name = "Phone", description = "台灣手機號碼，09開頭的10位數字")
public class Phone {
    
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^09\\d{8}$");
    
    private final String value;
    
    public Phone(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("電話號碼不能為空");
        }
        
        String trimmedValue = value.trim();
        if (!PHONE_PATTERN.matcher(trimmedValue).matches()) {
            throw new IllegalArgumentException("電話號碼格式不正確，應為09開頭的10位數字");
        }
        
        this.value = trimmedValue;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Phone phone = (Phone) obj;
        return value.equals(phone.value);
    }
    
    @Override
    public int hashCode() {
        return value.hashCode();
    }
    
    @Override
    public String toString() {
        return value;
    }
}