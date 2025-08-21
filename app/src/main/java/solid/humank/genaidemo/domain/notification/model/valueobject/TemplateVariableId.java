package solid.humank.genaidemo.domain.notification.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 模板變數ID值對象
 */
@ValueObject(name = "TemplateVariableId", description = "模板變數唯一標識符")
public record TemplateVariableId(UUID value) {

    public TemplateVariableId {
        Objects.requireNonNull(value, "Template variable ID cannot be null");
    }

    public static TemplateVariableId generate() {
        return new TemplateVariableId(UUID.randomUUID());
    }

    public static TemplateVariableId of(String id) {
        return new TemplateVariableId(UUID.fromString(id));
    }

    public static TemplateVariableId of(UUID id) {
        return new TemplateVariableId(id);
    }

    public String getValue() {
        return value.toString();
    }
}