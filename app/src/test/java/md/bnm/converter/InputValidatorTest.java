package md.bnm.converter;

import md.bnm.converter.core.InputValidator;
import md.bnm.converter.core.ValidationException;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for user input validation (monetary amounts, decimal separators,
 * zero/negative rejection, alphabetic rejection, date validation, and Convert button state).
 */
public class InputValidatorTest {

    @Test
    public void testValidIntegerAndDecimalAmountsWithDotAndComma() {
        assertEquals(new BigDecimal("100"), InputValidator.validateAmount("100"));
        assertEquals(new BigDecimal("250.50"), InputValidator.validateAmount("250.50"));
        assertEquals(new BigDecimal("250.50"), InputValidator.validateAmount("  250,50 "));
        assertEquals(new BigDecimal("0.01"), InputValidator.validateAmount("0,01"));
        assertEquals(new BigDecimal("1000000"), InputValidator.validateAmount("1 000 000"));
    }

    @Test(expected = ValidationException.class)
    public void testEmptyStringIsRejected() {
        InputValidator.validateAmount("");
    }

    @Test(expected = ValidationException.class)
    public void testWhitespaceOnlyIsRejected() {
        InputValidator.validateAmount("    ");
    }

    @Test(expected = ValidationException.class)
    public void testNullAmountIsRejected() {
        InputValidator.validateAmount(null);
    }

    @Test(expected = ValidationException.class)
    public void testZeroAmountIsRejected() {
        InputValidator.validateAmount("0");
    }

    @Test(expected = ValidationException.class)
    public void testZeroDecimalAmountIsRejected() {
        InputValidator.validateAmount("0,0000");
    }

    @Test(expected = ValidationException.class)
    public void testNegativeAmountIsRejected() {
        InputValidator.validateAmount("-50.25");
    }

    @Test(expected = ValidationException.class)
    public void testAlphabeticInputIsRejected() {
        InputValidator.validateAmount("abc");
    }

    @Test(expected = ValidationException.class)
    public void testMixedAlphanumericInputIsRejected() {
        InputValidator.validateAmount("100EUR");
    }

    @Test(expected = ValidationException.class)
    public void testOverflowAmountIsRejected() {
        InputValidator.validateAmount("9999999999999999");
    }

    @Test
    public void testConvertButtonReadinessState() {
        assertTrue(InputValidator.isConvertReady("100", "EUR", "MDL"));
        assertFalse(InputValidator.isConvertReady("", "EUR", "MDL"));
        assertFalse(InputValidator.isConvertReady("   ", "EUR", "MDL"));
        assertFalse(InputValidator.isConvertReady(null, "EUR", "MDL"));
        assertFalse(InputValidator.isConvertReady("100", "", "MDL"));
        assertFalse(InputValidator.isConvertReady("100", "EUR", null));
    }

    @Test
    public void testDateValidationAcceptsValidBnmFormat() {
        LocalDate parsed = InputValidator.validateDate("11.11.2026");
        assertEquals(LocalDate.of(2026, 11, 11), parsed);
        assertEquals("11.11.2026", InputValidator.formatDate(parsed));
    }

    @Test(expected = ValidationException.class)
    public void testDateValidationRejectsInvalidCalendarDate() {
        InputValidator.validateDate("31.02.2026");
    }

    @Test(expected = ValidationException.class)
    public void testDateValidationRejectsWrongFormat() {
        InputValidator.validateDate("2026-11-11");
    }
}
