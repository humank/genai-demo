# 整合測試指南

## 概述

整合測試驗證多個組件之間的互動和協作。本指南提供了專案中整合測試的最佳實踐和實作範例。

## 🔗 整合測試類型

### 1. 資料庫整合測試

使用 `@DataJpaTest` 測試 Repository 層：

```java
@DataJpaTest
@ActiveProfiles("test")
class CustomerRepositoryIntegrationTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    void should_find_customers_by_membership_level() {
        // Given
        Customer premiumCustomer = CustomerTestBuilder.aCustomer()
            .withMembershipLevel(MembershipLevel.PREMIUM)
            .build();
        Customer regularCustomer = CustomerTestBuilder.aCustomer()
            .withMembershipLevel(MembershipLevel.REGULAR)
            .build();
        
        entityManager.persistAndFlush(premiumCustomer);
        entityManager.persistAndFlush(regularCustomer);
        
        // When
        List<Customer> premiumCustomers = customerRepository
            .findByMembershipLevel(MembershipLevel.PREMIUM);
        
        // Then
        assertThat(premiumCustomers).hasSize(1)
            .extracting(Customer::getMembershipLevel)
            .containsOnly(MembershipLevel.PREMIUM);
    }
}
```

### 2. Web 層整合測試

使用 `@WebMvcTest` 測試 Controller 層：

```java
@WebMvcTest(CustomerController.class)
class CustomerControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CustomerService customerService;
    
    @Test
    void should_return_customer_when_valid_id_provided() throws Exception {
        // Given
        Customer customer = CustomerTestBuilder.aCustomer().build();
        when(customerService.findById("123")).thenReturn(customer);
        
        // When & Then
        mockMvc.perform(get("/api/v1/customers/123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("123"))
            .andExpect(jsonPath("$.name").value(customer.getName()));
    }
}
```

### 3. 服務層整合測試

使用 `@SpringBootTest` 測試服務整合：

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class CustomerServiceIntegrationTest {
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    void should_create_customer_and_save_to_database() {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand(
            "John Doe", "john@example.com"
        );
        
        // When
        Customer customer = customerService.createCustomer(command);
        
        // Then
        assertThat(customer.getId()).isNotNull();
        
        Optional<Customer> savedCustomer = customerRepository.findById(customer.getId());
        assertThat(savedCustomer).isPresent();
        assertThat(savedCustomer.get().getName()).isEqualTo("John Doe");
    }
}
```

## 🎯 最佳實踐

### 1. 使用適當的測試切片

- `@DataJpaTest`: 僅載入 JPA 相關組件
- `@WebMvcTest`: 僅載入 Web 層組件  
- `@JsonTest`: 僅測試 JSON 序列化
- `@SpringBootTest`: 載入完整應用上下文

### 2. 測試資料管理

```java
@TestConfiguration
public class TestDataConfiguration {
    
    @Bean
    @Primary
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .addScript("classpath:schema-test.sql")
            .addScript("classpath:test-data.sql")
            .build();
    }
}
```

## 🔗 相關資源

- [測試策略總覽](README.md)
- [測試優化指南](test-optimization.md)
- [效能測試指南](performance-monitoring/README.md)

---

**最後更新**: 2025年1月21日  
**維護者**: QA Team  
**版本**: 1.0