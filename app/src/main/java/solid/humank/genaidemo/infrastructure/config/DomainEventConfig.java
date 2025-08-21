package solid.humank.genaidemo.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/** 領域事件配置類 啟用 AspectJ 自動代理，支援 AOP 攔截器 */
@Configuration
@EnableAspectJAutoProxy
public class DomainEventConfig {
    // 配置類，主要用於啟用 AOP
}
