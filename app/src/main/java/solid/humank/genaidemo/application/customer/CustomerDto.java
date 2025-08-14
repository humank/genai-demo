package solid.humank.genaidemo.application.customer;

/**
 * 客戶數據傳輸對象
 */
public record CustomerDto(
        String id,
        String name,
        String email,
        String phone,
        String address,
        String membershipLevel
) {
}