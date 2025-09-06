package solid.humank.genaidemo.domain.common.valueobject;

import java.util.UUID;

/** 共享核心中的業務識別碼基礎類型 用於在不同 Bounded Context 之間共享通用的識別碼概念 */
public abstract class BusinessId {
    protected final UUID id;

    protected BusinessId(UUID id) {
        this.id = id;
    }

    protected BusinessId(String id) {
        this.id = UUID.fromString(id);
    }

    protected BusinessId() {
        this.id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    @Override
    public String toString() {
        return id.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BusinessId that = (BusinessId) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
