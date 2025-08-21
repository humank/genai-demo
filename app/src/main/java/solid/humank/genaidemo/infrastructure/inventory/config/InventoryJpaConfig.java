package solid.humank.genaidemo.infrastructure.inventory.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

/** 庫存JPA配置 */
@Configuration
@EntityScan(basePackages = "solid.humank.genaidemo.infrastructure.inventory.persistence.entity")
public class InventoryJpaConfig {
    // 配置類，不需要額外的方法
    // Repository 掃描由主應用程式類別的 @EnableJpaRepositories 處理
}
