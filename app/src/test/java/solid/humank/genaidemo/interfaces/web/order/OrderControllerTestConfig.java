package solid.humank.genaidemo.interfaces.web.order;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import solid.humank.genaidemo.application.order.port.incoming.OrderManagementUseCase;

import static org.mockito.Mockito.mock;

/**
 * 訂單控制器測試配置
 * 提供測試所需的模擬依賴
 */
@TestConfiguration
public class OrderControllerTestConfig {

    @Bean
    @Primary
    public OrderManagementUseCase orderManagementUseCase() {
        return mock(OrderManagementUseCase.class);
    }
}