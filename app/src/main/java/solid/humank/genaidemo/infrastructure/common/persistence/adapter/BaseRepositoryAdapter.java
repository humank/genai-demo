package solid.humank.genaidemo.infrastructure.common.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import solid.humank.genaidemo.domain.common.repository.Repository;

/**
 * 基礎儲存庫適配器抽象類別
 * 提供通用的 Repository Pattern 實作，減少重複代碼
 * 
 * @param <T>   聚合根類型
 * @param <ID>  聚合根ID類型
 * @param <E>   JPA實體類型
 * @param <JID> JPA實體ID類型
 */
@Component
public abstract class BaseRepositoryAdapter<T, ID, E, JID> implements Repository<T, ID> {

    protected final JpaRepository<E, JID> jpaRepository;

    protected BaseRepositoryAdapter(JpaRepository<E, JID> jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public T save(T aggregateRoot) {
        if (aggregateRoot == null) {
            throw new IllegalArgumentException("Aggregate root cannot be null");
        }

        E entity = toJpaEntity(aggregateRoot);
        jpaRepository.save(entity);

        // Return the original aggregate root to maintain aggregate consistency
        // The aggregate root should be the single source of truth
        return aggregateRoot;
    }

    @Override
    public Optional<T> findById(ID id) {
        JID jpaId = convertToJpaId(id);
        return jpaRepository.findById(jpaId).map(this::toDomainModel);
    }

    @Override
    public List<T> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomainModel)
                .toList();
    }

    @Override
    @Transactional
    public void delete(T aggregateRoot) {
        if (aggregateRoot == null) {
            throw new IllegalArgumentException("Aggregate root cannot be null");
        }
        ID id = extractId(aggregateRoot);
        deleteById(id);
    }

    @Override
    @Transactional
    public void deleteById(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        JID jpaId = convertToJpaId(id);
        jpaRepository.deleteById(jpaId);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public boolean existsById(ID id) {
        JID jpaId = convertToJpaId(id);
        return jpaRepository.existsById(jpaId);
    }

    /**
     * 將聚合根轉換為JPA實體
     * 
     * @param aggregateRoot 聚合根
     * @return JPA實體
     */
    protected abstract E toJpaEntity(T aggregateRoot);

    /**
     * 將JPA實體轉換為聚合根
     * 
     * @param entity JPA實體
     * @return 聚合根
     */
    protected abstract T toDomainModel(E entity);

    /**
     * 將領域ID轉換為JPA ID
     * 
     * @param domainId 領域ID
     * @return JPA ID
     */
    protected abstract JID convertToJpaId(ID domainId);

    /**
     * 從聚合根中提取ID
     * 
     * @param aggregateRoot 聚合根
     * @return ID
     */
    protected abstract ID extractId(T aggregateRoot);
}