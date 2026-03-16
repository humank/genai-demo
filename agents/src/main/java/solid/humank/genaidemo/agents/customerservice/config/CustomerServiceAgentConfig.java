package solid.humank.genaidemo.agents.customerservice.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智能客服 Agent 配置
 * 
 * 配置 Agent 相關的 Spring Bean 和排程任務。
 */
@Configuration
@ComponentScan(basePackages = {
    "solid.humank.genaidemo.agents.common",
    "solid.humank.genaidemo.agents.customerservice"
})
@EnableScheduling
public class CustomerServiceAgentConfig {
    
    // 未來可在此添加：
    // - Bedrock Client Bean (生產環境)
    // - DynamoDB Client Bean (生產環境)
    // - Metrics Configuration
    // - Rate Limiter Configuration
}
