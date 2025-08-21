package solid.humank.genaidemo.domain.notification.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 模板使用統計ID值對象
 */
@ValueObject(name = "TemplateUsageStatisticsId", description = "模板使用統計唯一標識符")
public record TemplateUsageStatisticsId(UUID value) {

    public TemplateUsageStatisticsId {
        Objects.requireNonNull(value, "Template usage statistics ID cannot be null");
    }

    public static TemplateUsageStatisticsId generate() {
        return new TemplateUsageStatisticsId(UUID.randomUUID());
    }

    public static TemplateUsageStatisticsId of(String id) {
        return new TemplateUsageStatisticsId(UUID.fromString(id));
    }

    public static TemplateUsageStatisticsId of(UUID id) {
        return new TemplateUsageStatisticsId(id);
    }

    public String getValue() {
        return value.toString();
    }
}