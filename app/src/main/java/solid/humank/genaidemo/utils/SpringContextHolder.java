package solid.humank.genaidemo.utils;

import java.util.logging.Logger;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Spring Context 持有者
 * 用於在非Spring管理的類中獲取Spring Bean
 * 
 * 注意：這個類使用靜態變數存儲 ApplicationContext，
 * 這是一個特例，因為它的目的就是提供靜態訪問 Spring 上下文的能力。
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {
    private static final Logger LOGGER = Logger.getLogger(SpringContextHolder.class.getName());
    private static ApplicationContext applicationContext;
    
    // 用於檢測是否已初始化
    private static boolean initialized = false;

    /**
     * 設置應用上下文
     * 
     * @param applicationContext Spring 應用上下文
     * @throws BeansException 如果設置過程中發生錯誤
     */
    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        SpringContextHolder.applicationContext = applicationContext;
        initialized = true;
        LOGGER.info("SpringContextHolder initialized with ApplicationContext: " + applicationContext.getDisplayName());
    }
    
    /**
     * 初始化後的處理
     */
    @PostConstruct
    public void init() {
        LOGGER.info("SpringContextHolder bean created");
    }

    /**
     * 獲取 Spring Bean
     * 
     * @param <T> Bean類型
     * @param clazz Bean類
     * @return Bean實例
     * @throws IllegalStateException 如果 ApplicationContext 未初始化
     */
    public static <T> T getBean(Class<T> clazz) {
        checkApplicationContext();
        return applicationContext.getBean(clazz);
    }

    /**
     * 獲取 Spring Bean
     * 
     * @param <T> Bean類型
     * @param name Bean名稱
     * @param clazz Bean類
     * @return Bean實例
     * @throws IllegalStateException 如果 ApplicationContext 未初始化
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        checkApplicationContext();
        return applicationContext.getBean(name, clazz);
    }
    
    /**
     * 檢查 ApplicationContext 是否已初始化
     * 
     * @throws IllegalStateException 如果 ApplicationContext 未初始化
     */
    private static void checkApplicationContext() {
        if (applicationContext == null) {
            throw new IllegalStateException(
                "ApplicationContext has not been initialized. " +
                "Make sure SpringContextHolder is properly registered as a Spring bean."
            );
        }
    }

    /**
     * 檢查是否已初始化
     * 
     * @return 是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 清理上下文
     * 主要用於單元測試
     */
    public static void clearContext() {
        applicationContext = null;
        initialized = false;
        LOGGER.info("SpringContextHolder context cleared");
    }
}
