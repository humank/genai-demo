package solid.humank.genaidemo.application.customer.service;

import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.customer.repository.CustomerRepository;
import solid.humank.genaidemo.application.customer.CustomerDto;
import solid.humank.genaidemo.application.customer.CustomerPageDto;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * 客戶應用服務
 */
@Service
public class CustomerApplicationService {
    
    private final CustomerRepository customerRepository;
    
    public CustomerApplicationService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    
    /**
     * 獲取客戶詳情
     */
    public Optional<CustomerDto> getCustomer(String customerId) {
        CustomerId id = new CustomerId(customerId);
        Optional<Customer> customer = customerRepository.findById(id);
        return customer.map(this::toDto);
    }
    
    /**
     * 分頁獲取客戶列表
     */
    public CustomerPageDto getCustomers(int page, int size) {
        List<CustomerId> customerIds = customerRepository.findCustomerIds(page, size);
        int totalElements = customerRepository.countCustomers();
        
        List<CustomerDto> customers = customerIds.stream()
                .map(customerRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::toDto)
                .toList();
        
        return new CustomerPageDto(
                customers,
                totalElements,
                (int) Math.ceil((double) totalElements / size),
                size,
                page,
                page == 0,
                page >= Math.ceil((double) totalElements / size) - 1
        );
    }
    
    /**
     * 檢查客戶是否存在
     */
    public boolean customerExists(String customerId) {
        CustomerId id = new CustomerId(customerId);
        return customerRepository.existsById(id);
    }
    
    private CustomerDto toDto(Customer customer) {
        return new CustomerDto(
                customer.getId().getId(),
                customer.getName().getName(),
                customer.getEmail().getEmail(),
                customer.getPhone().getPhone(),
                customer.getAddress().getFullAddress(),
                customer.getMembershipLevel().name()
        );
    }
}