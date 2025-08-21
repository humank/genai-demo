package solid.humank.genaidemo.domain.common.specification;

import solid.humank.genaidemo.domain.common.annotations.Specification;

@Specification(name = "NotSpecification", description = "Negates a specification with logical NOT operation")
public class NotSpecification<T> implements solid.humank.genaidemo.domain.common.specification.Specification<T> {
    private final solid.humank.genaidemo.domain.common.specification.Specification<T> spec;

    public NotSpecification(solid.humank.genaidemo.domain.common.specification.Specification<T> spec) {
        this.spec = spec;
    }

    @Override
    public boolean isSatisfiedBy(T candidate) {
        return !spec.isSatisfiedBy(candidate);
    }
}
