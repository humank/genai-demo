package solid.humank.genaidemo.domain.seller.model.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerVerificationId;
import solid.humank.genaidemo.domain.seller.model.valueobject.VerificationStatus;

/** 賣家驗證實體 */
@Entity(name = "SellerVerification", description = "賣家驗證實體")
public class SellerVerification {

    private final SellerVerificationId id;
    private VerificationStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime expiresAt;
    private String verifierUserId;
    private String rejectionReason;
    private List<String> requiredDocuments;
    private List<String> submittedDocuments;
    private String notes;
    private LocalDateTime lastUpdated;

    public SellerVerification(SellerVerificationId id) {
        this.id = Objects.requireNonNull(id, "驗證ID不能為空");
        this.status = VerificationStatus.PENDING;
        this.submittedAt = LocalDateTime.now();
        this.requiredDocuments = new ArrayList<>();
        this.submittedDocuments = new ArrayList<>();
        this.lastUpdated = LocalDateTime.now();

        // 設置預設的必要文件
        initializeRequiredDocuments();
    }

    /**
     * 重建用建構子（用於從持久化層重建）
     */
    public SellerVerification(SellerVerificationId id, VerificationStatus status, LocalDateTime submittedAt,
            LocalDateTime verifiedAt, LocalDateTime expiresAt, String verifierUserId,
            String rejectionReason, List<String> requiredDocuments,
            List<String> submittedDocuments, String notes, LocalDateTime lastUpdated) {
        this.id = Objects.requireNonNull(id, "驗證ID不能為空");
        this.status = status != null ? status : VerificationStatus.PENDING;
        this.submittedAt = submittedAt != null ? submittedAt : LocalDateTime.now();
        this.verifiedAt = verifiedAt;
        this.expiresAt = expiresAt;
        this.verifierUserId = verifierUserId;
        this.rejectionReason = rejectionReason;
        this.requiredDocuments = requiredDocuments != null ? new ArrayList<>(requiredDocuments) : new ArrayList<>();
        this.submittedDocuments = submittedDocuments != null ? new ArrayList<>(submittedDocuments) : new ArrayList<>();
        this.notes = notes;
        this.lastUpdated = lastUpdated != null ? lastUpdated : LocalDateTime.now();
    }

    // 業務邏輯方法

    /**
     * 提交文件
     */
    public void submitDocument(String documentPath) {
        if (status != VerificationStatus.PENDING) {
            throw new IllegalStateException("只有待驗證狀態才能提交文件");
        }

        if (documentPath == null || documentPath.isBlank()) {
            throw new IllegalArgumentException("文件路徑不能為空");
        }

        if (!submittedDocuments.contains(documentPath)) {
            submittedDocuments.add(documentPath);
            this.lastUpdated = LocalDateTime.now();
        }
    }

    /**
     * 驗證通過
     */
    public void approve(String verifierUserId, LocalDateTime expiresAt) {
        if (status != VerificationStatus.PENDING) {
            throw new IllegalStateException("只有待驗證狀態才能通過驗證");
        }

        if (!isDocumentationComplete()) {
            throw new IllegalStateException("文件不完整，無法通過驗證");
        }

        this.status = VerificationStatus.VERIFIED;
        this.verifierUserId = Objects.requireNonNull(verifierUserId, "驗證員ID不能為空");
        this.verifiedAt = LocalDateTime.now();
        this.expiresAt = expiresAt != null ? expiresAt : LocalDateTime.now().plusYears(1);
        this.rejectionReason = null;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 驗證拒絕
     */
    public void reject(String verifierUserId, String reason) {
        if (status != VerificationStatus.PENDING) {
            throw new IllegalStateException("只有待驗證狀態才能拒絕驗證");
        }

        this.status = VerificationStatus.REJECTED;
        this.verifierUserId = Objects.requireNonNull(verifierUserId, "驗證員ID不能為空");
        this.rejectionReason = Objects.requireNonNull(reason, "拒絕原因不能為空");
        this.verifiedAt = null;
        this.expiresAt = null;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 重新提交驗證
     */
    public void resubmit() {
        if (!status.canBeReverified()) {
            throw new IllegalStateException("當前狀態不允許重新提交驗證");
        }

        this.status = VerificationStatus.PENDING;
        this.submittedAt = LocalDateTime.now();
        this.verifiedAt = null;
        this.expiresAt = null;
        this.verifierUserId = null;
        this.rejectionReason = null;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 標記為過期
     */
    public void markAsExpired() {
        if (status == VerificationStatus.VERIFIED && isExpired()) {
            this.status = VerificationStatus.EXPIRED;
            this.lastUpdated = LocalDateTime.now();
        }
    }

    /**
     * 添加備註
     */
    public void addNotes(String notes) {
        this.notes = notes;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 檢查是否已驗證
     */
    public boolean isVerified() {
        return status.isVerified() && !isExpired();
    }

    /**
     * 檢查是否過期
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 檢查文件是否完整
     */
    public boolean isDocumentationComplete() {
        return submittedDocuments.containsAll(requiredDocuments);
    }

    /**
     * 獲取缺失的文件
     */
    public List<String> getMissingDocuments() {
        List<String> missing = new ArrayList<>(requiredDocuments);
        missing.removeAll(submittedDocuments);
        return missing;
    }

    /**
     * 檢查驗證是否即將過期（30天內）
     */
    public boolean isExpiringSoon() {
        return expiresAt != null &&
                LocalDateTime.now().plusDays(30).isAfter(expiresAt);
    }

    // 私有方法

    private void initializeRequiredDocuments() {
        requiredDocuments.add("business_license");
        requiredDocuments.add("tax_certificate");
        requiredDocuments.add("identity_document");
    }

    // Getters

    public SellerVerificationId getId() {
        return id;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getVerifierUserId() {
        return verifierUserId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public List<String> getRequiredDocuments() {
        return Collections.unmodifiableList(requiredDocuments);
    }

    public List<String> getSubmittedDocuments() {
        return Collections.unmodifiableList(submittedDocuments);
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SellerVerification that = (SellerVerification) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SellerVerification{" +
                "id=" + id +
                ", status=" + status +
                ", isVerified=" + isVerified() +
                ", isExpired=" + isExpired() +
                ", submittedAt=" + submittedAt +
                '}';
    }
}