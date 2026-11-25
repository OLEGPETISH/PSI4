package md.bnm.converter;

import md.bnm.converter.core.BnmXmlParser;
import md.bnm.converter.core.DataSourceException;
import md.bnm.converter.model.CurrencyRate;
import md.bnm.converter.model.RateSheet;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for parsing official BNM XML responses, handling nominals > 1,
 * comma/dot decimal formats, MDL base injection, and empty/malformed responses.
 */
public class BnmXmlParserTest {

    private static final String SAMPLE_BNM_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<ValCurs Date=\"11.11.2026\" name=\"Official exchange rates\">\n"
            + "    <Valute ID=\"47\">\n"
            + "        <NumCode>978</NumCode>\n"
            + "        <CharCode>EUR</CharCode>\n"
            + "        <Nominal>1</Nominal>\n"
            + "        <Name>Euro</Name>\n"
            + "        <Value>20.1089</Value>\n"
            + "    </Valute>\n"
            + "    <Valute ID=\"44\">\n"
            + "        <NumCode>840</NumCode>\n"
            + "        <CharCode>USD</CharCode>\n"
            + "        <Nominal>1</Nominal>\n"
            + "        <Name>US Dollar</Name>\n"
            + "        <Value>17,9200</Value>\n"
            + "    </Valute>\n"
            + "    <Valute ID=\"24\">\n"
            + "        <NumCode>392</NumCode>\n"
            + "        <CharCode>JPY</CharCode>\n"
            + "        <Nominal>100</Nominal>\n"
            + "        <Name>Japanese Yen</Name>\n"
            + "        <Value>11.3140</Value>\n"
            + "    </Valute>\n"
            + "</ValCurs>";

    @Test
    public void testParseValidBnmXmlExtractsRatesAndInjectsMdl() {
        RateSheet sheet = BnmXmlParser.parseXml(SAMPLE_BNM_XML, "11.11.2026");

        assertEquals("11.11.2026", sheet.getEffectiveDate());
        assertEquals("11.11.2026", sheet.getRequestedDate());
        assertFalse(sheet.isFallbackDate());
        assertFalse(sheet.isFromCache());

        CurrencyRate eur = sheet.getRate("EUR");
        assertNotNull(eur);
        assertEquals(1, eur.getNominal());
        assertEquals(new BigDecimal("20.1089"), eur.getValue());
        assertEquals(new BigDecimal("20.1089"), eur.getDisplayUnitRate());

        // Comma decimal value normalized to BigDecimal
        CurrencyRate usd = sheet.getRate("USD");
        assertNotNull(usd);
        assertEquals(new BigDecimal("17.9200"), usd.getValue());

        // Nominal = 100 for JPY -> unit rate = 11.3140 / 100 = 0.11314000
        CurrencyRate jpy = sheet.getRate("JPY");
        assertNotNull(jpy);
        assertEquals(100, jpy.getNominal());
        assertEquals(new BigDecimal("11.3140"), jpy.getValue());
        assertEquals(new BigDecimal("0.11314000"), jpy.getUnitRate());

        // Implicit base currency MDL must be automatically injected
        CurrencyRate mdl = sheet.getRate("MDL");
        assertNotNull(mdl);
        assertEquals(1, mdl.getNominal());
        assertEquals(new BigDecimal("1.0000"), mdl.getValue());
    }

    @Test
    public void testParseXmlDetectsWeekendFallbackWhenEffectiveDateDiffers() {
        RateSheet sheet = BnmXmlParser.parseXml(SAMPLE_BNM_XML, "14.11.2026");

        assertEquals("11.11.2026", sheet.getEffectiveDate());
        assertEquals("14.11.2026", sheet.getRequestedDate());
        assertTrue(sheet.isFallbackDate());
        assertTrue(sheet.getNoticeMessage().contains("11.11.2026"));
    }

    @Test
    public void testParseEmptyValCursThrowsEmptyDataSourceException() {
        String emptyValCurs = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ValCurs Date=\"15.11.2026\"></ValCurs>";
        try {
            BnmXmlParser.parseXml(emptyValCurs, "15.11.2026");
            fail("Expected DataSourceException for empty <ValCurs>");
        } catch (DataSourceException e) {
            assertTrue(e.isEmptyResponse());
            assertTrue(e.getMessage().contains("отсутствуют"));
        }
    }

    @Test(expected = DataSourceException.class)
    public void testParseBlankStringThrowsException() {
        BnmXmlParser.parseXml("   ", "11.11.2026");
    }

    @Test(expected = DataSourceException.class)
    public void testParseMalformedXmlThrowsException() {
        BnmXmlParser.parseXml("<ValCurs><Valute><CharCode>EUR", "11.11.2026");
    }

    @Test(expected = DataSourceException.class)
    public void testParseWrongRootTagThrowsException() {
        BnmXmlParser.parseXml("<ExchangeRates><Valute/></ExchangeRates>", "11.11.2026");
    }

    @Test(expected = DataSourceException.class)
    public void testParseInvalidNumericValueThrowsException() {
        String badNumberXml = "<ValCurs Date=\"11.11.2026\">"
                + "<Valute><CharCode>EUR</CharCode><Nominal>1</Nominal><Name>Euro</Name><Value>NOT_A_NUM</Value></Valute>"
                + "</ValCurs>";
        BnmXmlParser.parseXml(badNumberXml, "11.11.2026");
    }
}
