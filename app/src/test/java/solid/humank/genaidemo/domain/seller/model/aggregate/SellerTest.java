package solid.humank.genaidemo.domain.seller.model.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import solid.humank.genaidemo.domain.seller.model.valueobject.SellerId;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * 賣家聚合根測試
 */
class SellerTest {

    @Test
    void testCreateNewSeller() {
        // Given
        SellerId sellerId = SellerId.generateNew();
        String name = "測試賣家";
        String email = "test@example.com";
        String phone = "0912345678";
        String businessName = "測試商店";
        String businessAddress = "台北市信義區";
        String businessLicense = "12345678";

        // When
        Seller seller = new Seller(sellerId, name, email, phone, businessName, businessAddress, businessLicense);

        // Then
        assertNotNull(seller);
        assertEquals(sellerId, seller.getSellerId());
        assertEquals(name, seller.getName());
        assertEquals(email, seller.getEmail());
        assertEquals(phone, seller.getPhone());
        assertEquals(businessName, seller.getBusinessName());
        assertEquals(businessAddress, seller.getBusinessAddress());
        assertEquals(businessLicense, seller.getBusinessLicense());
        assertTrue(seller.isActive());
        assertFalse(seller.isVerified());
        assertNotNull(seller.getProfile());
        assertNotNull(seller.getContactInfo());
        assertNotNull(seller.getVerification());
        assertTrue(seller.getRatings().isEmpty());
    }

    @Test
    void testCreateFromLegacyData() {
        // Given
        SellerId sellerId = SellerId.generateNew();
        String name = "測試賣家";
        String email = "test@example.com";
        String phone = "0912345678";

        // When
        Seller seller = Seller.createFromLegacyData(sellerId, name, email, phone);

        // Then
        assertNotNull(seller);
        assertEquals(sellerId, seller.getSellerId());
        assertEquals(name, seller.getName());
        assertEquals(email, seller.getEmail());
        assertEquals(phone, seller.getPhone());
        assertTrue(seller.isActive());
        assertNotNull(seller.getProfile());
        assertNotNull(seller.getContactInfo());
        assertNotNull(seller.getVerification());
    }

    @Test
    void testUpdateContactInfo() {
        // Given
        Seller seller = createTestSeller();
        String newEmail = "newemail@example.com";
        String newPhone = "0987654321";

        // When
        seller.updateContactInfo(newEmail, newPhone);

        // Then
        assertEquals(newEmail, seller.getEmail());
        assertEquals(newPhone, seller.getPhone());
    }

    @Test
    void testUpdateBusinessInfo() {
        // Given
        Seller seller = createTestSeller();
        String newBusinessName = "新商店名稱";
        String newBusinessAddress = "新地址";
        String newDescription = "新描述";

        // When
        seller.updateBusinessInfo(newBusinessName, newBusinessAddress, newDescription);

        // Then
        assertEquals(newBusinessName, seller.getBusinessName());
        assertEquals(newBusinessAddress, seller.getBusinessAddress());
        assertEquals(newDescription, seller.getDescription());
    }

    @Test
    void testAddRating() {
        // Given
        Seller seller = createTestSeller();
        CustomerId customerId = CustomerId.generate();
        int rating = 5;
        String comment = "很好的賣家";

        // When
        seller.addRating(customerId, rating, comment);

        // Then
        assertEquals(1, seller.getRatings().size());
        assertEquals(1, seller.getTotalRatings());
        assertEquals(5.0, seller.calculateAverageRating());
    }

    @Test
    void testCalculateAverageRating() {
        // Given
        Seller seller = createTestSeller();
        CustomerId customerId1 = CustomerId.generate();
        CustomerId customerId2 = CustomerId.generate();

        // When
        seller.addRating(customerId1, 4, "不錯");
        seller.addRating(customerId2, 5, "很好");

        // Then
        assertEquals(2, seller.getTotalRatings());
        assertEquals(4.5, seller.calculateAverageRating());
    }

    @Test
    void testActivateDeactivate() {
        // Given
        Seller seller = createTestSeller();

        // When - Deactivate
        seller.deactivate();

        // Then
        assertFalse(seller.isActive());

        // When - Activate
        seller.activate();

        // Then
        assertTrue(seller.isActive());
    }

    @Test
    void testCanAcceptOrders() {
        // Given
        Seller seller = createTestSeller();

        // When - Initially should not be able to accept orders (not verified)
        boolean canAcceptInitially = seller.canAcceptOrders();

        // Then
        assertFalse(canAcceptInitially);

        // When - Verify contact info, submit required documents and approve
        // verification
        seller.getContactInfo().verifyEmail(); // Verify email to make contact info valid
        seller.submitVerificationDocument("business_license");
        seller.submitVerificationDocument("tax_certificate");
        seller.submitVerificationDocument("identity_document");
        seller.approveVerification("verifier123", LocalDateTime.now().plusYears(1));

        // Then
        assertTrue(seller.canAcceptOrders());
    }

    @Test
    void testSubmitVerificationDocument() {
        // Given
        Seller seller = createTestSeller();
        String documentPath = "/path/to/document.pdf";

        // When
        seller.submitVerificationDocument(documentPath);

        // Then
        assertTrue(seller.getVerification().getSubmittedDocuments().contains(documentPath));
    }

    @Test
    void testApproveVerification() {
        // Given
        Seller seller = createTestSeller();
        String verifierUserId = "verifier123";
        LocalDateTime expiresAt = LocalDateTime.now().plusYears(1);

        // Submit required documents first
        seller.submitVerificationDocument("business_license");
        seller.submitVerificationDocument("tax_certificate");
        seller.submitVerificationDocument("identity_document");

        // When
        seller.approveVerification(verifierUserId, expiresAt);

        // Then
        assertTrue(seller.isVerified());
        assertEquals(verifierUserId, seller.getVerification().getVerifierUserId());
    }

    @Test
    void testRejectVerification() {
        // Given
        Seller seller = createTestSeller();
        String verifierUserId = "verifier123";
        String reason = "文件不完整";

        // When
        seller.rejectVerification(verifierUserId, reason);

        // Then
        assertFalse(seller.isVerified());
        assertEquals(reason, seller.getVerification().getRejectionReason());
    }

    @Test
    void testBackwardCompatibilityMethods() {
        // Given
        Seller seller = createTestSeller();

        // Then - Test all backward compatibility methods
        assertNotNull(seller.getEmail());
        assertNotNull(seller.getPhone());
        assertNotNull(seller.getBusinessName());
        assertNotNull(seller.getBusinessAddress());
        assertNotNull(seller.getBusinessLicense());
        assertNotNull(seller.getJoinedAt());
        assertNotNull(seller.getLastActiveAt());
        assertNotNull(seller.getVerificationStatus());
        assertEquals(0.0, seller.getRating());
        assertEquals(0, seller.getTotalReviews());
    }

    private Seller createTestSeller() {
        SellerId sellerId = SellerId.generateNew();
        return new Seller(
                sellerId,
                "測試賣家",
                "test@example.com",
                "0912345678",
                "測試商店",
                "台北市信義區",
                "12345678");
    }
}