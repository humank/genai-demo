
# Design

## 概述

本設計文件提供直接的程式碼Refactoring方案，解決專案中的 DDD Entity 設計問題。我們將採用漸進式RefactoringPolicy，確保每次變更都是安全且可驗證的。

## RefactoringPolicy

### 1. Seller AggregateRefactoringPolicy

#### 目標架構

```java
// Refactoring後的 Seller Aggregate Root
public class Seller extends AggregateRoot {
    private SellerId sellerId;
    private SellerProfile profile;           // Entity (原 SellerProfile Aggregate Root)
    private ContactInfo contactInfo;         // 新 Entity
    private List<SellerRating> ratings;      // 新 Entity 集合
    private SellerVerification verification; // 新 Entity
    private SellerStatus status;            // Value Object
}
```

#### Design

- **SellerProfile Entity**: 管理商業資訊和描述
- **ContactInfo Entity**: 管理聯繫方式和通訊偏好
- **SellerRating Entity**: 管理評級歷史和統計
- **SellerVerification Entity**: 管理驗證狀態和文件

### 2. ProductReview AggregateRefactoringPolicy

#### 目標架構

```java
// Refactoring後的 ProductReview Aggregate Root
public class ProductReview extends AggregateRoot {
    private final ReviewId id;
    private final ProductId productId;
    private final CustomerId reviewerId;
    private ReviewRating rating;
    private ReviewStatus status;
    
    // Entity 集合
    private List<ReviewImage> images;           // 原 List<String>
    private List<ModerationRecord> moderations; // 原 ReviewModeration Aggregate Root
    private List<ReviewResponse> responses;     // 新增商家回覆功能
}
```

#### Design

- **ReviewImage Entity**: 管理評價圖片的完整生命週期
- **ModerationRecord Entity**: 管理審核歷史和決策
- **ReviewResponse Entity**: 管理商家回覆和互動

### 3. Customer Aggregate豐富Policy

#### 目標架構

```java
// 豐富後的 Customer Aggregate Root
public class Customer implements AggregateRootInterface {
    // 現有屬性保持不變
    
    // Entity 集合改善
    private List<DeliveryAddress> deliveryAddresses; // 原 List<Address>
    private List<PaymentMethod> paymentMethods;      // 從獨立Aggregate Root遷移
    private CustomerPreferences preferences;         // 新 Entity
}
```

#### Design

- **DeliveryAddress Entity**: 豐富地址管理功能
- **PaymentMethod Entity**: 從Aggregate Root降級為 Entity
- **CustomerPreferences Entity**: 統一管理Customer偏好設定

### Design

#### Entity ID Value Objects

遵循專案現有的 ID Value Object 模式：

```java
@ValueObject(name = "SellerProfileId", description = "賣家檔案ID")
public record SellerProfileId(UUID value) {
    
    /**
     * 緊湊建構子 - 驗證參數
     */
    public SellerProfileId {
        Objects.requireNonNull(value, "SellerProfile ID cannot be null");
    }
    
    /**
     * 生成新的賣家檔案ID
     */
    public static SellerProfileId generate() {
        return new SellerProfileId(UUID.randomUUID());
    }
    
    /**
     * 從UUID創建賣家檔案ID
     */
    public static SellerProfileId of(UUID uuid) {
        return new SellerProfileId(uuid);
    }
    
    /**
     * 獲取ID值（向後相容方法）
     */
    public UUID getId() {
        return value;
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}

@ValueObject(name = "ContactInfoId", description = "聯繫資訊ID")
public record ContactInfoId(UUID value) {
    public static ContactInfoId generate() {
        return new ContactInfoId(UUID.randomUUID());
    }
}

@ValueObject(name = "SellerRatingId", description = "賣家評級ID")
public record SellerRatingId(UUID value) {
    public static SellerRatingId generate() {
        return new SellerRatingId(UUID.randomUUID());
    }
}

@ValueObject(name = "ReviewImageId", description = "評價圖片ID")
public record ReviewImageId(UUID value) {
    public static ReviewImageId generate() {
        return new ReviewImageId(UUID.randomUUID());
    }
}

@ValueObject(name = "ModerationRecordId", description = "審核記錄ID")
public record ModerationRecordId(UUID value) {
    public static ModerationRecordId generate() {
        return new ModerationRecordId(UUID.randomUUID());
    }
}

@ValueObject(name = "StockReservationId", description = "庫存預留ID")
public record StockReservationId(UUID value) {
    public static StockReservationId generate() {
        return new StockReservationId(UUID.randomUUID());
    }
}

@ValueObject(name = "DeliveryAddressId", description = "配送地址ID")
public record DeliveryAddressId(UUID value) {
    public static DeliveryAddressId generate() {
        return new DeliveryAddressId(UUID.randomUUID());
    }
}
```

#### Status Value Objects

```java
@ValueObject(name = "RatingStatus", description = "評級狀態")
public enum RatingStatus {
    ACTIVE("活躍"),
    HIDDEN("隱藏"),
    DELETED("已刪除");
    
    private final String description;
    
    RatingStatus(String description) {
        this.description = description;
    }
}

@ValueObject(name = "ImageStatus", description = "圖片狀態")
public enum ImageStatus {
    PENDING("待處理"),
    PROCESSED("已處理"),
    FAILED("處理失敗"),
    DELETED("已刪除");
    
    private final String description;
    
    ImageStatus(String description) {
        this.description = description;
    }
}

@ValueObject(name = "ModerationAction", description = "審核動作")
public enum ModerationAction {
    APPROVE("通過"),
    REJECT("拒絕"),
    HIDE("隱藏");
    
    private final String description;
    
    ModerationAction(String description) {
        this.description = description;
    }
}

@ValueObject(name = "ModerationStatus", description = "審核狀態")
public enum ModerationStatus {
    PENDING("待審核"),
    COMPLETED("已完成"),
    CANCELLED("已取消");
    
    private final String description;
    
    ModerationStatus(String description) {
        this.description = description;
    }
}

@ValueObject(name = "AddressStatus", description = "地址狀態")
public enum AddressStatus {
    ACTIVE("活躍"),
    INACTIVE("非活躍"),
    INVALID("無效");
    
    private final String description;
    
    AddressStatus(String description) {
        this.description = description;
    }
}
```

### 4. Inventory Aggregate改善Policy

#### 目標架構

```java
// 改善後的 Inventory Aggregate Root
public class Inventory extends AggregateRoot {
    // 現有屬性保持不變
    
    // Entity 集合改善
    private List<StockReservation> reservations;     // 原 Map<ReservationId, Integer>
    private List<StockMovement> movementHistory;     // 新增庫存異動記錄
    private InventoryThreshold threshold;            // 新 Entity
}
```

### 5. 需要的 Repository 和 Domain Service

#### 新增 Repository（如果需要）

```java
// 如果 SellerProfile 作為獨立查詢需求，可能需要專門的查詢方法
@Repository(name = "SellerRepository", description = "賣家Aggregate Root儲存庫")
public interface SellerRepository extends BaseRepository<Seller, SellerId> {
    
    /**
     * 根據商業執照號碼查詢賣家
     */
    Optional<Seller> findByBusinessLicense(String businessLicense);
    
    /**
     * 根據評級範圍查詢賣家
     */
    List<Seller> findByRatingRange(double minRating, double maxRating);
}
```

#### 新增 Domain Service

```java
@DomainService(name = "SellerProfileService", description = "賣家檔案服務，處理賣家檔案相關的複雜業務邏輯", boundedContext = "Seller")
public class SellerProfileService {
    
    private final SellerRepository sellerRepository;
    
    /**
     * 驗證賣家檔案完整性
     */
    public boolean validateSellerProfile(Seller seller) {
        // 跨 Entity 的驗證邏輯
        return seller.getProfile().isProfileComplete() && 
               seller.getVerification().isVerified();
    }
    
    /**
     * 計算賣家綜合評級
     */
    public double calculateOverallRating(Seller seller) {
        // 複雜的評級計算邏輯
        return seller.getRatings().stream()
                .mapToInt(SellerRating::getRating)
                .average()
                .orElse(0.0);
    }
}
```

## Design

### 專案現有 DDD 戰術模式分析

基於專案現有的 DDD 實作模式，我們需要遵循以下設計慣例：

#### 1. **註解使用模式**

- `@AggregateRoot(name, description, boundedContext, version)` - Aggregate Root
- `@Entity(name, description)` - Entity
- `@ValueObject(name, description)` - Value Object
- `@Repository(name, description)` - 儲存庫
- `@DomainService(name, description, boundedContext)` - Domain Service
- `@Factory(name, description)` - Factory

#### Design

- 使用 `record` 實作，提供不可變性
- 在緊湊建構子中進行驗證
- 提供靜態Factory方法（如 `generate()`, `of()`, `from()`）
- 提供向後相容的 getter 方法

#### 3. **Repository 模式**

- 繼承 `BaseRepository<T, ID>` 介面
- 使用 `@Repository` 註解標記
- 提供Aggregate Root特定的查詢方法

#### 4. **Domain Service 模式**

- 使用 `@DomainService` 註解
- 處理跨Aggregate Root的複雜業務邏輯
- 無狀態設計，依賴注入 Repository

### Design

遵循專案現有模式：

1. **具體 Entity 類別** - 每個 Entity 都是獨立的具體類別
2. **業務導向設計** - 專注於領域邏輯而非技術抽象
3. **ID Value Object** - 每個 Entity 使用強型別的 ID Value Object
4. **狀態管理** - 使用 enum Value Object 管理狀態

### Examples

#### SellerProfile Entity

```java
@Entity(name = "SellerProfile", description = "賣家檔案Entity")
public class SellerProfile {
    private final SellerProfileId id;
    private String businessName;
    private String businessAddress;
    private String businessLicense;
    private String description;
    private LocalDateTime lastProfileUpdate;
    
    public SellerProfile(SellerProfileId id, String businessName) {
        this.id = Objects.requireNonNull(id);
        this.businessName = Objects.requireNonNull(businessName);
        this.lastProfileUpdate = LocalDateTime.now();
    }
    
    // 豐富的業務邏輯
    public void updateBusinessInfo(String name, String address, String description) {
        this.businessName = name;
        this.businessAddress = address;
        this.description = description;
        this.lastProfileUpdate = LocalDateTime.now();
    }
    
    public boolean isProfileComplete() {
        return businessName != null && businessAddress != null && businessLicense != null;
    }
    
    public void validateBusinessLicense() {
        // 驗證商業執照邏輯
    }
}
```

#### ContactInfo Entity

```java
@Entity(name = "ContactInfo", description = "聯繫資訊Entity")
public class ContactInfo {
    private final ContactInfoId id;
    private String email;
    private String phone;
    private String preferredContactMethod;
    private boolean emailVerified;
    private boolean phoneVerified;
    private LocalDateTime lastUpdated;
    
    public ContactInfo(ContactInfoId id, String email, String phone) {
        this.id = Objects.requireNonNull(id);
        this.email = email;
        this.phone = phone;
        this.preferredContactMethod = "EMAIL";
        this.emailVerified = false;
        this.phoneVerified = false;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public void updateContactInfo(String email, String phone) {
        this.email = email;
        this.phone = phone;
        this.lastUpdated = LocalDateTime.now();
        // 重新驗證
        this.emailVerified = false;
        this.phoneVerified = false;
    }
    
    public void verifyEmail() {
        this.emailVerified = true;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public void verifyPhone() {
        this.phoneVerified = true;
        this.lastUpdated = LocalDateTime.now();
    }
}
```

#### SellerRating Entity

```java
@Entity(name = "SellerRating", description = "賣家評級Entity")
public class SellerRating {
    private final SellerRatingId id;
    private final CustomerId customerId;
    private int rating;
    private String comment;
    private LocalDateTime ratedAt;
    private RatingStatus status;
    
    public SellerRating(SellerRatingId id, CustomerId customerId, int rating, String comment) {
        this.id = Objects.requireNonNull(id);
        this.customerId = Objects.requireNonNull(customerId);
        this.rating = validateRating(rating);
        this.comment = comment;
        this.ratedAt = LocalDateTime.now();
        this.status = RatingStatus.ACTIVE;
    }
    
    private int validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("評級必須在1-5之間");
        }
        return rating;
    }
    
    public void updateRating(int newRating, String newComment) {
        this.rating = validateRating(newRating);
        this.comment = newComment;
    }
    
    public void hide() {
        this.status = RatingStatus.HIDDEN;
    }
}
```

#### ReviewImage Entity

```java
@Entity(name = "ReviewImage", description = "評價圖片Entity")
public class ReviewImage {
    private final ReviewImageId id;
    private String originalUrl;
    private String thumbnailUrl;
    private String fileName;
    private long fileSize;
    private ImageStatus status;
    private LocalDateTime uploadedAt;
    
    public ReviewImage(ReviewImageId id, String originalUrl, String fileName, long fileSize) {
        this.id = Objects.requireNonNull(id);
        this.originalUrl = Objects.requireNonNull(originalUrl);
        this.fileName = Objects.requireNonNull(fileName);
        this.fileSize = fileSize;
        this.status = ImageStatus.PENDING;
        this.uploadedAt = LocalDateTime.now();
    }
    
    // 業務邏輯
    public void markAsProcessed() {
        this.status = ImageStatus.PROCESSED;
    }
    
    public boolean isValidImage() {
        return fileSize > 0 && fileName.matches(".*\\.(jpg|jpeg|png|gif)$");
    }
    
    public void generateThumbnail() {
        // 生成縮圖邏輯
        this.thumbnailUrl = originalUrl + "_thumb";
    }
}
```

#### ModerationRecord Entity

```java
@Entity(name = "ModerationRecord", description = "審核記錄Entity")
public class ModerationRecord {
    private final ModerationRecordId id;
    private final String moderatorId;
    private ModerationAction action;
    private String reason;
    private String comments;
    private LocalDateTime moderatedAt;
    private ModerationStatus status;
    
    public ModerationRecord(ModerationRecordId id, String moderatorId, 
                           ModerationAction action, String reason) {
        this.id = Objects.requireNonNull(id);
        this.moderatorId = Objects.requireNonNull(moderatorId);
        this.action = Objects.requireNonNull(action);
        this.reason = reason;
        this.moderatedAt = LocalDateTime.now();
        this.status = ModerationStatus.COMPLETED;
    }
    
    public void addComments(String comments) {
        this.comments = comments;
    }
    
    public boolean isApprovalAction() {
        return action == ModerationAction.APPROVE;
    }
    
    public boolean isRejectionAction() {
        return action == ModerationAction.REJECT;
    }
}
```

#### StockReservation Entity

```java
@Entity(name = "StockReservation", description = "庫存預留Entity")
public class StockReservation {
    private final StockReservationId id;
    private final ReservationId reservationId;
    private int quantity;
    private LocalDateTime reservedAt;
    private LocalDateTime expiresAt;
    private ReservationStatus status;
    private String reservationReason;
    
    public StockReservation(StockReservationId id, ReservationId reservationId, 
                           int quantity, String reason) {
        this.id = Objects.requireNonNull(id);
        this.reservationId = Objects.requireNonNull(reservationId);
        this.quantity = quantity;
        this.reservationReason = reason;
        this.reservedAt = LocalDateTime.now();
        this.expiresAt = reservedAt.plusHours(24); // 預設24小時過期
        this.status = ReservationStatus.ACTIVE;
    }
    
    // 業務邏輯
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public void extend(Duration duration) {
        this.expiresAt = this.expiresAt.plus(duration);
    }
    
    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }
    
    public void release() {
        this.status = ReservationStatus.RELEASED;
    }
}
```

#### DeliveryAddress Entity

```java
@Entity(name = "DeliveryAddress", description = "配送地址Entity")
public class DeliveryAddress {
    private final DeliveryAddressId id;
    private Address address;
    private String recipientName;
    private String recipientPhone;
    private boolean isDefault;
    private AddressStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    
    public DeliveryAddress(DeliveryAddressId id, Address address, 
                          String recipientName, String recipientPhone) {
        this.id = Objects.requireNonNull(id);
        this.address = Objects.requireNonNull(address);
        this.recipientName = Objects.requireNonNull(recipientName);
        this.recipientPhone = recipientPhone;
        this.isDefault = false;
        this.status = AddressStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }
    
    public void markAsDefault() {
        this.isDefault = true;
    }
    
    public void unmarkAsDefault() {
        this.isDefault = false;
    }
    
    public void markAsUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
    
    public boolean isValid() {
        return address != null && recipientName != null && 
               status == AddressStatus.ACTIVE;
    }
}
```

## 資料模型

### Entity 狀態管理

#### EntityStatus 列舉

```java
public enum EntityStatus {
    ACTIVE("活躍"),
    INACTIVE("非活躍"), 
    PENDING("待處理"),
    ARCHIVED("已歸檔");
}
```

#### Entity 生命週期介面

```java
public interface EntityLifecycle {
    void activate();
    void deactivate();
    void archive();
    boolean canBeDeleted();
}
```

## 遷移Policy

### 階段 1：基礎設施準備

1. ~~創建 BaseEntity 抽象類別~~ (跳過，直接使用具體 Entity)
2. Entity 註解已存在，無需額外建立
3. 測試框架已就緒
4. **遵循專案現有模式** - 確保所有新組件符合現有的設計慣例

### Implementation

#### 1. **保持向後相容性**

- 保留現有的 public API
- 提供向後相容的 getter 方法
- 漸進式遷移，避免破壞性變更

#### 2. **遵循現有模式**

- 使用專案既有的註解格式
- 遵循現有的命名慣例
- 保持與現有 Value Object 的一致性

#### Testing

- 為每個新 Entity 編寫Unit Test
- 確保Aggregate Root的業務不變性
- 驗證 Repository 和 Domain Service 的正確性

### 階段 2：Seller AggregateRefactoring

1. 創建新的 Entity 類別
2. Refactoring Seller Aggregate Root
3. 遷移 SellerProfile 資料和邏輯
4. 更新相關測試

### 階段 3：ProductReview AggregateRefactoring

1. 創建 ReviewImage, ModerationRecord Entity
2. Refactoring ProductReview Aggregate Root
3. 移除 ReviewModeration Aggregate Root
4. 更新審核流程

### 階段 4：其他Aggregate改善

1. 改善 Customer Aggregate的 Entity 結構
2. 改善 Inventory Aggregate的 Entity 結構
3. 處理 PaymentMethod Aggregate Root問題

## Testing

### Testing

所有 Domain Model（Aggregate Root、Entity、Value Object）已由現有的 BDD 測試完整覆蓋，無需額外的Unit Test：

1. **BDD 測試**: 使用 Cucumber 測試完整的業務場景，涵蓋所有Aggregate Root和 Entity 的行為
2. **Integration Test**: 測試Aggregate Root與 Repository 和 Domain Service 的互動
3. **Architecture Test**: 使用 ArchUnit 驗證Aggregate邊界和 DDD 模式合規性

### Testing

1. **架構合規性**: 確保新的 Entity 結構符合 DDD 戰術模式
2. **向後相容性**: 驗證 API 層面的相容性
3. **資料完整性**: 確保Refactoring不影響資料一致性

### Testing

1. **資料遷移測試**: 驗證資料完整性
2. **向後相容性測試**: 確保 API 相容性
3. **回歸測試**: 驗證現有功能正常運作

## 風險管控

### 技術風險

- **資料遺失風險**: 建立完整的備份和回滾機制
- **效能影響風險**: 進行效能基準測試
- **相容性風險**: 維護 API 向後相容性

### 業務風險

- **功能中斷風險**: 採用漸進式DeploymentPolicy
- **User體驗風險**: 確保 UI 層不受影響
- **資料一致性風險**: 實作強一致性檢查

## 成功Metrics

### Code QualityMetrics

- Entity 覆蓋率達到 80% 以上
- Aggregate Root複雜度降低 30%
- 程式碼重複率降低 50%

### DDD 合規性Metrics

- 所有Aggregate Root都包含至少一個 Entity 或複雜 Value Object
- Aggregate邊界清晰，無跨Aggregate直接依賴
- Entity 都有豐富的業務邏輯和狀態管理
