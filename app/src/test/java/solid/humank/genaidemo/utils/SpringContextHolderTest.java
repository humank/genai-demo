package solid.humank.genaidemo.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

class SpringContextHolderTest {

    private SpringContextHolder springContextHolder;
    
    @Mock
    private ApplicationContext applicationContext;
    
    @Mock
    private TestBean testBean;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        springContextHolder = new SpringContextHolder();
        
        // 清理上下文，確保測試獨立性
        SpringContextHolder.clearContext();
    }
    
    @AfterEach
    void tearDown() {
        // 清理上下文，確保測試獨立性
        SpringContextHolder.clearContext();
    }
    
    @Test
    void testSetApplicationContext() {
        // 執行
        springContextHolder.setApplicationContext(applicationContext);
        
        // 驗證
        assertTrue(SpringContextHolder.isInitialized());
    }
    
    @Test
    void testGetBean() {
        // 準備
        springContextHolder.setApplicationContext(applicationContext);
        when(applicationContext.getBean(TestBean.class)).thenReturn(testBean);
        
        // 執行
        TestBean result = SpringContextHolder.getBean(TestBean.class);
        
        // 驗證
        assertSame(testBean, result);
        verify(applicationContext).getBean(TestBean.class);
    }
    
    @Test
    void testGetBeanWithName() {
        // 準備
        springContextHolder.setApplicationContext(applicationContext);
        when(applicationContext.getBean("testBean", TestBean.class)).thenReturn(testBean);
        
        // 執行
        TestBean result = SpringContextHolder.getBean("testBean", TestBean.class);
        
        // 驗證
        assertSame(testBean, result);
        verify(applicationContext).getBean("testBean", TestBean.class);
    }
    
    @Test
    void testGetBeanWithoutInitialization() {
        // 驗證拋出異常
        assertThrows(IllegalStateException.class, () -> 
            SpringContextHolder.getBean(TestBean.class)
        );
    }
    
    @Test
    void testClearContext() {
        // 準備
        springContextHolder.setApplicationContext(applicationContext);
        assertTrue(SpringContextHolder.isInitialized());
        
        // 執行
        SpringContextHolder.clearContext();
        
        // 驗證
        assertFalse(SpringContextHolder.isInitialized());
        assertThrows(IllegalStateException.class, () -> 
            SpringContextHolder.getBean(TestBean.class)
        );
    }
    
    // 測試用的 Bean 類
    static class TestBean {}
}