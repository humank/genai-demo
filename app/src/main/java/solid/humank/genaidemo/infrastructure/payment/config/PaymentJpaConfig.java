package solid.humank.genaidemo.infrastructure.payment.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

/** 支付 JPA 配置類 配置 JPA 實體掃描 */
@Configuration
@EntityScan(basePackages = "solid.humank.genaidemo.infrastructure.payment.persistence.entity")
public class PaymentJpaConfig {
    // 配置類，不需要額外的方法
    // Repository 掃描由主應用程式類別的 @EnableJpaRepositories 處理
}
