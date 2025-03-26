package solid.humank.genaidemo.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring Context 持有者
 * 用於在非Spring管理的類中獲取Spring Bean
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextHolder.applicationContext = applicationContext;
    }

    /**
     * 獲取 Spring Bean
     * 
     * @param <T> Bean類型
     * @param clazz Bean類
     * @return Bean實例
     */
    public static <T> T getBean(Class<T> clazz) {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext has not been initialized");
        }
        return applicationContext.getBean(clazz);
    }

    /**
     * 獲取 Spring Bean
     * 
     * @param <T> Bean類型
     * @param name Bean名稱
     * @param clazz Bean類
     * @return Bean實例
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext has not been initialized");
        }
        return applicationContext.getBean(name, clazz);
    }

    /**
     * 清理上下文
     * 主要用於單元測試
     */
    public static void clearContext() {
        applicationContext = null;
    }
}
