package solid.humank.genaidemo.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import solid.humank.genaidemo.examples.order.Money;
import solid.humank.genaidemo.examples.order.policy.OrderDiscountPolicy;
import solid.humank.genaidemo.examples.order.validation.OrderValidator;

/**
 * 訂單相關的配置類
 * 將訂單處理相關的參數抽取到配置中，避免硬編碼
 */
@Configuration
@ComponentScan
@EnableConfigurationProperties(OrderProperties.class)
public class OrderConfig {
    
    private final OrderProperties orderProperties;
    
    public OrderConfig(OrderProperties orderProperties) {
        this.orderProperties = orderProperties;
    }
    
    /**
     * 訂單驗證器
     */
    @Bean
    public OrderValidator orderValidator() {
        return new OrderValidator(
            orderProperties.getValidation().getMaxItems(),
            Money.twd(orderProperties.getValidation().getMaxAmount())
        );
    }
    
    /**
     * 訂單折扣政策 - 週末折扣
     */
    @Bean
    public OrderDiscountPolicy weekendDiscountPolicy() {
        return OrderDiscountPolicy.weekendDiscount();
    }
}
