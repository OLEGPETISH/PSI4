package md.bnm.converter.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregated statistical metrics for the 7-day (Weekly) and 30-day (Monthly) comparison screens.
 */
public final class PeriodStats {
    private final String baseCode;
    private final String quoteCode;
    private final int periodDays;
    private final BigDecimal minRate;
    private final String minDate;
    private final BigDecimal maxRate;
    private final String maxDate;
    private final BigDecimal averageRate;
    private final BigDecimal startRate;
    private final String startDate;
    private final BigDecimal endRate;
    private final String endDate;
    private final BigDecimal absoluteChange;
    private final BigDecimal percentChange;
    private final TrendDirection trendDirection;
    private final List<HistoricalRatePoint> points;
    private final String effectiveCacheDate;
    private final boolean fromCache;
    private final String sourceLabel;

    public PeriodStats(
            String baseCode,
            String quoteCode,
            int periodDays,
            BigDecimal minRate,
            String minDate,
            BigDecimal maxRate,
            String maxDate,
            BigDecimal averageRate,
            BigDecimal startRate,
            String startDate,
            BigDecimal endRate,
            String endDate,
            BigDecimal absoluteChange,
            BigDecimal percentChange,
            TrendDirection trendDirection,
            List<HistoricalRatePoint> points,
            String effectiveCacheDate,
            boolean fromCache,
            String sourceLabel
    ) {
        this.baseCode = baseCode;
        this.quoteCode = quoteCode;
        this.periodDays = periodDays;
        this.minRate = minRate;
        this.minDate = minDate;
        this.maxRate = maxRate;
        this.maxDate = maxDate;
        this.averageRate = averageRate;
        this.startRate = startRate;
        this.startDate = startDate;
        this.endRate = endRate;
        this.endDate = endDate;
        this.absoluteChange = absoluteChange;
        this.percentChange = percentChange;
        this.trendDirection = trendDirection;
        this.points = points == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(points));
        this.effectiveCacheDate = effectiveCacheDate == null ? "" : effectiveCacheDate;
        this.fromCache = fromCache;
        this.sourceLabel = sourceLabel == null ? "" : sourceLabel;
    }

    public String getBaseCode() {
        return baseCode;
    }

    public String getQuoteCode() {
        return quoteCode;
    }

    public int getPeriodDays() {
        return periodDays;
    }

    public BigDecimal getMinRate() {
        return minRate;
    }

    public String getMinDate() {
        return minDate;
    }

    public BigDecimal getMaxRate() {
        return maxRate;
    }

    public String getMaxDate() {
        return maxDate;
    }

    public BigDecimal getAverageRate() {
        return averageRate;
    }

    public BigDecimal getStartRate() {
        return startRate;
    }

    public String getStartDate() {
        return startDate;
    }

    public BigDecimal getEndRate() {
        return endRate;
    }

    public String getEndDate() {
        return endDate;
    }

    public BigDecimal getAbsoluteChange() {
        return absoluteChange;
    }

    public BigDecimal getPercentChange() {
        return percentChange;
    }

    public TrendDirection getTrendDirection() {
        return trendDirection;
    }

    public List<HistoricalRatePoint> getPoints() {
        return points;
    }

    public String getEffectiveCacheDate() {
        return effectiveCacheDate;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }
}
