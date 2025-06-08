package solid.humank.genaidemo.application.pricing.dto;

/**
 * 更新佣金費率命令
 * 用於接收更新佣金費率的請求
 */
public class UpdateCommissionRateCommand {
    private String priceId;
    private int normalRate;
    private int eventRate;

    public UpdateCommissionRateCommand(String priceId, int normalRate, int eventRate) {
        this.priceId = priceId;
        this.normalRate = normalRate;
        this.eventRate = eventRate;
    }

    public String getPriceId() {
        return priceId;
    }

    public int getNormalRate() {
        return normalRate;
    }

    public int getEventRate() {
        return eventRate;
    }
}