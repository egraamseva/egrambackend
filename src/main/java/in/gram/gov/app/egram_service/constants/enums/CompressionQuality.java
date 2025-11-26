package in.gram.gov.app.egram_service.constants.enums;

/**
 * Enum for image compression quality levels
 */
public enum CompressionQuality {
    HIGH(0.75f, "High quality - 75% compression"),
    MEDIUM(0.60f, "Medium quality - 60% compression"),
    LOW(0.40f, "Low quality - 40% compression");

    private final float qualityPercentage;
    private final String description;

    CompressionQuality(float qualityPercentage, String description) {
        this.qualityPercentage = qualityPercentage;
        this.description = description;
    }

    public float getQualityPercentage() {
        return qualityPercentage;
    }

    public String getDescription() {
        return description;
    }
}


