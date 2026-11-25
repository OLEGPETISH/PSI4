package md.bnm.converter.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable domain model representing a single currency quotation from the National Bank of Moldova (BNM).
 */
public final class CurrencyRate {
    private static final int INTERNAL_SCALE = 8;
    private static final int DISPLAY_SCALE = 4;

    private final String charCode;
    private final int nominal;
    private final String name;
    private final BigDecimal value;
    private final String numCode;

    public CurrencyRate(String charCode, int nominal, String name, BigDecimal value, String numCode) {
        if (charCode == null || charCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Код валюты не может быть пустым.");
        }
        if (nominal <= 0) {
            throw new IllegalArgumentException("Номинал валюты должен быть больше нуля: " + nominal);
        }
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Курс валюты должен быть положительным числом.");
        }
        this.charCode = charCode.trim().toUpperCase(Locale.ROOT);
        this.nominal = nominal;
        this.name = (name == null || name.trim().isEmpty()) ? this.charCode : name.trim();
        this.value = value.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
        this.numCode = (numCode == null) ? "" : numCode.trim();
    }

    public String getCharCode() {
        return charCode;
    }

    public int getNominal() {
        return nominal;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getValue() {
        return value;
    }

    public String getNumCode() {
        return numCode;
    }

    /**
     * Returns the rate in MDL for 1 single unit of this currency: value / nominal.
     */
    public BigDecimal getUnitRate() {
        return value.divide(BigDecimal.valueOf(nominal), INTERNAL_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Returns the unit rate rounded to 4 decimal places for display.
     */
    public BigDecimal getDisplayUnitRate() {
        return getUnitRate().setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return charCode + " — " + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CurrencyRate)) return false;
        CurrencyRate that = (CurrencyRate) o;
        return nominal == that.nominal
                && Objects.equals(charCode, that.charCode)
                && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(charCode, nominal, value);
    }
}
