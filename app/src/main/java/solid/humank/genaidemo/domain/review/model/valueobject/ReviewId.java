package solid.humank.genaidemo.domain.review.model.valueobject;

import java.util.Objects;
import java.util.UUID;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 評價ID值對象 */
@ValueObject
public class ReviewId {
    private final String id;

    public ReviewId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("評價ID不能為空");
        }
        this.id = id;
    }

    public static ReviewId generateNew() {
        return new ReviewId(UUID.randomUUID().toString());
    }

    public static ReviewId of(String id) {
        return new ReviewId(id);
    }

    public String value() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewId reviewId = (ReviewId) o;
        return Objects.equals(id, reviewId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
