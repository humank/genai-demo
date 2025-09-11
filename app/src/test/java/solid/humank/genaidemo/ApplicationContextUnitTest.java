package solid.humank.genaidemo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 輕量級單元測試 - Application Context
 * 
 * 記憶體使用：~5MB (vs @SpringBootTest ~500MB)
 * 執行時間：~10ms (vs @SpringBootTest ~2s)
 * 
 * 測試應用程序基本結構，而不是實際的 Spring 上下文啟動
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Application Context Unit Tests")
class ApplicationContextUnitTest {

    @Test
    @DisplayName("Should validate application main class exists")
    void shouldValidateApplicationMainClassExists() {
        // When & Then
        assertThat(GenAiDemoApplication.class).isNotNull();
        assertThat(GenAiDemoApplication.class.getSimpleName()).isEqualTo("GenAiDemoApplication");
        assertThat(GenAiDemoApplication.class.getPackageName()).isEqualTo("solid.humank.genaidemo");
    }

    @Test
    @DisplayName("Should validate application has main method")
    void shouldValidateApplicationHasMainMethod() throws NoSuchMethodException {
        // When
        var mainMethod = GenAiDemoApplication.class.getMethod("main", String[].class);

        // Then
        assertThat(mainMethod).isNotNull();
        assertThat(mainMethod.getReturnType()).isEqualTo(void.class);
        assertThat(mainMethod.getParameterCount()).isEqualTo(1);
        assertThat(mainMethod.getParameterTypes()[0]).isEqualTo(String[].class);
    }

    @Test
    @DisplayName("Should validate application class annotations")
    void shouldValidateApplicationClassAnnotations() {
        // When
        var annotations = GenAiDemoApplication.class.getAnnotations();

        // Then
        assertThat(annotations).isNotEmpty();

        boolean hasSpringBootApplication = false;
        for (var annotation : annotations) {
            if (annotation.annotationType().getSimpleName().equals("SpringBootApplication")) {
                hasSpringBootApplication = true;
                break;
            }
        }
        assertThat(hasSpringBootApplication).isTrue();
    }

    @Test
    @DisplayName("Should validate package structure")
    void shouldValidatePackageStructure() {
        // Given
        String expectedPackage = "solid.humank.genaidemo";

        // When
        Package applicationPackage = GenAiDemoApplication.class.getPackage();

        // Then
        assertThat(applicationPackage).isNotNull();
        assertThat(applicationPackage.getName()).isEqualTo(expectedPackage);
    }

    @Test
    @DisplayName("Should validate application class is public")
    void shouldValidateApplicationClassIsPublic() {
        // When
        int modifiers = GenAiDemoApplication.class.getModifiers();

        // Then
        assertThat(java.lang.reflect.Modifier.isPublic(modifiers)).isTrue();
        assertThat(java.lang.reflect.Modifier.isFinal(modifiers)).isFalse(); // Spring Boot 應用不應該是 final
    }

    @Test
    @DisplayName("Should validate application can be instantiated")
    void shouldValidateApplicationCanBeInstantiated() {
        // When & Then - 驗證類可以被實例化（有預設構造函數）
        try {
            var constructor = GenAiDemoApplication.class.getDeclaredConstructor();
            assertThat(constructor).isNotNull();

            var instance = constructor.newInstance();
            assertThat(instance).isNotNull();
            assertThat(instance).isInstanceOf(GenAiDemoApplication.class);
        } catch (Exception e) {
            throw new AssertionError("Application class should be instantiable", e);
        }
    }
}