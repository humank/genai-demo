# Creating a New Aggregate

> **Last Updated**: 2025-10-25

## Overview

This guide provides step-by-step instructions for creating a new aggregate root in the Enterprise E-Commerce Platform, following Domain-Driven Design (DDD) principles and our hexagonal architecture.

## Prerequisites

Before starting, ensure you understand:

- [DDD Tactical Patterns](../../architecture/patterns/ddd-patterns.md)
- [Coding Standards](../coding-standards/java-standards.md)
- [Testing Strategy](../testing/testing-strategy.md)

## Example: Creating a Review Aggregate

We'll create a `Review` aggregate that allows customers to review products they've purchased.

### Step 1: Define Requirements

**User Story:**

```text
As a customer
I want to review products I've purchased
So that I can share my experience with other customers
```

**Acceptance Criteria:**

- Customer can create a review for a purchased product
- Review includes rating (1-5 stars) and comment
- Customer can only review products they've purchased
- Customer can update their own reviews
- Customer can delete their own reviews

### Step 2: Design the Aggregate

#### Identify Aggregate Boundaries

**Aggregate Root:** `Review`

**Entities:** None (Review is a simple aggregate)

**Value Objects:**

- `ReviewId`: Unique identifier
- `Rating`: 1-5 stars with validation
- `ReviewComment`: Text content with length validation

**Domain Events:**

- `ReviewCreatedEvent`
- `ReviewUpdatedEvent`
- `ReviewDeletedEvent`

### Step 3: Create Value Objects

#### Create ReviewId

```java
// Location: domain/review/model/valueobject/ReviewId.java
package solid.humank.genaidemo.domain.review.model.valueobject;

import java.util.UUID;

public record ReviewId(String value) {
    
    public ReviewId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Review ID cannot be empty");
        }
    }
    
    public static ReviewId generate() {
        return new ReviewId(UUID.randomUUID().toString());
    }
    
    public static ReviewId of(String value) {
        return new ReviewId(value);
    }
}
```

#### Create Rating

```java
// Location: domain/review/model/valueobject/Rating.java
package solid.humank.genaidemo.domain.review.model.valueobject;

public record Rating(int value) {
    
    public Rating {
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException(
                "Rating must be between 1 and 5, got: " + value);
        }
    }
    
    public static Rating of(int value) {
        return new Rating(value);
    }
    
    public static Rating oneStar() {
        return new Rating(1);
    }
    
    public static Rating fiveStars() {
        return new Rating(5);
    }
}
```

#### Create ReviewComment

```java
// Location: domain/review/model/valueobject/ReviewComment.java
package solid.humank.genaidemo.domain.review.model.valueobject;

public record ReviewComment(String value) {
    
    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 1000;
    
    public ReviewComment {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Review comment cannot be empty");
        }
        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                "Review comment must be at least " + MIN_LENGTH + " characters");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "Review comment cannot exceed " + MAX_LENGTH + " characters");
        }
    }
    
    public static ReviewComment of(String value) {
        return new ReviewComment(value);
    }
}
```

### Step 4: Create Domain Events

#### ReviewCreatedEvent

```java
// Location: domain/review/events/ReviewCreatedEvent.java
package solid.humank.genaidemo.domain.review.events;

import solid.humank.genaidemo.domain.review.model.valueobject.ReviewId;
import solid.humank.genaidemo.domain.review.model.valueobject.Rating;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.shared.DomainEvent;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewCreatedEvent(
    ReviewId reviewId,
    CustomerId customerId,
    ProductId productId,
    Rating rating,
    String comment,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    public static ReviewCreatedEvent create(
        ReviewId reviewId,
        CustomerId customerId,
        ProductId productId,
        Rating rating,
        String comment
    ) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new ReviewCreatedEvent(
            reviewId, customerId, productId, rating, comment,
            metadata.eventId(), metadata.occurredOn()
        );
    }
    
    @Override
    public String getEventType() {
        return DomainEvent.getEventTypeFromClass(this.getClass());
    }
    
    @Override
    public String getAggregateId() {
        return reviewId.value();
    }
}
```

### Step 5: Create Aggregate Root

```java
// Location: domain/review/model/aggregate/Review.java
package solid.humank.genaidemo.domain.review.model.aggregate;

import solid.humank.genaidemo.domain.review.model.valueobject.ReviewId;
import solid.humank.genaidemo.domain.review.model.valueobject.Rating;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewComment;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.review.events.*;
import solid.humank.genaidemo.domain.shared.AggregateRoot;
import solid.humank.genaidemo.domain.shared.annotations.AggregateRootAnnotation;

import java.time.LocalDateTime;

@AggregateRootAnnotation(name = "Review", boundedContext = "Review", version = "1.0")
public class Review extends AggregateRoot {
    
    private final ReviewId id;
    private final CustomerId customerId;
    private final ProductId productId;
    private Rating rating;
    private ReviewComment comment;
    private ReviewStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructor for creating new review
    public Review(
        ReviewId id,
        CustomerId customerId,
        ProductId productId,
        Rating rating,
        ReviewComment comment
    ) {
        this.id = id;
        this.customerId = customerId;
        this.productId = productId;
        this.rating = rating;
        this.comment = comment;
        this.status = ReviewStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        // Collect domain event
        collectEvent(ReviewCreatedEvent.create(
            id, customerId, productId, rating, comment.value()
        ));
    }
    
    // Business methods
    public void update(Rating newRating, ReviewComment newComment) {
        validateCanUpdate();
        
        this.rating = newRating;
        this.comment = newComment;
        this.updatedAt = LocalDateTime.now();
        
        collectEvent(ReviewUpdatedEvent.create(
            id, newRating, newComment.value()
        ));
    }
    
    public void delete() {
        validateCanDelete();
        
        this.status = ReviewStatus.DELETED;
        this.updatedAt = LocalDateTime.now();
        
        collectEvent(ReviewDeletedEvent.create(id));
    }
    
    // Validation methods
    private void validateCanUpdate() {
        if (status == ReviewStatus.DELETED) {
            throw new IllegalStateException("Cannot update deleted review");
        }
    }
    
    private void validateCanDelete() {
        if (status == ReviewStatus.DELETED) {
            throw new IllegalStateException("Review is already deleted");
        }
    }
    
    // Getters
    public ReviewId getId() {
        return id;
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
    
    public ProductId getProductId() {
        return productId;
    }
    
    public Rating getRating() {
        return rating;
    }
    
    public ReviewComment getComment() {
        return comment;
    }
    
    public ReviewStatus getStatus() {
        return status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
```

#### Create ReviewStatus Enum

```java
// Location: domain/review/model/ReviewStatus.java
package solid.humank.genaidemo.domain.review.model;

public enum ReviewStatus {
    ACTIVE,
    DELETED
}
```

### Step 6: Create Repository Interface

```java
// Location: domain/review/repository/ReviewRepository.java
package solid.humank.genaidemo.domain.review.repository;

import solid.humank.genaidemo.domain.review.model.aggregate.Review;
import solid.humank.genaidemo.domain.review.model.valueobject.ReviewId;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository {
    
    Review save(Review review);
    
    Optional<Review> findById(ReviewId reviewId);
    
    List<Review> findByProductId(ProductId productId);
    
    List<Review> findByCustomerId(CustomerId customerId);
    
    Optional<Review> findByCustomerIdAndProductId(CustomerId customerId, ProductId productId);
    
    void delete(ReviewId reviewId);
}
```

### Step 7: Write Tests

#### Unit Tests for Value Objects

```java
// Location: test/java/domain/review/model/valueobject/RatingTest.java
package solid.humank.genaidemo.domain.review.model.valueobject;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RatingTest {
    
    @Test
    void should_create_rating_with_valid_value() {
        // When
        Rating rating = Rating.of(5);
        
        // Then
        assertThat(rating.value()).isEqualTo(5);
    }
    
    @Test
    void should_throw_exception_when_rating_below_minimum() {
        // When & Then
        assertThatThrownBy(() -> Rating.of(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Rating must be between 1 and 5");
    }
    
    @Test
    void should_throw_exception_when_rating_above_maximum() {
        // When & Then
        assertThatThrownBy(() -> Rating.of(6))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Rating must be between 1 and 5");
    }
}
```

#### Unit Tests for Aggregate

```java
// Location: test/java/domain/review/model/aggregate/ReviewTest.java
package solid.humank.genaidemo.domain.review.model.aggregate;

import org.junit.jupiter.api.Test;
import solid.humank.genaidemo.domain.review.model.valueobject.*;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.review.events.ReviewCreatedEvent;

import static org.assertj.core.api.Assertions.*;

class ReviewTest {
    
    @Test
    void should_create_review_with_valid_data() {
        // Given
        ReviewId reviewId = ReviewId.generate();
        CustomerId customerId = CustomerId.of("CUST-001");
        ProductId productId = ProductId.of("PROD-001");
        Rating rating = Rating.of(5);
        ReviewComment comment = ReviewComment.of("Excellent product! Highly recommended.");
        
        // When
        Review review = new Review(reviewId, customerId, productId, rating, comment);
        
        // Then
        assertThat(review.getId()).isEqualTo(reviewId);
        assertThat(review.getCustomerId()).isEqualTo(customerId);
        assertThat(review.getProductId()).isEqualTo(productId);
        assertThat(review.getRating()).isEqualTo(rating);
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.ACTIVE);
        assertThat(review.hasUncommittedEvents()).isTrue();
        assertThat(review.getUncommittedEvents()).hasSize(1);
        assertThat(review.getUncommittedEvents().get(0))
            .isInstanceOf(ReviewCreatedEvent.class);
    }
    
    @Test
    void should_update_review_successfully() {
        // Given
        Review review = createReview();
        Rating newRating = Rating.of(4);
        ReviewComment newComment = ReviewComment.of("Good product, but could be better.");
        
        // When
        review.update(newRating, newComment);
        
        // Then
        assertThat(review.getRating()).isEqualTo(newRating);
        assertThat(review.getComment()).isEqualTo(newComment);
    }
    
    @Test
    void should_throw_exception_when_updating_deleted_review() {
        // Given
        Review review = createReview();
        review.delete();
        
        // When & Then
        assertThatThrownBy(() -> review.update(
            Rating.of(4),
            ReviewComment.of("Updated comment that should fail.")
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot update deleted review");
    }
    
    private Review createReview() {
        return new Review(
            ReviewId.generate(),
            CustomerId.of("CUST-001"),
            ProductId.of("PROD-001"),
            Rating.of(5),
            ReviewComment.of("Excellent product! Highly recommended.")
        );
    }
}
```

### Step 8: Implement Infrastructure

#### JPA Entity

```java
// Location: infrastructure/review/persistence/entity/ReviewEntity.java
package solid.humank.genaidemo.infrastructure.review.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class ReviewEntity {
    
    @Id
    private String id;
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    
    @Column(name = "product_id", nullable = false)
    private String productId;
    
    @Column(nullable = false)
    private int rating;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String comment;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReviewStatus status;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors, getters, setters
}
```

#### Repository Implementation

```java
// Location: infrastructure/review/persistence/repository/JpaReviewRepository.java
package solid.humank.genaidemo.infrastructure.review.persistence.repository;

import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.domain.review.model.aggregate.Review;
import solid.humank.genaidemo.domain.review.repository.ReviewRepository;
import solid.humank.genaidemo.infrastructure.review.persistence.mapper.ReviewMapper;

@Repository
public class JpaReviewRepository implements ReviewRepository {
    
    private final ReviewJpaRepository jpaRepository;
    private final ReviewMapper mapper;
    
    public JpaReviewRepository(ReviewJpaRepository jpaRepository, ReviewMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Review save(Review review) {
        ReviewEntity entity = mapper.toEntity(review);
        ReviewEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<Review> findById(ReviewId reviewId) {
        return jpaRepository.findById(reviewId.value())
            .map(mapper::toDomain);
    }
    
    // Implement other methods...
}
```

### Step 9: Create Application Service

```java
// Location: application/review/ReviewApplicationService.java
package solid.humank.genaidemo.application.review;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solid.humank.genaidemo.domain.review.model.aggregate.Review;
import solid.humank.genaidemo.domain.review.repository.ReviewRepository;
import solid.humank.genaidemo.application.shared.DomainEventApplicationService;

@Service
@Transactional
public class ReviewApplicationService {
    
    private final ReviewRepository reviewRepository;
    private final DomainEventApplicationService eventService;
    
    public ReviewApplicationService(
        ReviewRepository reviewRepository,
        DomainEventApplicationService eventService
    ) {
        this.reviewRepository = reviewRepository;
        this.eventService = eventService;
    }
    
    public Review createReview(CreateReviewCommand command) {
        // Create review
        Review review = new Review(
            ReviewId.generate(),
            command.customerId(),
            command.productId(),
            command.rating(),
            command.comment()
        );
        
        // Save review
        Review savedReview = reviewRepository.save(review);
        
        // Publish events
        eventService.publishEventsFromAggregate(savedReview);
        
        return savedReview;
    }
}
```

### Step 10: Create REST Controller

```java
// Location: interfaces/rest/review/controller/ReviewController.java
package solid.humank.genaidemo.interfaces.rest.review.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import solid.humank.genaidemo.application.review.ReviewApplicationService;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    
    private final ReviewApplicationService reviewService;
    
    public ReviewController(ReviewApplicationService reviewService) {
        this.reviewService = reviewService;
    }
    
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
        @Valid @RequestBody CreateReviewRequest request
    ) {
        CreateReviewCommand command = new CreateReviewCommand(
            CustomerId.of(request.customerId()),
            ProductId.of(request.productId()),
            Rating.of(request.rating()),
            ReviewComment.of(request.comment())
        );
        
        Review review = reviewService.createReview(command);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ReviewResponse.from(review));
    }
}
```

## Summary

You've successfully created a new aggregate! The key steps were:

1. ✅ Define requirements and acceptance criteria
2. ✅ Design aggregate boundaries
3. ✅ Create value objects with validation
4. ✅ Create domain events
5. ✅ Implement aggregate root with business logic
6. ✅ Define repository interface
7. ✅ Write comprehensive tests
8. ✅ Implement infrastructure (JPA entities, repositories)
9. ✅ Create application service
10. ✅ Create REST controller

## Next Steps

- Add more business methods to the aggregate
- Implement event handlers
- Add integration tests
- Update API documentation
- Add to Swagger/OpenAPI spec

## Related Documentation

- [DDD Tactical Patterns](../../architecture/patterns/ddd-patterns.md)
- [Adding REST Endpoint](adding-endpoint.md)
- [Implementing Domain Event](implementing-event.md)

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-25  
**Maintained By**: Development Team
