package md.bnm.converter.core;

import md.bnm.converter.model.HistoricalRatePoint;
import md.bnm.converter.model.PeriodStats;
import md.bnm.converter.model.RateSheet;
import md.bnm.converter.model.TrendDirection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure Java statistical engine for the Weekly (7-day) and Monthly (30-day) comparison screens.
 * Computes min, max, arithmetic mean, absolute change, percentage change, and explicit trend direction.
 */
public final class TrendAnalyticsCalculator {
    private static final int RATE_SCALE = 4;
    private static final int PERCENT_SCALE = 2;
    private static final BigDecimal FLAT_THRESHOLD = new BigDecimal("0.00005");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private TrendAnalyticsCalculator() {
    }

    /**
     * Builds a time series of historical cross-rates for (baseCode / quoteCode) from ordered RateSheets
     * and calculates complete period statistics.
     */
    public static PeriodStats analyzeFromSheets(
            List<RateSheet> orderedSheets,
            String baseCode,
            String quoteCode,
            int periodDays,
            String effectiveCacheDate,
            boolean fromCache,
            String sourceLabel
    ) {
        if (orderedSheets == null || orderedSheets.isEmpty()) {
            throw new DataSourceException("Исторический набор данных за выбранный период пуст.", true);
        }

        String validBase = InputValidator.validateCurrencyCode(baseCode, "базовая валюта");
        String validQuote = InputValidator.validateCurrencyCode(quoteCode, "котируемая валюта");

        List<HistoricalRatePoint> points = new ArrayList<>();
        for (RateSheet sheet : orderedSheets) {
            if (sheet == null || sheet.getRatesMap().isEmpty()) {
                continue;
            }
            BigDecimal pairRate = CurrencyConverter.calculatePairUnitRate(sheet, validBase, validQuote)
                    .setScale(RATE_SCALE, RoundingMode.HALF_UP);
            String pointDate = sheet.getRequestedDate().isEmpty()
                    ? sheet.getEffectiveDate()
                    : sheet.getRequestedDate();
            points.add(new HistoricalRatePoint(pointDate, pairRate));
        }

        if (points.isEmpty()) {
            throw new DataSourceException("Не удалось сформировать точки графика: исторические данные пусты.", true);
        }

        return calculateStats(
                validBase,
                validQuote,
                periodDays,
                points,
                effectiveCacheDate,
                fromCache,
                sourceLabel
        );
    }

    /**
     * Computes min, max, average, absolute delta, percentage delta, and trend direction
     * for a chronologically ordered list of HistoricalRatePoints.
     */
    public static PeriodStats calculateStats(
            String baseCode,
            String quoteCode,
            int periodDays,
            List<HistoricalRatePoint> points,
            String effectiveCacheDate,
            boolean fromCache,
            String sourceLabel
    ) {
        if (points == null || points.isEmpty()) {
            throw new DataSourceException("Набор исторических данных пуст. Невозможно рассчитать статистику периода.", true);
        }
        if (periodDays <= 0) {
            throw new ValidationException("Длительность анализируемого периода должна быть положительным числом дней.");
        }

        String validBase = InputValidator.validateCurrencyCode(baseCode, "базовая валюта");
        String validQuote = InputValidator.validateCurrencyCode(quoteCode, "котируемая валюта");

        HistoricalRatePoint firstPoint = points.get(0);
        HistoricalRatePoint lastPoint = points.get(points.size() - 1);

        BigDecimal minRate = firstPoint.getRate();
        String minDate = firstPoint.getDate();

        BigDecimal maxRate = firstPoint.getRate();
        String maxDate = firstPoint.getDate();

        BigDecimal sum = BigDecimal.ZERO;

        for (HistoricalRatePoint pt : points) {
            if (pt == null) {
                throw new DataSourceException("Обнаружена пустая точка в историческом ряду.");
            }
            BigDecimal r = pt.getRate();
            sum = sum.add(r);

            if (r.compareTo(minRate) < 0) {
                minRate = r;
                minDate = pt.getDate();
            }
            if (r.compareTo(maxRate) > 0) {
                maxRate = r;
                maxDate = pt.getDate();
            }
        }

        BigDecimal count = BigDecimal.valueOf(points.size());
        BigDecimal averageRate = sum.divide(count, RATE_SCALE, RoundingMode.HALF_UP);

        BigDecimal startRate = firstPoint.getRate().setScale(RATE_SCALE, RoundingMode.HALF_UP);
        BigDecimal endRate = lastPoint.getRate().setScale(RATE_SCALE, RoundingMode.HALF_UP);

        BigDecimal absoluteChange = endRate.subtract(startRate).setScale(RATE_SCALE, RoundingMode.HALF_UP);

        BigDecimal percentChange;
        if (startRate.compareTo(BigDecimal.ZERO) == 0) {
            percentChange = BigDecimal.ZERO.setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
        } else {
            percentChange = endRate.subtract(startRate)
                    .multiply(ONE_HUNDRED)
                    .divide(startRate, PERCENT_SCALE, RoundingMode.HALF_UP);
        }

        TrendDirection direction = classifyTrend(absoluteChange);

        return new PeriodStats(
                validBase,
                validQuote,
                periodDays,
                minRate.setScale(RATE_SCALE, RoundingMode.HALF_UP),
                minDate,
                maxRate.setScale(RATE_SCALE, RoundingMode.HALF_UP),
                maxDate,
                averageRate,
                startRate,
                firstPoint.getDate(),
                endRate,
                lastPoint.getDate(),
                absoluteChange,
                percentChange,
                direction,
                points,
                effectiveCacheDate,
                fromCache,
                sourceLabel
        );
    }

    /**
     * Classifies trend direction as UP, DOWN, or FLAT based on the absolute rate change.
     */
    public static TrendDirection classifyTrend(BigDecimal absoluteChange) {
        if (absoluteChange == null) {
            return TrendDirection.FLAT;
        }
        if (absoluteChange.compareTo(FLAT_THRESHOLD) > 0) {
            return TrendDirection.UP;
        }
        if (absoluteChange.compareTo(FLAT_THRESHOLD.negate()) < 0) {
            return TrendDirection.DOWN;
        }
        return TrendDirection.FLAT;
    }
}
