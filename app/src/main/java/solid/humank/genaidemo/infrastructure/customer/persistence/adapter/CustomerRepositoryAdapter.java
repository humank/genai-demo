package solid.humank.genaidemo.infrastructure.customer.persistence.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.customer.model.valueobject.MembershipLevel;
import solid.humank.genaidemo.domain.customer.repository.CustomerRepository;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.infrastructure.common.persistence.adapter.BaseRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.customer.persistence.entity.JpaCustomerEntity;
import solid.humank.genaidemo.infrastructure.customer.persistence.mapper.CustomerMapper;
import solid.humank.genaidemo.infrastructure.customer.persistence.repository.JpaCustomerRepository;

/**
 * 客戶儲存庫適配器
 * 實現領域儲存庫接口，使用 JPA 進行持久化
 * 遵循六角架構的端口和適配器模式
 */
@Component
public class CustomerRepositoryAdapter
        extends BaseRepositoryAdapter<Customer, CustomerId, JpaCustomerEntity, String>
        implements CustomerRepository {

    private final JpaCustomerRepository jpaCustomerRepository;
    private final CustomerMapper customerMapper;

    public CustomerRepositoryAdapter(
            JpaCustomerRepository jpaCustomerRepository,
            CustomerMapper customerMapper) {
        super(jpaCustomerRepository);
        this.jpaCustomerRepository = jpaCustomerRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        return jpaCustomerRepository.findById(customerId.getValue())
                .map(customerMapper::toDomainModel);
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return jpaCustomerRepository.findByEmail(email)
                .map(customerMapper::toDomainModel);
    }

    @Override
    public List<Customer> findByMembershipLevel(MembershipLevel level) {
        return jpaCustomerRepository.findByMembershipLevel(level.name())
                .stream()
                .map(customerMapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Customer> findByBirthMonth(int month) {
        return jpaCustomerRepository.findByBirthMonth(month)
                .stream()
                .map(customerMapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Customer> findNewMembers(LocalDate since) {
        return jpaCustomerRepository.findNewMembersSince(since)
                .stream()
                .map(customerMapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public Customer save(Customer customer) {
        JpaCustomerEntity entity = customerMapper.toJpaEntity(customer);
        JpaCustomerEntity savedEntity = jpaCustomerRepository.save(entity);
        return customerMapper.toDomainModel(savedEntity);
    }

    @Override
    public void delete(CustomerId customerId) {
        jpaCustomerRepository.deleteById(customerId.getValue());
    }

    @Override
    public List<CustomerId> findCustomerIds(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jpaCustomerRepository.findCustomerIds((int) pageable.getOffset(), size)
                .stream()
                .map(CustomerId::of)
                .collect(Collectors.toList());
    }

    @Override
    public int countCustomers() {
        return (int) jpaCustomerRepository.countCustomers();
    }

    // BaseRepositoryAdapter required methods
    @Override
    protected JpaCustomerEntity toJpaEntity(Customer aggregateRoot) {
        return customerMapper.toJpaEntity(aggregateRoot);
    }

    @Override
    protected Customer toDomainModel(JpaCustomerEntity entity) {
        return customerMapper.toDomainModel(entity);
    }

    @Override
    protected String convertToJpaId(CustomerId domainId) {
        return domainId.getValue();
    }

    @Override
    protected CustomerId extractId(Customer aggregateRoot) {
        return aggregateRoot.getId();
    }
}