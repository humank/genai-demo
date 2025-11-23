package solid.humank.genaidemo.interfaces.web.pricing.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 更新佣金費率請求 用於接收HTTP請求中的數據，支援一般費率和活動費率的獨立設定 佣金費率以百分比形式表示，範圍為 0-100 */
@Schema(
        description = "更新佣金費率請求",
        example =
                """
        {
            "normalRate": 5,
            "eventRate": 8
        }
        """)
public class UpdateCommissionRateRequest {
    @Schema(
            description = "一般佣金費率，以百分比表示 (0-100)，適用於日常銷售",
            example = "5",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minimum = "0",
            maximum = "100",
            format = "percentage")
    private int normalRate;

    @Schema(
            description = "活動佣金費率，以百分比表示 (0-100)，適用於特殊促銷活動期間",
            example = "8",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minimum = "0",
            maximum = "100",
            format = "percentage")
    private int eventRate;

    // 無參構造函數，用於JSON反序列化
    public UpdateCommissionRateRequest() {}

    public int getNormalRate() {
        return normalRate;
    }

    public void setNormalRate(int normalRate) {
        this.normalRate = normalRate;
    }

    public int getEventRate() {
        return eventRate;
    }

    public void setEventRate(int eventRate) {
        this.eventRate = eventRate;
    }
}
