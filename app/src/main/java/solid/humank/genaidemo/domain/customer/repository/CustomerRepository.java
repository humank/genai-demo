package solid.humank.genaidemo.domain.customer.repository;

import solid.humank.genaidemo.domain.common.repository.Repository;
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.customer.model.valueobject.MembershipLevel;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
}