package solid.humank.genaidemo.domain.review.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 評價評分值對象 */
@ValueObject(name = "ReviewRating", description = "評價評分值對象")
public record ReviewRating(int score, String comment) {
    public ReviewRating {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("評分必須在 1-5 分之間");
        }
        if (comment != null && comment.length() > 500) {
            throw new IllegalArgumentException("評論內容不能超過 500 字");
        }
    }

    public Level getLevel() {
        return switch (score) {
            case 1 -> Level.POOR;
            case 2 -> Level.FAIR;
            case 3 -> Level.AVERAGE;
            case 4 -> Level.GOOD;
            case 5 -> Level.EXCELLENT;
            default -> throw new IllegalStateException("無效的評分: " + score);
        };
    }

    public boolean hasComment() {
        return comment != null && !comment.trim().isEmpty();
    }

    public boolean isPositive() {
        return score >= 4;
    }

    public boolean isNegative() {
        return score <= 2;
    }

    public enum Level {
        POOR("很差"),
        FAIR("一般"),
        AVERAGE("普通"),
        GOOD("不錯"),
        EXCELLENT("很棒");

        private final String description;

        Level(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 便利建構子
    public static ReviewRating of(int score) {
        return new ReviewRating(score, null);
    }

    public static ReviewRating of(int score, String comment) {
        return new ReviewRating(score, comment);
    }
}
