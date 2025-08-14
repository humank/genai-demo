package solid.humank.genaidemo.domain.customer.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import solid.humank.genaidemo.domain.common.repository.Repository;
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.customer.model.valueobject.MembershipLevel;

public interface CustomerRepository extends Repository<Customer, CustomerId> {
    @Override
    Optional<Customer> findById(CustomerId customerId);

    Optional<Customer> findByEmail(String email);

    List<Customer> findByMembershipLevel(MembershipLevel level);

    List<Customer> findByBirthMonth(int month);

    List<Customer> findNewMembers(LocalDate since);

    @Override
    Customer save(Customer customer);

    void delete(CustomerId customerId);

    // 分頁查詢方法
    List<CustomerId> findCustomerIds(int page, int size);

    int countCustomers();
}
