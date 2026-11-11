package md.bnm.converter.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Single dated data point for historical comparison charts (7-day and 30-day dynamics).
 */
public final class HistoricalRatePoint {
    private final String date;
    private final BigDecimal rate;

    public HistoricalRatePoint(String date, BigDecimal rate) {
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("Дата исторической точки не может быть пустой.");
        }
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Значение курса в исторической точке должно быть положительным.");
        }
        this.date = date.trim();
        this.rate = rate.setScale(4, RoundingMode.HALF_UP);
    }

    public String getDate() {
        return date;
    }

    public BigDecimal getRate() {
        return rate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HistoricalRatePoint)) return false;
        HistoricalRatePoint that = (HistoricalRatePoint) o;
        return Objects.equals(date, that.date) && Objects.equals(rate, that.rate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, rate);
    }
}
