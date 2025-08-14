package solid.humank.genaidemo.infrastructure.customer.persistence;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.customer.model.valueobject.Address;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerName;
import solid.humank.genaidemo.domain.customer.model.valueobject.Email;
import solid.humank.genaidemo.domain.customer.model.valueobject.MembershipLevel;
import solid.humank.genaidemo.domain.customer.model.valueobject.Phone;
import solid.humank.genaidemo.domain.customer.repository.CustomerRepository;
import solid.humank.genaidemo.infrastructure.order.persistence.repository.JpaOrderRepository;

/** 客戶儲存庫 JPA 適配器 - 使用 JPA 替代原生 SQL 符合六角形架構的基礎設施層實現，實現領域層定義的 CustomerRepository 接口 */
@Repository
public class CustomerRepositoryJpaAdapter implements CustomerRepository {

    private final JpaOrderRepository orderRepository;

    public CustomerRepositoryJpaAdapter(JpaOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        if (!existsById(customerId)) {
            return Optional.empty();
        }

        // 根據客戶ID生成客戶信息（模擬數據）
        Customer customer = generateCustomer(customerId);
        return Optional.of(customer);
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        // 這是一個簡化實現，實際應該有專門的客戶表
        // 目前基於現有數據結構的限制，返回空
        return Optional.empty();
    }

    @Override
    public List<Customer> findByMembershipLevel(MembershipLevel level) {
        // 簡化實現，返回空列表
        return new ArrayList<>();
    }

    @Override
    public List<Customer> findByBirthMonth(int month) {
        // 簡化實現，返回空列表
        return new ArrayList<>();
    }

    @Override
    public List<Customer> findNewMembers(LocalDate since) {
        // 簡化實現，返回空列表
        return new ArrayList<>();
    }

    @Override
    public List<Customer> findAll() {
        List<String> customerIdStrings = orderRepository.findDistinctCustomerIds();
        return customerIdStrings.stream()
                .map(CustomerId::new)
                .map(this::generateCustomer)
                .collect(Collectors.toList());
    }

    @Override
    public Customer save(Customer customer) {
        // 在這個簡化實現中，我們不實際保存客戶數據
        // 因為客戶數據是基於訂單數據生成的
        return customer;
    }

    @Override
    public void delete(Customer customer) {
        deleteById(customer.getId());
    }

    @Override
    public void deleteById(CustomerId customerId) {
        // 在這個簡化實現中，我們不實際刪除客戶數據
        // 因為客戶數據是基於訂單數據生成的
    }

    @Override
    public void delete(CustomerId customerId) {
        deleteById(customerId);
    }

    @Override
    public long count() {
        return orderRepository.countDistinctCustomers();
    }

    @Override
    public boolean existsById(CustomerId customerId) {
        // 使用 JPA 查詢方法替代原生 SQL
        return orderRepository.existsByCustomerId(customerId.getId());
    }

    // 以下是為了支持應用層的額外方法
    public List<CustomerId> findAllCustomerIds() {
        // 使用 JPA 查詢方法替代原生 SQL
        List<String> customerIdStrings = orderRepository.findDistinctCustomerIds();
        return customerIdStrings.stream().map(CustomerId::new).collect(Collectors.toList());
    }

    @Override
    public List<CustomerId> findCustomerIds(int page, int size) {
        List<CustomerId> allIds = findAllCustomerIds();
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, allIds.size());

        if (startIndex >= allIds.size()) {
            return new ArrayList<>();
        }

        return allIds.subList(startIndex, endIndex);
    }

    @Override
    public int countCustomers() {
        // 使用 JPA 查詢方法替代原生 SQL
        return (int) orderRepository.countDistinctCustomers();
    }

    /** 根據客戶ID生成客戶信息（模擬數據） 這個方法保持不變，因為它是業務邏輯而非數據訪問邏輯 */
    private Customer generateCustomer(CustomerId customerId) {
        String customerIdValue = customerId.getId();

        CustomerName name = new CustomerName(generateCustomerName(customerIdValue));
        Email email = new Email(generateEmail(customerIdValue));
        Phone phone = new Phone(generatePhone(customerIdValue));
        Address address = generateAddress(customerIdValue);
        MembershipLevel membershipLevel = generateMembershipLevel(customerIdValue);

        return new Customer(customerId, name, email, phone, address, membershipLevel);
    }

    private String generateCustomerName(String customerId) {
        String[] surnames = {"張", "李", "王", "陳", "林", "黃", "吳", "劉", "蔡", "楊"};
        String[] givenNames = {"小明", "小華", "大明", "美麗", "志偉", "淑芬", "建國", "雅婷", "俊傑", "怡君"};

        int surnameIndex = Math.abs(customerId.hashCode()) % surnames.length;
        int givenNameIndex = Math.abs((customerId + "name").hashCode()) % givenNames.length;

        return surnames[surnameIndex] + givenNames[givenNameIndex];
    }

    private String generateEmail(String customerId) {
        String name = generateCustomerName(customerId);
        String[] domains = {"gmail.com", "yahoo.com", "hotmail.com", "email.com"};
        int domainIndex = Math.abs(customerId.hashCode()) % domains.length;

        String pinyin = convertToPinyin(name);
        return pinyin.toLowerCase() + "@" + domains[domainIndex];
    }

    private String generatePhone(String customerId) {
        int phoneNumber = Math.abs(customerId.hashCode()) % 100000000;
        return String.format("09%08d", phoneNumber);
    }

    private Address generateAddress(String customerId) {
        String[] cities = {
            "台北市信義區信義路五段7號",
            "新北市板橋區中山路一段161號",
            "桃園市中壢區中正路123號",
            "台中市西屯區台灣大道三段99號",
            "高雄市前金區中正四路211號",
            "台南市東區東門路二段89號"
        };
        String fullAddress = cities[Math.abs(customerId.hashCode()) % cities.length];

        // 簡化地址解析
        String[] parts = fullAddress.split("區");
        if (parts.length >= 2) {
            String cityDistrict = parts[0] + "區";
            String street = parts[1];
            return new Address(cityDistrict, "", street, "");
        } else {
            return new Address(fullAddress, "", "", "");
        }
    }

    private MembershipLevel generateMembershipLevel(String customerId) {
        MembershipLevel[] levels = MembershipLevel.values();
        return levels[Math.abs(customerId.hashCode()) % levels.length];
    }

    private String convertToPinyin(String chinese) {
        // 簡化的中文轉拼音實現
        return chinese.replaceAll("[\\u4e00-\\u9fa5]", "user");
    }
}
