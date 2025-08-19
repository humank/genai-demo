package solid.humank.genaidemo.domain.common.specification;

import solid.humank.genaidemo.domain.common.annotations.Specification;

@Specification(name = "AndSpecification", description = "Combines two specifications with logical AND operation")
public class AndSpecification<T> implements solid.humank.genaidemo.domain.common.specification.Specification<T> {
    private final solid.humank.genaidemo.domain.common.specification.Specification<T> first;
    private final solid.humank.genaidemo.domain.common.specification.Specification<T> second;

    public AndSpecification(solid.humank.genaidemo.domain.common.specification.Specification<T> first,
            solid.humank.genaidemo.domain.common.specification.Specification<T> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean isSatisfiedBy(T candidate) {
        return first.isSatisfiedBy(candidate) && second.isSatisfiedBy(candidate);
    }
}
