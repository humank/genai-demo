package solid.humank.genaidemo.application.customer;

import io.swagger.v3.oas.annotations.media.Schema;

/** 客戶數據傳輸對象 */
@Schema(description = "客戶資訊數據傳輸對象，包含客戶的基本資訊。敏感資訊已進行適當的隱私保護處理")
public record CustomerDto(
        @Schema(description = "客戶唯一識別碼", example = "cust-12345-abcde", required = true) String id,
        @Schema(description = "客戶姓名（已脫敏處理）", example = "王**", required = true) String name,
        @Schema(description = "客戶電子郵件（已脫敏處理）", example = "w***@example.com", required = true)
                String email,
        @Schema(description = "客戶電話號碼（已脫敏處理）", example = "09****1234", nullable = true)
                String phone,
        @Schema(description = "客戶地址（已脫敏處理）", example = "台北市***區***路", nullable = true)
                String address,
        @Schema(
                        description = "會員等級",
                        example = "GOLD",
                        allowableValues = {"BRONZE", "SILVER", "GOLD", "PLATINUM"},
                        required = true)
                String membershipLevel) {}
