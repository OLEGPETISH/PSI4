package md.bnm.converter;

import md.bnm.converter.core.DataSourceException;
import md.bnm.converter.core.TrendAnalyticsCalculator;
import md.bnm.converter.model.HistoricalRatePoint;
import md.bnm.converter.model.PeriodStats;
import md.bnm.converter.model.RateSheet;
import md.bnm.converter.model.TrendDirection;
import md.bnm.converter.data.HistoricalRatesCache;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for 7-day (Weekly) and 30-day (Monthly) comparison statistics:
 * minimum, maximum, mean (average), absolute change, percentage change,
 * explicit trend direction (UP, DOWN, FLAT), and empty dataset behavior.
 */
public class TrendAnalyticsCalculatorTest {

    @Test
    public void testWeeklyStatsUpwardTrendCalculation() {
        List<HistoricalRatePoint> weekPoints = Arrays.asList(
                new HistoricalRatePoint("05.11.2026", new BigDecimal("19.8000")),
                new HistoricalRatePoint("06.11.2026", new BigDecimal("19.7500")), // min
                new HistoricalRatePoint("07.11.2026", new BigDecimal("19.8500")),
                new HistoricalRatePoint("08.11.2026", new BigDecimal("19.9000")),
                new HistoricalRatePoint("09.11.2026", new BigDecimal("20.0000")),
                new HistoricalRatePoint("10.11.2026", new BigDecimal("20.2500")), // max
                new HistoricalRatePoint("11.11.2026", new BigDecimal("20.1960"))
        );

        PeriodStats stats = TrendAnalyticsCalculator.calculateStats(
                "EUR", "MDL", 7, weekPoints, "11.11.2026", false, "НБМ"
        );

        assertEquals("EUR", stats.getBaseCode());
        assertEquals("MDL", stats.getQuoteCode());
        assertEquals(7, stats.getPeriodDays());

        // Min = 19.7500 on 06.11.2026
        assertEquals(new BigDecimal("19.7500"), stats.getMinRate());
        assertEquals("06.11.2026", stats.getMinDate());

        // Max = 20.2500 on 10.11.2026
        assertEquals(new BigDecimal("20.2500"), stats.getMaxRate());
        assertEquals("10.11.2026", stats.getMaxDate());

        // Sum = 139.7460 / 7 = 19.963714... -> 19.9637
        assertEquals(new BigDecimal("19.9637"), stats.getAverageRate());

        // Absolute change = 20.1960 - 19.8000 = +0.3960
        assertEquals(new BigDecimal("0.3960"), stats.getAbsoluteChange());

        // Percentage change = (0.3960 / 19.8000) * 100 = +2.00%
        assertEquals(new BigDecimal("2.00"), stats.getPercentChange());
        assertEquals(TrendDirection.UP, stats.getTrendDirection());
        assertEquals("↑ Рост", stats.getTrendDirection().getDisplayBadge());
    }

    @Test
    public void testWeeklyStatsDownwardTrendCalculation() {
        List<HistoricalRatePoint> points = Arrays.asList(
                new HistoricalRatePoint("05.11.2026", new BigDecimal("18.0000")),
                new HistoricalRatePoint("06.11.2026", new BigDecimal("18.1000")),
                new HistoricalRatePoint("07.11.2026", new BigDecimal("17.9000")),
                new HistoricalRatePoint("08.11.2026", new BigDecimal("17.8500")),
                new HistoricalRatePoint("09.11.2026", new BigDecimal("17.7000")),
                new HistoricalRatePoint("10.11.2026", new BigDecimal("17.5500")),
                new HistoricalRatePoint("11.11.2026", new BigDecimal("17.6400"))
        );

        PeriodStats stats = TrendAnalyticsCalculator.calculateStats(
                "USD", "MDL", 7, points, "11.11.2026", true, "Локальный кэш"
        );

        assertEquals(new BigDecimal("17.5500"), stats.getMinRate());
        assertEquals("10.11.2026", stats.getMinDate());
        assertEquals(new BigDecimal("18.1000"), stats.getMaxRate());
        assertEquals("06.11.2026", stats.getMaxDate());

        // Absolute change = 17.6400 - 18.0000 = -0.3600
        assertEquals(new BigDecimal("-0.3600"), stats.getAbsoluteChange());

        // Percentage change = (-0.3600 / 18.0000) * 100 = -2.00%
        assertEquals(new BigDecimal("-2.00"), stats.getPercentChange());
        assertEquals(TrendDirection.DOWN, stats.getTrendDirection());
        assertEquals("↓ Падение", stats.getTrendDirection().getDisplayBadge());
    }

    @Test
    public void testMonthlyStats30DaysAndFlatTrend() {
        List<HistoricalRatePoint> monthPoints = new ArrayList<>();
        for (int day = 1; day <= 30; day++) {
            String d = String.format("%02d.10.2026", day);
            monthPoints.add(new HistoricalRatePoint(d, new BigDecimal("4.0000")));
        }

        PeriodStats stats = TrendAnalyticsCalculator.calculateStats(
                "RON", "MDL", 30, monthPoints, "30.10.2026", false, "НБМ"
        );

        assertEquals(30, stats.getPeriodDays());
        assertEquals(30, stats.getPoints().size());
        assertEquals(new BigDecimal("4.0000"), stats.getMinRate());
        assertEquals(new BigDecimal("4.0000"), stats.getMaxRate());
        assertEquals(new BigDecimal("4.0000"), stats.getAverageRate());
        assertEquals(new BigDecimal("0.0000"), stats.getAbsoluteChange());
        assertEquals(new BigDecimal("0.00"), stats.getPercentChange());
        assertEquals(TrendDirection.FLAT, stats.getTrendDirection());
        assertEquals("→ Без изменений", stats.getTrendDirection().getDisplayBadge());
    }

    @Test
    public void testAnalyzeFrom30DaySeedSheetsForCrossCurrencyPair() {
        List<RateSheet> sheets = HistoricalRatesCache.buildSeedHistory(LocalDate.of(2026, 11, 11), 30);
        PeriodStats stats = TrendAnalyticsCalculator.analyzeFromSheets(
                sheets, "EUR", "USD", 30, "11.11.2026", true, "Локальный кэш"
        );

        assertEquals(30, stats.getPoints().size());
        assertTrue(stats.getMinRate().compareTo(BigDecimal.ONE) > 0);
        assertTrue(stats.getMaxRate().compareTo(stats.getMinRate()) >= 0);
        assertTrue(stats.getAverageRate().compareTo(stats.getMinRate()) >= 0);
        assertTrue(stats.getAverageRate().compareTo(stats.getMaxRate()) <= 0);
    }

    @Test
    public void testEmptyPointsDatasetThrowsDataSourceException() {
        try {
            TrendAnalyticsCalculator.calculateStats(
                    "EUR", "MDL", 7, Collections.emptyList(), "11.11.2026", false, "НБМ"
            );
            fail("Expected DataSourceException when points list is empty");
        } catch (DataSourceException e) {
            assertTrue(e.isEmptyResponse());
            assertTrue(e.getMessage().contains("пуст"));
        }
    }

    @Test(expected = DataSourceException.class)
    public void testNullPointsDatasetThrowsDataSourceException() {
        TrendAnalyticsCalculator.calculateStats(
                "EUR", "MDL", 7, null, "11.11.2026", false, "НБМ"
        );
    }

    @Test(expected = DataSourceException.class)
    public void testEmptySheetsListThrowsDataSourceException() {
        TrendAnalyticsCalculator.analyzeFromSheets(
                Collections.emptyList(), "EUR", "MDL", 30, "11.11.2026", false, "НБМ"
        );
    }
}
