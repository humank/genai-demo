package solid.humank.genaidemo.utils;

import java.util.Objects;
import java.util.logging.Level;
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
    private static volatile ApplicationContext applicationContext;
    
    // 用於檢測是否已初始化
    private static volatile boolean initialized = false;

    /**
     * 設置應用上下文
     * 
     * @param applicationContext Spring 應用上下文
     * @throws BeansException 如果設置過程中發生錯誤
     */
    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        Objects.requireNonNull(applicationContext, "ApplicationContext must not be null");
        
        synchronized (SpringContextHolder.class) {
            SpringContextHolder.applicationContext = applicationContext;
            initialized = true;
        }
        
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info(() -> String.format(
                "SpringContextHolder initialized with ApplicationContext: %s", 
                applicationContext.getDisplayName()
            ));
        }
    }
    
    /**
     * 初始化後的處理
     */
    @PostConstruct
    public void init() {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("SpringContextHolder bean created");
        }
    }

    /**
     * 獲取 Spring Bean
     * 
     * @param <T> Bean類型
     * @param clazz Bean類
     * @return Bean實例
     * @throws IllegalStateException 如果 ApplicationContext 未初始化
     * @throws IllegalArgumentException 如果 clazz 為 null
     */
    public static <T> T getBean(Class<T> clazz) {
        Objects.requireNonNull(clazz, "Bean class must not be null");
        checkApplicationContext();
        
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(() -> String.format("Getting bean of type: %s", clazz.getName()));
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
     * @throws IllegalStateException 如果 ApplicationContext 未初始化
     * @throws IllegalArgumentException 如果 name 或 clazz 為 null
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        Objects.requireNonNull(name, "Bean name must not be null");
        Objects.requireNonNull(clazz, "Bean class must not be null");
        checkApplicationContext();
        
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(() -> String.format("Getting bean with name: %s and type: %s", name, clazz.getName()));
        }
        
        return applicationContext.getBean(name, clazz);
    }
    
    /**
     * 檢查 ApplicationContext 是否已初始化
     * 
     * @throws IllegalStateException 如果 ApplicationContext 未初始化
     */
    private static void checkApplicationContext() {
        if (applicationContext == null) {
            LOGGER.severe("ApplicationContext has not been initialized");
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
        synchronized (SpringContextHolder.class) {
            applicationContext = null;
            initialized = false;
        }
        
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("SpringContextHolder context cleared");
        }
    }
}
