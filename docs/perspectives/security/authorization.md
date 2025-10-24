# Authorization

> **Last Updated**: 2025-10-23  
> **Status**: ✅ Active

## Overview

This document describes the authorization mechanisms used in the e-commerce platform. Authorization determines what authenticated users are allowed to do - which resources they can access and what operations they can perform. The system implements Role-Based Access Control (RBAC) with fine-grained permissions.

## Authorization Model

### Role-Based Access Control (RBAC)

The system uses RBAC where:
- **Users** are assigned one or more **Roles**
- **Roles** have associated **Permissions**
- **Permissions** define access to specific resources and operations

### Roles

#### ADMIN
- **Description**: System administrators with full access
- **Permissions**: All operations on all resources
- **Use Cases**: System management, user management, configuration

#### CUSTOMER
- **Description**: Regular customers who purchase products
- **Permissions**: 
  - Read own profile and orders
  - Create orders and reviews
  - Update own profile
  - Cannot access other customers' data

#### SELLER
- **Description**: Sellers who list and manage products
- **Permissions**:
  - Manage own products and inventory
  - View own sales and orders
  - Cannot access other sellers' data

#### GUEST
- **Description**: Unauthenticated users
- **Permissions**:
  - Browse products
  - View public content
  - Cannot place orders or access user data

## Permission Model

### Permission Format

Permissions follow the format: `resource:operation`

Examples:
- `customer:read` - Read customer data
- `order:create` - Create orders
- `product:update` - Update products
- `admin:*` - All admin operations

### Permission Matrix

| Resource | ADMIN | CUSTOMER | SELLER | GUEST |
|----------|-------|----------|--------|-------|
| **Customer Profile** |
| Read Any | ✅ | ❌ (own only) | ❌ | ❌ |
| Read Own | ✅ | ✅ | ✅ | ❌ |
| Create | ✅ | ✅ (registration) | ✅ (registration) | ✅ (registration) |
| Update Own | ✅ | ✅ | ✅ | ❌ |
| Delete | ✅ | ❌ | ❌ | ❌ |
| **Orders** |
| Read Any | ✅ | ❌ | ❌ | ❌ |
| Read Own | ✅ | ✅ | ❌ | ❌ |
| Create | ✅ | ✅ | ❌ | ❌ |
| Update | ✅ | ❌ | ❌ | ❌ |
| Cancel Own | ✅ | ✅ (if pending) | ❌ | ❌ |
| **Products** |
| Read | ✅ | ✅ | ✅ | ✅ |
| Create | ✅ | ❌ | ✅ | ❌ |
| Update Any | ✅ | ❌ | ❌ | ❌ |
| Update Own | ✅ | ❌ | ✅ | ❌ |
| Delete | ✅ | ❌ | ❌ | ❌ |
| **Reviews** |
| Read | ✅ | ✅ | ✅ | ✅ |
| Create | ✅ | ✅ (purchased only) | ❌ | ❌ |
| Update Own | ✅ | ✅ | ❌ | ❌ |
| Delete Any | ✅ | ❌ | ❌ | ❌ |
| Delete Own | ✅ | ✅ | ❌ | ❌ |

## Implementation

### Method-Level Security

```java
@Configuration
@EnableGlobalMethodSecurity(
    prePostEnabled = true,
    securedEnabled = true,
    jsr250Enabled = true
)
public class MethodSecurityConfiguration {
    
    @Bean
    public PermissionEvaluator permissionEvaluator() {
        return new CustomPermissionEvaluator();
    }
}
```

### Using @PreAuthorize

```java
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {
    
    /**
     * Admin can access any customer, users can only access their own data
     */
    @GetMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or #customerId == authentication.principal.customerId")
    public ResponseEntity<CustomerResponse> getCustomer(
            @PathVariable String customerId) {
        
        Customer customer = customerService.findById(customerId);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }
    
    /**
     * Only admins can list all customers
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CustomerResponse>> listCustomers(
            Pageable pageable) {
        
        Page<Customer> customers = customerService.findAll(pageable);
        return ResponseEntity.ok(customers.map(CustomerResponse::from));
    }
    
    /**
     * Users can update their own profile
     */
    @PutMapping("/{customerId}")
    @PreAuthorize("#customerId == authentication.principal.customerId")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable String customerId,
            @Valid @RequestBody UpdateCustomerRequest request) {
        
        Customer customer = customerService.updateCustomer(customerId, request);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }
}
```

### Custom Permission Evaluator

```java
@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {
    
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    
    @Override
    public boolean hasPermission(
            Authentication authentication,
            Object targetDomainObject,
            Object permission) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        String permissionString = permission.toString();
        
        if (targetDomainObject instanceof Order order) {
            return evaluateOrderPermission(authentication, order, permissionString);
        }
        
        if (targetDomainObject instanceof Product product) {
            return evaluateProductPermission(authentication, product, permissionString);
        }
        
        return false;
    }
    
    @Override
    public boolean hasPermission(
            Authentication authentication,
            Serializable targetId,
            String targetType,
            Object permission) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return switch (targetType) {
            case "Order" -> evaluateOrderPermissionById(
                authentication, targetId.toString(), permission.toString()
            );
            case "Product" -> evaluateProductPermissionById(
                authentication, targetId.toString(), permission.toString()
            );
            default -> false;
        };
    }
    
    private boolean evaluateOrderPermission(
            Authentication auth,
            Order order,
            String permission) {
        
        String userId = auth.getName();
        
        // Admins have all permissions
        if (hasRole(auth, "ADMIN")) {
            return true;
        }
        
        return switch (permission) {
            case "READ" -> order.getCustomerId().equals(userId);
            case "CANCEL" -> order.getCustomerId().equals(userId) 
                && order.getStatus() == OrderStatus.PENDING;
            case "UPDATE", "DELETE" -> false; // Only admins
            default -> false;
        };
    }
    
    private boolean evaluateProductPermission(
            Authentication auth,
            Product product,
            String permission) {
        
        String userId = auth.getName();
        
        // Admins have all permissions
        if (hasRole(auth, "ADMIN")) {
            return true;
        }
        
        // Sellers can manage their own products
        if (hasRole(auth, "SELLER")) {
            return switch (permission) {
                case "READ" -> true;
                case "UPDATE", "DELETE" -> product.getSellerId().equals(userId);
                default -> false;
            };
        }
        
        // Customers can only read
        return "READ".equals(permission);
    }
    
    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
```

### Using Custom Permissions

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    
    /**
     * Check permission on loaded domain object
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasPermission(#orderId, 'Order', 'READ')")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        Order order = orderService.findById(orderId);
        return ResponseEntity.ok(OrderResponse.from(order));
    }
    
    /**
     * Check permission after loading object
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderId) {
        Order order = orderService.findById(orderId);
        
        // Check permission on actual object
        if (!hasPermission(order, "CANCEL")) {
            throw new AccessDeniedException("Cannot cancel this order");
        }
        
        orderService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }
}
```

### Service-Level Authorization

```java
@Service
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final SecurityContext securityContext;
    
    /**
     * Enforce authorization at service level
     */
    public Order findById(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Verify user has permission to access this order
        String currentUserId = securityContext.getCurrentUserId();
        if (!order.getCustomerId().equals(currentUserId) 
                && !securityContext.hasRole("ADMIN")) {
            throw new AccessDeniedException(
                "User does not have permission to access this order"
            );
        }
        
        return order;
    }
    
    /**
     * Filter results based on user permissions
     */
    public Page<Order> findOrders(Pageable pageable) {
        String currentUserId = securityContext.getCurrentUserId();
        
        // Admins see all orders
        if (securityContext.hasRole("ADMIN")) {
            return orderRepository.findAll(pageable);
        }
        
        // Customers see only their own orders
        return orderRepository.findByCustomerId(currentUserId, pageable);
    }
}
```

## Data Filtering

### Row-Level Security

```java
@Component
public class DataFilteringAspect {
    
    /**
     * Automatically filter query results based on user permissions
     */
    @Around("@annotation(FilterByUser)")
    public Object filterResults(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        
        if (result instanceof List<?> list) {
            return filterList(list);
        }
        
        if (result instanceof Page<?> page) {
            return filterPage(page);
        }
        
        return result;
    }
    
    private List<?> filterList(List<?> list) {
        String currentUserId = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        
        return list.stream()
            .filter(item -> canAccess(item, currentUserId))
            .collect(Collectors.toList());
    }
    
    private boolean canAccess(Object item, String userId) {
        if (item instanceof OwnedByUser ownedItem) {
            return ownedItem.getOwnerId().equals(userId);
        }
        return true;
    }
}
```

### Field-Level Security

```java
@Component
public class FieldMaskingService {
    
    /**
     * Mask sensitive fields based on user role
     */
    public CustomerDto maskSensitiveFields(Customer customer, String userRole) {
        CustomerDto dto = CustomerDto.from(customer);
        
        // Non-admins cannot see full email and phone
        if (!"ADMIN".equals(userRole)) {
            dto.setEmail(maskEmail(dto.getEmail()));
            dto.setPhoneNumber(maskPhoneNumber(dto.getPhoneNumber()));
        }
        
        return dto;
    }
    
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return "*".repeat(localPart.length()) + "@" + domain;
        }
        
        return localPart.charAt(0) 
            + "*".repeat(localPart.length() - 2) 
            + localPart.charAt(localPart.length() - 1) 
            + "@" + domain;
    }
}
```

## Authorization Patterns

### Resource Ownership

```java
public interface OwnedResource {
    String getOwnerId();
    
    default boolean isOwnedBy(String userId) {
        return getOwnerId().equals(userId);
    }
}

@Entity
public class Order implements OwnedResource {
    
    @Column(name = "customer_id")
    private String customerId;
    
    @Override
    public String getOwnerId() {
        return customerId;
    }
}
```

### Hierarchical Permissions

```java
public enum Permission {
    // Admin permissions (highest level)
    ADMIN_ALL("admin:*"),
    
    // Customer management
    CUSTOMER_READ_ALL("customer:read:all"),
    CUSTOMER_READ_OWN("customer:read:own"),
    CUSTOMER_UPDATE_OWN("customer:update:own"),
    
    // Order management
    ORDER_READ_ALL("order:read:all"),
    ORDER_READ_OWN("order:read:own"),
    ORDER_CREATE("order:create"),
    ORDER_CANCEL_OWN("order:cancel:own"),
    
    // Product management
    PRODUCT_READ("product:read"),
    PRODUCT_CREATE("product:create"),
    PRODUCT_UPDATE_OWN("product:update:own"),
    PRODUCT_DELETE_OWN("product:delete:own");
    
    private final String permission;
    
    Permission(String permission) {
        this.permission = permission;
    }
    
    public boolean implies(Permission other) {
        // ADMIN_ALL implies all other permissions
        if (this == ADMIN_ALL) {
            return true;
        }
        
        // Check if this permission implies the other
        String thisBase = permission.split(":")[0];
        String otherBase = other.permission.split(":")[0];
        
        return thisBase.equals(otherBase) && permission.endsWith("*");
    }
}
```

## Security Context

### Accessing Current User

```java
@Component
public class SecurityContextService {
    
    /**
     * Get current authenticated user ID
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationException("User not authenticated");
        }
        
        return authentication.getName();
    }
    
    /**
     * Get current user details
     */
    public UserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationException("User not authenticated");
        }
        
        return (UserDetails) authentication.getPrincipal();
    }
    
    /**
     * Check if current user has role
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();
        
        if (authentication == null) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
    
    /**
     * Check if current user has permission
     */
    public boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();
        
        if (authentication == null) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(permission));
    }
}
```

## Error Handling

### Authorization Exceptions

```java
@RestControllerAdvice
public class AuthorizationExceptionHandler {
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex) {
        
        ErrorResponse error = ErrorResponse.builder()
            .errorCode("ACCESS_DENIED")
            .message("You do not have permission to access this resource")
            .timestamp(Instant.now())
            .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    
    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientAuthentication(
            InsufficientAuthenticationException ex) {
        
        ErrorResponse error = ErrorResponse.builder()
            .errorCode("AUTHENTICATION_REQUIRED")
            .message("Authentication is required to access this resource")
            .timestamp(Instant.now())
            .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}
```

## Testing

### Authorization Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @WithMockUser(username = "user-123", roles = "CUSTOMER")
    void customer_can_access_own_data() throws Exception {
        mockMvc.perform(get("/api/v1/customers/user-123"))
            .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(username = "user-123", roles = "CUSTOMER")
    void customer_cannot_access_other_customer_data() throws Exception {
        mockMvc.perform(get("/api/v1/customers/user-456"))
            .andExpect(status().isForbidden());
    }
    
    @Test
    @WithMockUser(username = "admin-1", roles = "ADMIN")
    void admin_can_access_any_customer_data() throws Exception {
        mockMvc.perform(get("/api/v1/customers/user-123"))
            .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(username = "user-123", roles = "CUSTOMER")
    void customer_can_create_order() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "items": [{"productId": "prod-1", "quantity": 2}]
                    }
                    """))
            .andExpect(status().isCreated());
    }
    
    @Test
    @WithAnonymousUser
    void anonymous_user_cannot_create_order() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "items": [{"productId": "prod-1", "quantity": 2}]
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }
}
```

## Monitoring and Logging

### Authorization Events to Log

```java
@Component
public class AuthorizationEventLogger {
    
    private final Logger logger = LoggerFactory.getLogger("SECURITY");
    
    @EventListener
    public void handleAuthorizationFailure(
            AuthorizationFailureEvent event) {
        
        logger.warn("Authorization failure",
            kv("event", "AUTHZ_FAILURE"),
            kv("userId", event.getUserId()),
            kv("resource", event.getResource()),
            kv("action", event.getAction()),
            kv("reason", event.getReason()),
            kv("timestamp", Instant.now()));
    }
    
    @EventListener
    public void handleAccessDenied(AccessDeniedEvent event) {
        logger.warn("Access denied",
            kv("event", "ACCESS_DENIED"),
            kv("userId", event.getUserId()),
            kv("requestUri", event.getRequestUri()),
            kv("method", event.getHttpMethod()),
            kv("timestamp", Instant.now()));
    }
}
```

## Best Practices

### ✅ Do

- Always check authorization at multiple layers (controller, service, data)
- Use method-level security annotations for clarity
- Implement custom permission evaluators for complex logic
- Log all authorization failures for security monitoring
- Test authorization rules thoroughly
- Use least privilege principle
- Filter data based on user permissions

### ❌ Don't

- Don't rely solely on client-side authorization
- Don't expose unauthorized data in error messages
- Don't implement authorization logic in controllers only
- Don't forget to check authorization on all endpoints
- Don't use hard-coded role checks everywhere
- Don't skip authorization tests

## Related Documentation

- [Authentication](authentication.md) - Authentication mechanisms
- [Security Overview](overview.md) - Overall security perspective
- [Security Standards](../../.kiro/steering/security-standards.md) - Detailed security standards

## References

- Spring Security Authorization: https://docs.spring.io/spring-security/reference/servlet/authorization/index.html
- OWASP Authorization Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html
- RBAC: https://en.wikipedia.org/wiki/Role-based_access_control
