package solid.humank.genaidemo.infrastructure;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import solid.humank.genaidemo.domain.customer.repository.CustomerRepository;
import solid.humank.genaidemo.infrastructure.customer.persistence.adapter.CustomerRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.customer.persistence.mapper.CustomerMapper;
import solid.humank.genaidemo.infrastructure.customer.persistence.repository.JpaCustomerRepository;
import solid.humank.genaidemo.testutils.BaseTest;

/**
 * 基礎設施配置測試
 * 驗證所有基礎設施組件是否正確配置和注入
 */
@SpringBootTest
@ActiveProfiles("test")
class InfrastructureConfigurationTest extends BaseTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldLoadApplicationContext() {
        assertNotNull(applicationContext, "Application context should be loaded");
    }

    @Test
    void shouldHaveCustomerRepositoryBean() {
        assertTrue(applicationContext.containsBean("customerRepositoryAdapter"),
                "CustomerRepositoryAdapter bean should be present");

        CustomerRepository customerRepository = applicationContext.getBean(CustomerRepository.class);
        assertNotNull(customerRepository, "CustomerRepository should be injected");

        assertTrue(customerRepository instanceof CustomerRepositoryAdapter,
                "CustomerRepository should be implemented by CustomerRepositoryAdapter");
    }

    @Test
    void shouldHaveCustomerMapperBean() {
        assertTrue(applicationContext.containsBean("customerMapper"),
                "CustomerMapper bean should be present");

        CustomerMapper customerMapper = applicationContext.getBean(CustomerMapper.class);
        assertNotNull(customerMapper, "CustomerMapper should be injected");
    }

    @Test
    void shouldHaveJpaCustomerRepositoryBean() {
        JpaCustomerRepository jpaCustomerRepository = applicationContext.getBean(JpaCustomerRepository.class);
        assertNotNull(jpaCustomerRepository, "JpaCustomerRepository should be injected");
    }

    @Test
    void shouldHaveAllRequiredRepositoryBeans() {
        // 檢查所有主要的 Repository 是否都有對應的 Bean
        String[] expectedRepositoryBeans = {
                "customerRepositoryAdapter",
                "orderRepositoryAdapter",
                "productRepositoryAdapter",
                "inventoryRepositoryAdapter",
                "shoppingCartRepositoryAdapter",
                "paymentRepositoryAdapter",
                "promotionRepositoryAdapter"
        };

        for (String beanName : expectedRepositoryBeans) {
            assertTrue(applicationContext.containsBean(beanName),
                    "Repository bean '" + beanName + "' should be present");
        }
    }

    @Test
    void shouldHaveSwaggerConfiguration() {
        // 檢查 OpenAPI 配置是否正確載入
        assertTrue(applicationContext.containsBean("openApiConfig"),
                "OpenApiConfig bean should be present");
    }
}