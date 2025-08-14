package solid.humank.genaidemo.interfaces.web.pricing.dto;

/** 更新佣金費率請求 用於接收HTTP請求中的數據 */
public class UpdateCommissionRateRequest {
    private int normalRate;
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
