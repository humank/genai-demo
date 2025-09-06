package solid.humank.genaidemo.domain.review.service;

import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.review.model.aggregate.ProductReview;

/**
 * 評價審核領域服務
 * 
 * 處理評價審核相關的複雜業務邏輯，現在與 ProductReview 聚合整合
 */
@DomainService(name = "ReviewModerationService", description = "評價審核領域服務，處理評價審核的複雜業務邏輯", boundedContext = "Review")
public class ReviewModerationService {

    /**
     * 自動審核評價
     * 
     * @param review 評價聚合根
     * @return 是否通過自動審核
     */
    public boolean autoModerateReview(ProductReview review) {
        // 實作自動審核邏輯（敏感詞檢測、垃圾內容檢測等）
        String content = review.getRating().comment();

        // 如果沒有評論內容，只檢查評分
        if (content == null || content.isBlank()) {
            return true; // 只有評分沒有評論的情況下通過
        }

        // 簡單的敏感詞檢測
        String[] sensitiveWords = { "垃圾", "騙子", "假貨", "詐騙" };
        for (String word : sensitiveWords) {
            if (content.contains(word)) {
                return false;
            }
        }

        // 檢查內容長度（如果有評論的話）
        if (content.length() < 5) {
            return false;
        }

        return true;
    }

    /**
     * 計算評價可信度
     * 
     * @param review 評價聚合根
     * @return 可信度分數 (0-1)
     */
    public double calculateReviewCredibility(ProductReview review) {
        double credibility = 0.5; // 基礎分數

        // 根據評價長度調整可信度
        String content = review.getRating().comment();
        if (content != null && !content.isBlank()) {
            int contentLength = content.length();
            if (contentLength > 50) {
                credibility += 0.2;
            }
            if (contentLength > 100) {
                credibility += 0.1;
            }
        }

        // 根據評分合理性調整
        int rating = review.getRating().score();
        if (rating >= 2 && rating <= 4) {
            credibility += 0.1; // 中等評分更可信
        }

        // 根據審核歷史調整
        long moderationCount = review.getModerations().size();
        if (moderationCount == 0) {
            credibility += 0.1; // 沒有被審核過的評價更可信
        }

        return Math.min(1.0, credibility);
    }

    /**
     * 檢查評價是否需要人工審核
     * 
     * @param review 評價聚合根
     * @return 是否需要人工審核
     */
    public boolean requiresManualModeration(ProductReview review) {
        // 如果自動審核失敗，需要人工審核
        if (!autoModerateReview(review)) {
            return true;
        }

        // 如果可信度太低，需要人工審核
        if (calculateReviewCredibility(review) < 0.6) {
            return true;
        }

        // 如果評分極端（1星或5星），需要人工審核
        int rating = review.getRating().score();
        if (rating == 1 || rating == 5) {
            return true;
        }

        return false;
    }
}