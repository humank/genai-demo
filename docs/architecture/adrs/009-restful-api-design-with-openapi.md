---
adr_number: 009
title: "RESTful API Design with OpenAPI 3.0"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [002, 003]
affected_viewpoints: ["functional", "development"]
affected_perspectives: ["evolution", "accessibility"]
---

# ADR-009: RESTful API Design with OpenAPI 3.0

## Status

**Accepted** - 2025-10-24

## Context

### Problem Statement

The Enterprise E-Commerce Platform needs a well-defined API strategy that:

- Provides consistent, intuitive interfaces for clients
- Supports multiple client types (web, mobile, third-party integrations)
- Enables API versioning and evolution
- Provides comprehensive API documentation
- Supports API testing and validation
- Follows industry best practices
- Enables API-first development
- Supports contract testing

### Business Context

**Business Drivers**:
- Need for web and mobile client applications
- Future requirement for third-party integrations
- API marketplace potential
- Developer experience for internal and external developers
- Rapid feature development without breaking clients
- Compliance with industry standards

**Constraints**:
- Team has REST API experience
- Spring Boot framework (ADR-002)
- Need for backward compatibility
- Must support API versioning
- Budget: No additional API gateway costs initially

### Technical Context

**Current State**:
- Spring Boot 3.4.5 + Java 21
- Hexagonal Architecture (ADR-002)
- Multiple bounded contexts
- Event-driven architecture (ADR-003)
- Next.js and Angular frontends

**Requirements**:
- RESTful API design
- API documentation generation
- Request/response validation
- Error handling standards
- API versioning strategy
- Authentication and authorization
- Rate limiting support
- CORS configuration

## Decision Drivers

1. **Industry Standards**: Follow widely-adopted REST principles
2. **Documentation**: Auto-generate comprehensive API docs
3. **Developer Experience**: Easy to understand and use
4. **Versioning**: Support API evolution without breaking clients
5. **Validation**: Automatic request/response validation
6. **Testing**: Enable contract testing
7. **Tooling**: Good IDE and testing tool support
8. **Team Skills**: Leverage existing REST API knowledge

## Considered Options

### Option 1: RESTful API with OpenAPI 3.0 (SpringDoc)

**Description**: REST API following OpenAPI 3.0 specification with SpringDoc for documentation

**Pros**:
- ✅ Industry-standard REST principles
- ✅ OpenAPI 3.0 widely supported
- ✅ SpringDoc auto-generates documentation from code
- ✅ Swagger UI for interactive testing
- ✅ Strong tooling ecosystem
- ✅ Team has REST experience
- ✅ Supports API versioning
- ✅ Contract-first or code-first approach
- ✅ Free and open source

**Cons**:
- ⚠️ REST can be verbose for complex operations
- ⚠️ Need to maintain API versioning discipline
- ⚠️ Over-fetching/under-fetching possible

**Cost**: $0 (open source)

**Risk**: **Low** - Proven, widely adopted

### Option 2: GraphQL

**Description**: GraphQL API with schema-first design

**Pros**:
- ✅ Flexible querying (no over/under-fetching)
- ✅ Strong typing
- ✅ Single endpoint
- ✅ Real-time subscriptions
- ✅ Introspection

**Cons**:
- ❌ Team lacks GraphQL experience
- ❌ More complex to implement
- ❌ Caching more difficult
- ❌ N+1 query problems
- ❌ Harder to version
- ❌ Security concerns (query complexity)

**Cost**: $0 (open source)

**Risk**: **Medium** - Learning curve, complexity

### Option 3: gRPC

**Description**: gRPC with Protocol Buffers

**Pros**:
- ✅ High performance (binary protocol)
- ✅ Strong typing
- ✅ Bi-directional streaming
- ✅ Code generation

**Cons**:
- ❌ Not browser-friendly (needs gRPC-Web)
- ❌ Team lacks gRPC experience
- ❌ Limited tooling for debugging
- ❌ Harder to test manually
- ❌ Not suitable for public APIs

**Cost**: $0 (open source)

**Risk**: **High** - Not suitable for web/mobile clients

### Option 4: REST without OpenAPI

**Description**: REST API without formal specification

**Pros**:
- ✅ Simple to start
- ✅ Flexible

**Cons**:
- ❌ No auto-generated documentation
- ❌ Manual documentation maintenance
- ❌ No contract testing
- ❌ Inconsistent API design
- ❌ Poor developer experience

**Cost**: $0

**Risk**: **High** - Poor maintainability

## Decision Outcome

**Chosen Option**: **RESTful API with OpenAPI 3.0 (SpringDoc)**

### Rationale

RESTful API with OpenAPI 3.0 was selected for the following reasons:

1. **Industry Standard**: REST is widely understood and adopted
2. **Team Experience**: Team already knows REST principles
3. **OpenAPI Ecosystem**: Excellent tooling for documentation, testing, and code generation
4. **SpringDoc Integration**: Seamless Spring Boot integration with auto-generated docs
5. **Swagger UI**: Interactive API documentation and testing
6. **Versioning Support**: URL-based versioning strategy
7. **Client Support**: Works with all client types (web, mobile, third-party)
8. **Cost-Effective**: Free and open source

**Implementation Strategy**:

**API Design Principles**:
- RESTful resource-based URLs
- Standard HTTP methods (GET, POST, PUT, DELETE, PATCH)
- Consistent response formats
- Proper HTTP status codes
- HATEOAS for discoverability (optional)

**OpenAPI Documentation**:
- SpringDoc annotations on controllers
- Auto-generated OpenAPI 3.0 specification
- Swagger UI for interactive testing
- API documentation versioned with code

**Versioning Strategy**:
- URL-based versioning: `/api/v1/`, `/api/v2/`
- Maintain backward compatibility for at least 2 versions
- Deprecation headers for old versions

**Why Not GraphQL**: While GraphQL offers flexibility, the team lacks experience and REST meets all current requirements. Can add GraphQL later if needed.

**Why Not gRPC**: Not suitable for browser-based clients. Better for internal service-to-service communication, which we handle with domain events.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Frontend Developers | High | Primary API consumers | Clear documentation, examples, Swagger UI |
| Mobile Developers | High | API consumers | SDK generation from OpenAPI spec |
| Third-Party Developers | Medium | Future API consumers | Public API documentation, sandbox environment |
| Backend Developers | Medium | API implementers | SpringDoc annotations, examples |
| QA Team | Medium | API testing | Postman collections, contract tests |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:
- All client applications
- API documentation
- Testing strategy
- Deployment process
- Monitoring and logging
- Security implementation

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Breaking API changes | Medium | High | Versioning strategy, deprecation policy |
| Inconsistent API design | Medium | Medium | API design guidelines, code reviews |
| Documentation drift | Medium | Medium | Auto-generation from code, CI/CD checks |
| Over-fetching data | Low | Low | Optimize endpoints, consider GraphQL later |
| API security issues | Low | High | Security best practices, regular audits |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Setup and Standards (Week 1)

- [x] Add SpringDoc dependency
  ```xml
  <dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
  </dependency>
  ```

- [x] Configure SpringDoc
  ```java
  @Configuration
  public class OpenApiConfiguration {
      @Bean
      public OpenAPI customOpenAPI() {
          return new OpenAPI()
              .info(new Info()
                  .title("E-Commerce Platform API")
                  .version("v1")
                  .description("Enterprise E-Commerce Platform REST API")
                  .contact(new Contact()
                      .name("API Support")
                      .email("api@ecommerce.com"))
                  .license(new License()
                      .name("Apache 2.0")
                      .url("https://www.apache.org/licenses/LICENSE-2.0")))
              .servers(List.of(
                  new Server().url("https://api.ecommerce.com").description("Production"),
                  new Server().url("https://api-staging.ecommerce.com").description("Staging"),
                  new Server().url("http://localhost:8080").description("Local")
              ));
      }
  }
  ```

- [x] Create API design guidelines document
- [x] Define standard response formats

### Phase 2: API Design Patterns (Week 1-2)

- [ ] Implement standard response wrapper
  ```java
  public record ApiResponse<T>(
      T data,
      ApiMetadata metadata,
      List<ApiError> errors
  ) {
      public static <T> ApiResponse<T> success(T data) {
          return new ApiResponse<>(data, ApiMetadata.create(), null);
      }
      
      public static <T> ApiResponse<T> error(List<ApiError> errors) {
          return new ApiResponse<>(null, ApiMetadata.create(), errors);
      }
  }
  
  public record ApiMetadata(
      String requestId,
      LocalDateTime timestamp,
      String version
  ) {
      public static ApiMetadata create() {
          return new ApiMetadata(
              UUID.randomUUID().toString(),
              LocalDateTime.now(),
              "v1"
          );
      }
  }
  ```

- [ ] Implement error response format
  ```java
  public record ApiError(
      String code,
      String message,
      String field,
      Object rejectedValue
  ) {}
  
  @RestControllerAdvice
  public class GlobalExceptionHandler {
      @ExceptionHandler(MethodArgumentNotValidException.class)
      public ResponseEntity<ApiResponse<Void>> handleValidation(
          MethodArgumentNotValidException ex) {
          
          List<ApiError> errors = ex.getBindingResult()
              .getFieldErrors()
              .stream()
              .map(error -> new ApiError(
                  "VALIDATION_ERROR",
                  error.getDefaultMessage(),
                  error.getField(),
                  error.getRejectedValue()
              ))
              .toList();
          
          return ResponseEntity
              .badRequest()
              .body(ApiResponse.error(errors));
      }
  }
  ```

- [ ] Create pagination support
  ```java
  public record PageResponse<T>(
      List<T> content,
      PageMetadata page
  ) {}
  
  public record PageMetadata(
      int number,
      int size,
      long totalElements,
      int totalPages,
      boolean first,
      boolean last
  ) {
      public static PageMetadata from(Page<?> page) {
          return new PageMetadata(
              page.getNumber(),
              page.getSize(),
              page.getTotalElements(),
              page.getTotalPages(),
              page.isFirst(),
              page.isLast()
          );
      }
  }
  ```

### Phase 3: Customer API Implementation (Week 2-3)

- [ ] Implement Customer API endpoints
  ```java
  @RestController
  @RequestMapping("/api/v1/customers")
  @Tag(name = "Customer", description = "Customer management APIs")
  public class CustomerController {
      
      @Operation(
          summary = "Get customer by ID",
          description = "Returns a single customer by their unique identifier"
      )
      @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Customer found"),
          @ApiResponse(responseCode = "404", description = "Customer not found"),
          @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
      @GetMapping("/{id}")
      public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(
          @Parameter(description = "Customer ID", required = true)
          @PathVariable String id) {
          
          Customer customer = customerService.findById(id);
          CustomerResponse response = CustomerResponse.from(customer);
          return ResponseEntity.ok(ApiResponse.success(response));
      }
      
      @Operation(summary = "Create new customer")
      @PostMapping
      public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
          @Valid @RequestBody CreateCustomerRequest request) {
          
          Customer customer = customerService.create(request);
          CustomerResponse response = CustomerResponse.from(customer);
          return ResponseEntity
              .status(HttpStatus.CREATED)
              .body(ApiResponse.success(response));
      }
      
      @Operation(summary = "Update customer")
      @PutMapping("/{id}")
      public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
          @PathVariable String id,
          @Valid @RequestBody UpdateCustomerRequest request) {
          
          Customer customer = customerService.update(id, request);
          CustomerResponse response = CustomerResponse.from(customer);
          return ResponseEntity.ok(ApiResponse.success(response));
      }
      
      @Operation(summary = "Delete customer")
      @DeleteMapping("/{id}")
      public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
          customerService.delete(id);
          return ResponseEntity.noContent().build();
      }
      
      @Operation(summary = "List customers with pagination")
      @GetMapping
      public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> listCustomers(
          @Parameter(description = "Page number (0-based)")
          @RequestParam(defaultValue = "0") int page,
          @Parameter(description = "Page size")
          @RequestParam(defaultValue = "20") int size,
          @Parameter(description = "Sort field")
          @RequestParam(defaultValue = "createdAt") String sort) {
          
          Page<Customer> customers = customerService.findAll(
              PageRequest.of(page, size, Sort.by(sort).descending())
          );
          
          PageResponse<CustomerResponse> response = new PageResponse<>(
              customers.getContent().stream()
                  .map(CustomerResponse::from)
                  .toList(),
              PageMetadata.from(customers)
          );
          
          return ResponseEntity.ok(ApiResponse.success(response));
      }
  }
  ```

- [ ] Add request/response DTOs with validation
  ```java
  public record CreateCustomerRequest(
      @NotBlank(message = "Name is required")
      @Size(min = 2, max = 100)
      String name,
      
      @NotBlank(message = "Email is required")
      @Email(message = "Invalid email format")
      String email,
      
      @NotBlank(message = "Password is required")
      @Size(min = 8, max = 128)
      @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
               message = "Password must contain uppercase, lowercase, and number")
      String password,
      
      @Valid
      @NotNull(message = "Address is required")
      AddressDto address
  ) {}
  
  @Schema(description = "Customer response")
  public record CustomerResponse(
      @Schema(description = "Customer unique identifier")
      String id,
      
      @Schema(description = "Customer name")
      String name,
      
      @Schema(description = "Customer email")
      String email,
      
      @Schema(description = "Customer address")
      AddressDto address,
      
      @Schema(description = "Membership level")
      String membershipLevel,
      
      @Schema(description = "Creation timestamp")
      LocalDateTime createdAt,
      
      @Schema(description = "Last update timestamp")
      LocalDateTime updatedAt
  ) {
      public static CustomerResponse from(Customer customer) {
          return new CustomerResponse(
              customer.getId().getValue(),
              customer.getName().getValue(),
              customer.getEmail().getValue(),
              AddressDto.from(customer.getAddress()),
              customer.getMembershipLevel().name(),
              customer.getCreatedAt(),
              customer.getUpdatedAt()
          );
      }
  }
  ```

### Phase 4: Remaining APIs (Week 3-6)

- [ ] Implement Order API
- [ ] Implement Product API
- [ ] Implement Shopping Cart API
- [ ] Implement Payment API
- [ ] Implement remaining bounded context APIs

### Phase 5: API Versioning (Week 6-7)

- [ ] Implement versioning strategy
  ```java
  @RestController
  @RequestMapping("/api/v1/customers")
  public class CustomerV1Controller {
      // V1 implementation
  }
  
  @RestController
  @RequestMapping("/api/v2/customers")
  public class CustomerV2Controller {
      // V2 implementation with breaking changes
  }
  ```

- [ ] Add deprecation headers
  ```java
  @GetMapping("/{id}")
  public ResponseEntity<CustomerResponse> getCustomer(@PathVariable String id) {
      return ResponseEntity.ok()
          .header("Deprecation", "true")
          .header("Sunset", "2026-12-31T23:59:59Z")
          .header("Link", "</api/v2/customers>; rel=\"successor-version\"")
          .body(response);
  }
  ```

### Phase 6: Testing and Documentation (Week 7-8)

- [ ] Generate OpenAPI specification
  ```bash
  # Access at http://localhost:8080/v3/api-docs
  # Swagger UI at http://localhost:8080/swagger-ui.html
  ```

- [ ] Create Postman collection from OpenAPI spec
- [ ] Implement contract tests
  ```java
  @SpringBootTest(webEnvironment = RANDOM_PORT)
  class CustomerApiContractTest {
      @Test
      void should_match_openapi_specification() {
          // Validate API responses against OpenAPI spec
      }
  }
  ```

- [ ] Create API usage examples
- [ ] Document authentication flow

### Rollback Strategy

**Trigger Conditions**:
- API design inconsistencies causing client issues
- OpenAPI documentation drift > 20%
- Team unable to maintain API standards
- Performance issues with SpringDoc

**Rollback Steps**:
1. Remove SpringDoc dependency
2. Create manual API documentation
3. Simplify API design
4. Re-evaluate API strategy

**Rollback Time**: 1 week

## Monitoring and Success Criteria

### Success Metrics

- ✅ 100% of endpoints documented in OpenAPI
- ✅ API response time < 2 seconds (95th percentile)
- ✅ API error rate < 1%
- ✅ Zero breaking changes without version bump
- ✅ API documentation accuracy > 95%
- ✅ Developer satisfaction > 4/5

### Monitoring Plan

**API Metrics**:
- Request rate per endpoint
- Response time per endpoint
- Error rate per endpoint
- API version usage distribution

**Documentation Metrics**:
- OpenAPI spec generation success
- Documentation page views
- API usage examples accessed

**Review Schedule**:
- Weekly: API design review
- Monthly: API versioning review
- Quarterly: API strategy review

## Consequences

### Positive Consequences

- ✅ **Consistent API Design**: RESTful principles ensure consistency
- ✅ **Auto-Generated Docs**: SpringDoc generates docs from code
- ✅ **Interactive Testing**: Swagger UI enables easy API testing
- ✅ **Contract Testing**: OpenAPI spec enables contract tests
- ✅ **Client Generation**: Can generate client SDKs from spec
- ✅ **Versioning Support**: URL-based versioning is straightforward
- ✅ **Industry Standard**: REST and OpenAPI are widely adopted
- ✅ **Team Familiarity**: Team already knows REST

### Negative Consequences

- ⚠️ **Verbosity**: REST can be verbose for complex operations
- ⚠️ **Over/Under-Fetching**: May need multiple requests or get extra data
- ⚠️ **Versioning Discipline**: Need to maintain backward compatibility
- ⚠️ **Documentation Maintenance**: Need to keep annotations up to date

### Technical Debt

**Identified Debt**:
1. No HATEOAS implementation yet (acceptable for MVP)
2. Limited API rate limiting (future enhancement)
3. No API gateway yet (future requirement)
4. Manual Postman collection creation (can be automated)

**Debt Repayment Plan**:
- **Q1 2026**: Implement API rate limiting
- **Q2 2026**: Add HATEOAS for discoverability
- **Q3 2026**: Evaluate API gateway (Kong, AWS API Gateway)
- **Q4 2026**: Automate client SDK generation

## Related Decisions

- [ADR-002: Adopt Hexagonal Architecture](002-adopt-hexagonal-architecture.md) - API in interfaces layer
- [ADR-003: Use Domain Events for Cross-Context Communication](003-use-domain-events-for-cross-context-communication.md) - Internal vs external communication

## Notes

### API Design Guidelines

**URL Naming**:
- Use nouns, not verbs: `/customers` not `/getCustomers`
- Use plural nouns: `/customers` not `/customer`
- Use kebab-case: `/order-items` not `/orderItems`
- Nest resources: `/customers/{id}/orders`

**HTTP Methods**:
- GET: Retrieve resource(s)
- POST: Create new resource
- PUT: Update entire resource
- PATCH: Partial update
- DELETE: Remove resource

**HTTP Status Codes**:
- 200 OK: Successful GET, PUT, PATCH
- 201 Created: Successful POST
- 204 No Content: Successful DELETE
- 400 Bad Request: Validation error
- 401 Unauthorized: Authentication required
- 403 Forbidden: Authorization failed
- 404 Not Found: Resource not found
- 409 Conflict: Business rule violation
- 500 Internal Server Error: System error

### OpenAPI Annotations

```java
@Tag(name = "Customer", description = "Customer management APIs")
@Operation(summary = "Get customer", description = "Returns customer by ID")
@ApiResponse(responseCode = "200", description = "Success")
@Parameter(description = "Customer ID", required = true)
@Schema(description = "Customer data")
@Valid
```

### Sample OpenAPI Specification

```yaml
openapi: 3.0.1
info:
  title: E-Commerce Platform API
  version: v1
  description: Enterprise E-Commerce Platform REST API
paths:
  /api/v1/customers:
    get:
      tags:
        - Customer
      summary: List customers
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 0
        - name: size
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: Success
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PageResponseCustomerResponse'
```

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-24  
**Next Review**: 2026-01-24 (Quarterly)
