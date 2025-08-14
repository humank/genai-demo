package solid.humank.genaidemo.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import solid.humank.genaidemo.domain.customer.repository.CustomerRepository;
import solid.humank.genaidemo.infrastructure.customer.persistence.CustomerRepositoryJpaAdapter;
import solid.humank.genaidemo.infrastructure.order.persistence.repository.JpaOrderRepository;

/**
 * 儲存庫配置類
 * 確保使用 JPA 實現替代原生 SQL 實現
 * 符合六角形架構的依賴注入配置
 */
@Configuration
public class RepositoryConfig {
    
    /**
     * 配置客戶儲存庫使用 JPA 適配器
     * 使用 @Primary 確保優先使用 JPA 實現
     */
    @Bean
    @Primary
    public CustomerRepository customerRepository(JpaOrderRepository orderRepository) {
        return new CustomerRepositoryJpaAdapter(orderRepository);
    }
}
