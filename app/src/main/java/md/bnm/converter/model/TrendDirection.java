package md.bnm.converter.model;

/**
 * Explicit trend direction indicator for weekly (7-day) and monthly (30-day) comparison screens.
 */
public enum TrendDirection {
    UP("↑ Рост", "Рост курса за выбранный период"),
    DOWN("↓ Падение", "Снижение курса за выбранный период"),
    FLAT("→ Без изменений", "Отсутствие значимых изменений за период");

    private final String displayBadge;
    private final String description;

    TrendDirection(String displayBadge, String description) {
        this.displayBadge = displayBadge;
        this.description = description;
    }

    public String getDisplayBadge() {
        return displayBadge;
    }

    public String getDescription() {
        return description;
    }
}
