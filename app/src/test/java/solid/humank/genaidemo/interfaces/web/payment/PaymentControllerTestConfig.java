package solid.humank.genaidemo.interfaces.web.payment;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import solid.humank.genaidemo.application.payment.port.incoming.PaymentManagementUseCase;

import static org.mockito.Mockito.mock;

/**
 * 支付控制器測試配置
 * 提供測試所需的模擬依賴
 */
@TestConfiguration
public class PaymentControllerTestConfig {

    @Bean
    @Primary
    public PaymentManagementUseCase paymentManagementUseCase() {
        return mock(PaymentManagementUseCase.class);
    }
}