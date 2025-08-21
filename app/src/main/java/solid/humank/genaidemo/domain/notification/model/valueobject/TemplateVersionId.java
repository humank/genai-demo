package solid.humank.genaidemo.domain.notification.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 模板版本ID值對象
 */
@ValueObject(name = "TemplateVersionId", description = "模板版本唯一標識符")
public record TemplateVersionId(UUID value) {

    public TemplateVersionId {
        Objects.requireNonNull(value, "Template version ID cannot be null");
    }

    public static TemplateVersionId generate() {
        return new TemplateVersionId(UUID.randomUUID());
    }

    public static TemplateVersionId of(String id) {
        return new TemplateVersionId(UUID.fromString(id));
    }

    public static TemplateVersionId of(UUID id) {
        return new TemplateVersionId(id);
    }

    public String getValue() {
        return value.toString();
    }
}