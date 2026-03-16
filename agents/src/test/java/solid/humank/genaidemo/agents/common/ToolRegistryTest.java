package solid.humank.genaidemo.agents.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ToolRegistry 單元測試
 */
@DisplayName("ToolRegistry 單元測試")
class ToolRegistryTest {

    private ToolRegistry toolRegistry;

    @BeforeEach
    void setUp() {
        toolRegistry = new ToolRegistry(Collections.emptyList());
    }

    @Test
    @DisplayName("應該成功註冊工具")
    void should_register_tool_successfully() {
        // Given
        AgentTool mockTool = createMockTool("order-query", "查詢訂單狀態");

        // When
        toolRegistry.register(mockTool);

        // Then
        assertThat(toolRegistry.getTool("order-query")).isPresent();
    }

    @Test
    @DisplayName("應該返回所有已註冊的工具")
    void should_return_all_registered_tools() {
        // Given
        toolRegistry.register(createMockTool("tool-1", "工具1"));
        toolRegistry.register(createMockTool("tool-2", "工具2"));

        // When
        var tools = toolRegistry.getAvailableTools();

        // Then
        assertThat(tools).hasSize(2);
    }

    @Test
    @DisplayName("當工具不存在時應該返回空")
    void should_return_empty_when_tool_not_found() {
        // When
        Optional<AgentTool> result = toolRegistry.getTool("non-existent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("應該成功執行已註冊的工具")
    void should_execute_registered_tool() {
        // Given
        AgentTool mockTool = createMockTool("test-tool", "測試工具");
        toolRegistry.register(mockTool);

        // When
        ToolResult result = toolRegistry.executeTool("test-tool", Map.of("param", "value"));

        // Then
        assertThat(result.success()).isTrue();
    }

    @Test
    @DisplayName("執行不存在的工具應該返回錯誤")
    void should_return_error_when_executing_non_existent_tool() {
        // When
        ToolResult result = toolRegistry.executeTool("non-existent", Map.of());

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Tool not found");
    }

    private AgentTool createMockTool(String name, String description) {
        return new AgentTool() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public ToolSchema getSchema() {
                return ToolSchema.builder().build();
            }

            @Override
            public ToolResult execute(Map<String, Object> parameters) {
                return ToolResult.success("執行成功", Map.of("result", "mock"));
            }
        };
    }
}
