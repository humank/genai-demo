package solid.humank.genaidemo.infrastructure.order.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 訂單 JPA 配置類
 * 配置 JPA 實體掃描和 Repository 掃描
 */
@Configuration
@EntityScan(basePackages = "solid.humank.genaidemo.infrastructure.order.persistence.entity")
@EnableJpaRepositories(basePackages = "solid.humank.genaidemo.infrastructure.order.persistence")
public class OrderJpaConfig {
    // 配置類，不需要額外的方法
}