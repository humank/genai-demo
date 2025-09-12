package solid.humank.genaidemo.testutils.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

/**
 * 單元測試標籤註解
 * 
 * 特徵：
 * - 記憶體使用：~5MB
 * - 執行時間：~50ms
 * - 不需要 Spring 上下文
 * - 使用 Mock 替代外部依賴
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Tag("unit")
@Tag("fast")
public @interface UnitTest {
}