package solid.humank.genaidemo.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import solid.humank.genaidemo.examples.order.model.valueobject.Money;
import solid.humank.genaidemo.examples.order.model.policy.OrderDiscountPolicy;

/**
 * 訂單相關配置
 * 提供訂單相關的Bean定義
 */
@Configuration
public class OrderConfig {
    
    /**
     * 定義最大訂單項數量
     * 用於訂單驗證
     */
    @Bean
    public int maxOrderItems() {
        return 20;
    }
    
    /**
     * 定義最大訂單金額
     * 用於訂單驗證
     */
    @Bean
    public Money maxOrderAmount() {
        return Money.twd(100000);
    }
    
    /**
     * 定義折扣率
     * 用於折扣計算
     */
    @Bean
    public BigDecimal discountRate() {
        return new BigDecimal("0.1");
    }
    
    /**
     * 定義週末折扣政策
     */
    @Bean
    public OrderDiscountPolicy weekendDiscountPolicy() {
        return new OrderDiscountPolicy(
            LocalDateTime.now(),
            discountRate()
        );
    }
}