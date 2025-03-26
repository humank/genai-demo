package solid.humank.genaidemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 訂單相關配置參數
 * 用於綁定 application.properties 中的訂單相關配置
 */
@Component
@ConfigurationProperties(prefix = "order")
public class OrderProperties {
    
    private Validation validation = new Validation();
    
    public static class Validation {
        private int maxItems = 100;
        private int maxAmount = 1000000;
        
        public int getMaxItems() {
            return maxItems;
        }
        
        public void setMaxItems(int maxItems) {
            this.maxItems = maxItems;
        }
        
        public int getMaxAmount() {
            return maxAmount;
        }
        
        public void setMaxAmount(int maxAmount) {
            this.maxAmount = maxAmount;
        }
    }
    
    public Validation getValidation() {
        return validation;
    }
    
    public void setValidation(Validation validation) {
        this.validation = validation;
    }
}
