package md.bnm.converter;

import md.bnm.converter.core.BnmXmlParser;
import md.bnm.converter.core.CurrencyConverter;
import md.bnm.converter.data.BnmRateService;
import md.bnm.converter.data.HistoricalRatesCache;
import md.bnm.converter.model.ConversionResult;
import md.bnm.converter.model.PeriodStats;
import md.bnm.converter.model.RateSheet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for forward, reverse, cross-currency, and identical-currency conversion,
 * plus persistent local JSON caching across restarts and offline fallback (Airplane Mode).
 */
public class CurrencyConverterAndCacheTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final String XML_PAYLOAD =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<ValCurs Date=\"11.11.2026\" name=\"Official exchange rates\">\n"
            + "    <Valute><NumCode>978</NumCode><CharCode>EUR</CharCode><Nominal>1</Nominal><Name>Euro</Name><Value>20.0000</Value></Valute>\n"
            + "    <Valute><NumCode>840</NumCode><CharCode>USD</CharCode><Nominal>1</Nominal><Name>US Dollar</Name><Value>18.0000</Value></Valute>\n"
            + "    <Valute><NumCode>392</NumCode><CharCode>JPY</CharCode><Nominal>100</Nominal><Name>Japanese Yen</Name><Value>12.0000</Value></Valute>\n"
            + "</ValCurs>";

    @Test
    public void testForwardReverseCrossAndIdenticalCurrencyConversion() {
        RateSheet sheet = BnmXmlParser.parseXml(XML_PAYLOAD, "11.11.2026");

        // 1. Forward conversion: 100 EUR -> MDL (100 * 20.0000 = 2000.00 MDL)
        ConversionResult eurToMdl = CurrencyConverter.convert("100", "EUR", "MDL", sheet);
        assertEquals(new BigDecimal("2000.00"), eurToMdl.getConvertedAmount());
        assertEquals(new BigDecimal("20.0000"), eurToMdl.getUnitRate());
        assertEquals(new BigDecimal("0.0500"), eurToMdl.getInverseUnitRate());

        // 2. Reverse conversion: 1800 MDL -> USD (1800 / 18.0000 = 100.00 USD)
        ConversionResult mdlToUsd = CurrencyConverter.convert("1800", "MDL", "USD", sheet);
        assertEquals(new BigDecimal("100.00"), mdlToUsd.getConvertedAmount());
        assertEquals(new BigDecimal("0.0556"), mdlToUsd.getUnitRate());
        assertEquals(new BigDecimal("18.0000"), mdlToUsd.getInverseUnitRate());

        // 3. Cross-currency conversion: 90 EUR -> USD (90 * 20 / 18 = 100.00 USD)
        ConversionResult eurToUsd = CurrencyConverter.convert("90", "EUR", "USD", sheet);
        assertEquals(new BigDecimal("100.00"), eurToUsd.getConvertedAmount());
        assertEquals(new BigDecimal("1.1111"), eurToUsd.getUnitRate());
        assertEquals(new BigDecimal("0.9000"), eurToUsd.getInverseUnitRate());

        // 4. Multi-nominal conversion: 1000 JPY -> MDL (unit rate = 12 / 100 = 0.12 -> 120.00 MDL)
        ConversionResult jpyToMdl = CurrencyConverter.convert("1000", "JPY", "MDL", sheet);
        assertEquals(new BigDecimal("120.00"), jpyToMdl.getConvertedAmount());
        assertEquals(new BigDecimal("0.1200"), jpyToMdl.getUnitRate());

        // 5. Identical currency conversion: 250.75 EUR -> EUR (1:1 without error)
        ConversionResult sameCurrency = CurrencyConverter.convert("250,75", "EUR", "EUR", sheet);
        assertEquals(new BigDecimal("250.75"), sameCurrency.getConvertedAmount());
        assertEquals(new BigDecimal("1.0000"), sameCurrency.getUnitRate());
    }

    @Test
    public void testPersistentCacheSurvivesRestartAndWorksOffline() throws IOException {
        File cacheFile = tempFolder.newFile("rates_cache.json");
        HistoricalRatesCache writerCache = new HistoricalRatesCache(cacheFile);
        writerCache.ensureSeeded(LocalDate.of(2026, 11, 11));

        RateSheet liveSheet = BnmXmlParser.parseXml(XML_PAYLOAD, "11.11.2026");
        writerCache.saveSheet(liveSheet);

        // Simulate application restart by creating a new HistoricalRatesCache instance pointing to the same file
        HistoricalRatesCache readerCacheAfterRestart = new HistoricalRatesCache(cacheFile);
        BnmRateService offlineService = new BnmRateService(readerCacheAfterRestart, dateStr -> {
            throw new IOException("Simulated Airplane Mode: Network unreachable");
        });

        RateSheet cachedSheet = offlineService.getRateSheet("11.11.2026", true);
        assertNotNull(cachedSheet);
        assertTrue(cachedSheet.isFromCache());
        assertEquals("11.11.2026", cachedSheet.getEffectiveDate());
        assertTrue(cachedSheet.getNoticeMessage().contains("офлайн"));

        // Verify 7-day and 30-day analytics also work in Airplane Mode after restart
        PeriodStats stats7 = offlineService.getPeriodAnalytics("EUR", "MDL", 7, "11.11.2026");
        assertEquals(7, stats7.getPoints().size());
        assertTrue(stats7.isFromCache());

        PeriodStats stats30 = offlineService.getPeriodAnalytics("USD", "MDL", 30, "11.11.2026");
        assertEquals(30, stats30.getPoints().size());
        assertTrue(stats30.isFromCache());
    }

    @Test
    public void testWeekendEmptyResponseStepsBackToPreviousWorkingDay() throws IOException {
        File cacheFile = tempFolder.newFile("rates_cache.json");
        HistoricalRatesCache cache = new HistoricalRatesCache(cacheFile);

        BnmRateService service = new BnmRateService(cache, dateStr -> {
            if ("15.11.2026".equals(dateStr) || "14.11.2026".equals(dateStr)) {
                return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ValCurs Date=\"" + dateStr + "\"></ValCurs>";
            }
            return XML_PAYLOAD;
        });

        RateSheet resolved = service.getRateSheet("15.11.2026", true);
        assertNotNull(resolved);
        assertEquals("11.11.2026", resolved.getEffectiveDate());
        assertEquals("15.11.2026", resolved.getRequestedDate());
        assertTrue(resolved.isFallbackDate());
        assertFalse(resolved.isFromCache());
    }
}
