package solid.humank.genaidemo.domain.customer;

/**
 * 會員等級枚舉
 */
public enum MembershipLevel {
    BRONZE("Bronze", "銅級會員"),
    SILVER("Silver", "銀級會員"),
    GOLD("Gold", "金級會員"),
    PLATINUM("Platinum", "白金會員"),
    DIAMOND("Diamond", "鑽石會員");
    
    private final String code;
    private final String displayName;
    
    MembershipLevel(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static MembershipLevel fromCode(String code) {
        for (MembershipLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("未知的會員等級: " + code);
    }
}