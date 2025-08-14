package solid.humank.genaidemo.infrastructure.inventory.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** 庫存JPA配置 */
@Configuration
@EntityScan(basePackages = "solid.humank.genaidemo.infrastructure.inventory.persistence.entity")
@EnableJpaRepositories(
        basePackages = "solid.humank.genaidemo.infrastructure.inventory.persistence.repository")
public class InventoryJpaConfig {
    // 配置類，不需要額外的方法
}
