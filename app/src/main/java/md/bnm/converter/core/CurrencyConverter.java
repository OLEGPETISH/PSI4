package md.bnm.converter.core;

import md.bnm.converter.model.ConversionResult;
import md.bnm.converter.model.CurrencyRate;
import md.bnm.converter.model.RateSheet;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure Java currency conversion engine using BigDecimal arithmetic.
 * Supports direct (Foreign -> MDL), reverse (MDL -> Foreign), cross-currency (Foreign A -> Foreign B),
 * and identical-currency (A -> A) conversions.
 */
public final class CurrencyConverter {
    private static final int INTERNAL_SCALE = 8;
    private static final int RATE_DISPLAY_SCALE = 4;
    private static final int AMOUNT_DISPLAY_SCALE = 2;

    private CurrencyConverter() {
    }

    /**
     * Calculates the unit exchange rate for 1 unit of sourceCurrency expressed in targetCurrency.
     */
    public static BigDecimal calculatePairUnitRate(RateSheet sheet, String sourceCurrency, String targetCurrency) {
        if (sheet == null || sheet.getRatesMap().isEmpty()) {
            throw new DataSourceException("Таблица курсов валют пуста или не загружена.", true);
        }

        String fromCode = InputValidator.validateCurrencyCode(sourceCurrency, "исходная валюта");
        String toCode = InputValidator.validateCurrencyCode(targetCurrency, "целевая валюта");

        CurrencyRate fromRate = sheet.getRate(fromCode);
        if (fromRate == null) {
            throw new DataSourceException("Валюта «" + fromCode + "» отсутствует в котировках НБМ за " + sheet.getEffectiveDate() + ".");
        }

        CurrencyRate toRate = sheet.getRate(toCode);
        if (toRate == null) {
            throw new DataSourceException("Валюта «" + toCode + "» отсутствует в котировках НБМ за " + sheet.getEffectiveDate() + ".");
        }

        if (fromCode.equals(toCode)) {
            return BigDecimal.ONE.setScale(RATE_DISPLAY_SCALE, RoundingMode.HALF_UP);
        }

        return fromRate.getUnitRate().divide(toRate.getUnitRate(), INTERNAL_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Converts a raw user amount string from sourceCurrency to targetCurrency using the given RateSheet.
     */
    public static ConversionResult convert(String rawAmount, String sourceCurrency, String targetCurrency, RateSheet sheet) {
        BigDecimal amount = InputValidator.validateAmount(rawAmount);
        return convert(amount, sourceCurrency, targetCurrency, sheet);
    }

    /**
     * Converts a validated BigDecimal amount from sourceCurrency to targetCurrency using the given RateSheet.
     */
    public static ConversionResult convert(BigDecimal amount, String sourceCurrency, String targetCurrency, RateSheet sheet) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Сумма конвертации должна быть положительным числом.");
        }
        if (sheet == null || sheet.getRatesMap().isEmpty()) {
            throw new DataSourceException("Таблица курсов валют пуста.", true);
        }

        String fromCode = InputValidator.validateCurrencyCode(sourceCurrency, "исходная валюта");
        String toCode = InputValidator.validateCurrencyCode(targetCurrency, "целевая валюта");

        if (fromCode.equals(toCode)) {
            if (sheet.getRate(fromCode) == null) {
                throw new DataSourceException("Валюта «" + fromCode + "» отсутствует в котировках НБМ.");
            }
            BigDecimal oneRate = BigDecimal.ONE.setScale(RATE_DISPLAY_SCALE, RoundingMode.HALF_UP);
            BigDecimal sameAmount = amount.setScale(AMOUNT_DISPLAY_SCALE, RoundingMode.HALF_UP);
            return new ConversionResult(
                    sameAmount,
                    fromCode,
                    toCode,
                    sameAmount,
                    oneRate,
                    oneRate,
                    sheet.getEffectiveDate(),
                    sheet.getSource(),
                    sheet.isFromCache(),
                    sheet.getNoticeMessage()
            );
        }

        BigDecimal exactUnitRate = calculatePairUnitRate(sheet, fromCode, toCode);
        BigDecimal exactInverseRate = BigDecimal.ONE.divide(exactUnitRate, INTERNAL_SCALE, RoundingMode.HALF_UP);

        BigDecimal convertedAmount = amount.multiply(exactUnitRate).setScale(AMOUNT_DISPLAY_SCALE, RoundingMode.HALF_UP);
        BigDecimal displayUnitRate = exactUnitRate.setScale(RATE_DISPLAY_SCALE, RoundingMode.HALF_UP);
        BigDecimal displayInverseRate = exactInverseRate.setScale(RATE_DISPLAY_SCALE, RoundingMode.HALF_UP);

        return new ConversionResult(
                amount.setScale(AMOUNT_DISPLAY_SCALE, RoundingMode.HALF_UP),
                fromCode,
                toCode,
                convertedAmount,
                displayUnitRate,
                displayInverseRate,
                sheet.getEffectiveDate(),
                sheet.getSource(),
                sheet.isFromCache(),
                sheet.getNoticeMessage()
        );
    }
}
